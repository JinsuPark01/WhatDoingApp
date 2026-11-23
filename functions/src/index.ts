import { onSchedule } from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import { initializeApp } from "firebase-admin/app";
import {
  getFirestore,
  type DocumentReference,
  type Timestamp,
} from "firebase-admin/firestore";
import { getMessaging, type Message } from "firebase-admin/messaging";

initializeApp();

const db = getFirestore();

const KST_OFFSET_MS = 9 * 60 * 60 * 1000;
const SEND_CHUNK = 500; // sendEach 1회 제한
const READ_CHUNK = 100; // getAll 한 번에 넘길 문서 수 (요청 크기를 무난하게 유지)

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

/**
 * 토큰은 users 문서가 아니라 본인만 읽는 하위 문서에 있다.
 * (users는 로그인한 누구나 읽을 수 있어서 토큰을 두면 노출된다)
 * 앱(UserRepositoryImpl)과 경로가 같아야 한다.
 */
function fcmDocRef(uid: string): DocumentReference {
  return db.collection("users").doc(uid).collection("private").doc("fcm");
}

interface Target {
  uid: string;
  groupId: string;
  groupName: string;
}

interface TokenEntry {
  token: string;
  // 정리할 때 "읽은 뒤로 바뀌지 않았을 때만" 지우기 위한 기준
  updateTime: Timestamp;
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

    // ── 4. 사용자별로 묶기
    // 발송 단위는 그룹이 아니라 사용자다.
    // 그룹마다 보내면 3개 그룹에 참여한 사람은 매일 저녁 알림 3개를 받는다.
    const byUid = new Map<string, Target[]>();
    for (const t of targets) {
      const list = byUid.get(t.uid);
      if (list) list.push(t);
      else byUid.set(t.uid, [t]);
    }

    // ── 5. 토큰 조회 — 보낼 사람 것만
    // users 전체를 읽으면 사용자 수만큼 읽기가 나간다. 대상자 토큰 문서만 getAll로 한 번에.
    const tokenByUid = new Map<string, TokenEntry>();
    const uids = [...byUid.keys()];
    for (let i = 0; i < uids.length; i += READ_CHUNK) {
      const refs = uids.slice(i, i + READ_CHUNK).map(fcmDocRef);
      const snaps = await db.getAll(...refs);
      for (const snap of snaps) {
        const token = snap.get("token");
        // 결과 순서에 기대지 않고 경로에서 uid를 꺼낸다 (users/{uid}/private/fcm)
        const uid = snap.ref.parent.parent?.id;
        if (
          snap.exists &&
          uid &&
          snap.updateTime &&
          typeof token === "string" &&
          token.length > 0
        ) {
          tokenByUid.set(uid, { token, updateTime: snap.updateTime });
        }
      }
    }

    // ── 6. 메시지 조립
    // data-only 페이로드를 쓴다. 앱이 백그라운드여도 onMessageReceived가 호출되어
    // 앱이 직접 알림을 만들 수 있다.
    // 대신 Doze 모드를 깨우려면 android.priority = high 가 필요하다.
    const messages: Message[] = [];
    const messageUids: string[] = []; // 실패 응답을 uid로 되짚기 위한 병렬 배열

    for (const [uid, list] of byUid) {
      const entry = tokenByUid.get(uid);
      if (!entry) continue; // 토큰 없음(로그아웃 상태 등) → 건너뜀

      const single = list.length === 1;

      messages.push({
        token: entry.token,
        // FCM data 페이로드의 값은 전부 문자열이어야 한다
        data: {
          title: "오늘 운동 기록이 비어 있어요",
          body: single
            ? `${list[0].groupName}에 오늘 기록을 남겨보세요`
            : `${list.length}개 그룹에 오늘 기록이 비어 있어요`,
          // 미작성 그룹이 하나면 거기로 바로 보낸다.
          // 여러 개면 어디로 갈지 고를 수 없으므로 키를 빼서 홈으로 열리게 한다.
          ...(single ? { groupId: list[0].groupId } : {}),
        },
        android: { priority: "high" },
      });
      messageUids.push(uid);
    }

    if (messages.length === 0) {
      logger.info(`미작성자 ${byUid.size}명, 토큰 보유자 0명`);
      return;
    }

    // ── 7. 발송
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

    // ── 8. 죽은 토큰 정리
    // 읽은 뒤 사용자가 새 토큰을 등록했을 수 있다 → 문서가 그대로일 때만 지운다(lastUpdateTime 전제조건).
    // 배치는 하나만 실패해도 전부 취소되므로 개별 삭제로 돌리고, 전제조건 실패는 정상 상황으로 본다.
    let cleaned = 0;
    if (deadTokenUids.size > 0) {
      const results = await Promise.allSettled(
        [...deadTokenUids].map((uid) => {
          const entry = tokenByUid.get(uid);
          if (!entry) return Promise.resolve();
          return fcmDocRef(uid).delete({ lastUpdateTime: entry.updateTime });
        })
      );
      cleaned = results.filter((r) => r.status === "fulfilled").length;
    }

    logger.info(
      `리마인더 완료 성공=${success} 실패=${failure} 토큰정리=${cleaned}/${deadTokenUids.size}`
    );
  }
);