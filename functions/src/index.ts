import { onSchedule } from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { getMessaging, type Message } from "firebase-admin/messaging";

initializeApp();

const db = getFirestore();

const KST_OFFSET_MS = 9 * 60 * 60 * 1000;
const SEND_CHUNK = 500; // sendEach 1회 제한

/**
 * 한국 시간 기준 '오늘 00:00'의 epoch millis.
 *
 * Cloud Functions는 UTC로 실행되므로 서버 로컬 시간을 믿으면 안 된다.
 * records.createdAt은 안드로이드에서 System.currentTimeMillis()로 저장하므로
 * Timestamp가 아니라 숫자(Long)이고, 따라서 숫자끼리 비교한다.
 */
function startOfTodayKst(now: Date): number {
  const kst = new Date(now.getTime() + KST_OFFSET_MS);
  const midnightAsUtc = Date.UTC(
    kst.getUTCFullYear(),
    kst.getUTCMonth(),
    kst.getUTCDate()
  );
  return midnightAsUtc - KST_OFFSET_MS;
}

interface Target {
  uid: string;
  groupId: string;
  groupName: string;
}

export const workoutReminder = onSchedule(
  {
    schedule: "0 21 * * *",
    timeZone: "Asia/Seoul",
    region: "asia-northeast3",
    // 재시도하면 같은 알림이 두 번 갈 수 있으므로 끈다.
    // 리마인더 한 번 놓치는 것보다 중복 발송이 더 나쁘다.
    retryCount: 0,
  },
  async () => {
    const todayStart = startOfTodayKst(new Date());

    // ── 1. 오늘 작성된 기록 전부 (그룹 구분 없이 한 번에)
    const recordsSnap = await db
      .collection("records")
      .where("createdAt", ">=", todayStart)
      .get();

    // "userId_groupId" 집합. 같은 사람이 그룹별로 따로 작성하므로 쌍으로 관리한다.
    const wrote = new Set<string>();
    recordsSnap.forEach((doc) => {
      const { userId, groupId } = doc.data();
      if (typeof userId === "string" && typeof groupId === "string") {
        wrote.add(`${userId}_${groupId}`);
      }
    });

    // ── 2. 그룹 전체
    const groupsSnap = await db.collection("groups").get();
    if (groupsSnap.empty) {
      logger.info("그룹이 없어 발송하지 않음");
      return;
    }

    // ── 3. 그룹 × 멤버 순회 → 미작성자 추출
    const targets: Target[] = [];
    groupsSnap.forEach((doc) => {
      const data = doc.data();
      const members: string[] = Array.isArray(data.members)
        ? data.members.filter((m: unknown): m is string => typeof m === "string")
        : [];
      const groupName = typeof data.name === "string" ? data.name : "그룹";

      for (const uid of members) {
        if (!wrote.has(`${uid}_${doc.id}`)) {
          targets.push({ uid, groupId: doc.id, groupName });
        }
      }
    });

    if (targets.length === 0) {
      logger.info("모든 멤버가 오늘 기록을 작성함");
      return;
    }

    // ── 4. 토큰 조회
    // 그룹 수만큼 쿼리를 돌리면 읽기가 폭증하므로 users를 1회 조회해 메모리에서 조합한다.
    const usersSnap = await db.collection("users").get();
    const tokenByUid = new Map<string, string>();
    usersSnap.forEach((doc) => {
      const token = doc.data().fcmToken;
      if (typeof token === "string" && token.length > 0) {
        tokenByUid.set(doc.id, token);
      }
    });

    // ── 5. 메시지 조립
    // data-only 페이로드를 쓴다. 앱이 백그라운드여도 onMessageReceived가 호출되어
    // 그룹 딥링크가 붙은 알림을 앱이 직접 만들 수 있다.
    // 대신 Doze 모드를 깨우려면 android.priority = high 가 필요하다.
    const messages: Message[] = [];
    const messageUids: string[] = []; // 실패 응답을 uid로 되짚기 위한 병렬 배열

    for (const t of targets) {
      const token = tokenByUid.get(t.uid);
      if (!token) continue; // 토큰 없음(로그아웃 상태 등) → 건너뜀

      messages.push({
        token,
        // FCM data 페이로드의 값은 전부 문자열이어야 한다
        data: {
          title: "오늘 운동 기록이 비어 있어요",
          body: `${t.groupName}에 오늘 기록을 남겨보세요`,
          groupId: t.groupId,
        },
        android: { priority: "high" },
      });
      messageUids.push(t.uid);
    }

    if (messages.length === 0) {
      logger.info(`미작성자 ${targets.length}명, 토큰 보유자 0명`);
      return;
    }

    // ── 6. 발송
    const deadTokenUids = new Set<string>();
    let success = 0;
    let failure = 0;

    for (let i = 0; i < messages.length; i += SEND_CHUNK) {
      const chunk = messages.slice(i, i + SEND_CHUNK);
      const res = await getMessaging().sendEach(chunk);
      success += res.successCount;
      failure += res.failureCount;

      res.responses.forEach((r, idx) => {
        if (r.success) return;

        const code = r.error?.code;
        const uid = messageUids[i + idx];
        logger.warn(`발송 실패 uid=${uid} code=${code}`);

        // 앱 삭제는 로그아웃을 거치지 않아 clearFcmToken이 호출되지 않는다.
        // 죽은 토큰을 정리할 수 있는 지점은 사실상 여기뿐이다.
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token"
        ) {
          deadTokenUids.add(uid);
        }
      });
    }

    // ── 7. 죽은 토큰 정리
    if (deadTokenUids.size > 0) {
      const batch = db.batch();
      for (const uid of deadTokenUids) {
        // update()는 문서가 없으면 실패하므로 set(merge)로 삭제 센티넬 전송
        batch.set(
          db.collection("users").doc(uid),
          { fcmToken: FieldValue.delete() },
          { merge: true }
        );
      }
      await batch.commit();
    }

    logger.info(
      `리마인더 완료 성공=${success} 실패=${failure} 토큰정리=${deadTokenUids.size}`
    );
  }
);