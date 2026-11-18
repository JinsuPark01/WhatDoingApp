package com.jinsupark.helpumta.widget

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.jinsupark.helpumta.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

// --- 상태 모델 ---

internal data class GroupStatus(
    val groupId: String,
    val name: String,
    val memberCount: Int,
    val wroteCount: Int,
    val didIWrite: Boolean
)

internal sealed interface TodayState {
    data object LoggedOut : TodayState
    data object NoGroups : TodayState
    data object Error : TodayState
    data class Content(val groups: List<GroupStatus>) : TodayState
}

internal sealed interface WeekState {
    data object LoggedOut : WeekState
    data object NoGroups : WeekState
    data object Error : WeekState
    /** doneDays: 월=0 ... 일=6 중 기록한 요일 */
    data class Content(val doneDays: Set<Int>, val todayIndex: Int) : WeekState
}

/**
 * 세 위젯이 공유하는 스냅샷.
 *
 * 위젯 상태(Preferences)에 JSON으로 저장된다. 컴포지션 안에서 상태를 읽어야
 * updateAll이 새 데이터로 다시 그린다 — 컴포지션 밖에서 읽으면 캡처된 옛 값이 고정된다.
 */
internal data class WidgetSnapshot(
    val status: Status,
    val groups: List<GroupStatus> = emptyList(),
    val weekDoneDays: Set<Int> = emptySet(),
    val todayIndex: Int = 0
) {
    enum class Status { OK, LOGGED_OUT, NO_GROUPS, ERROR, EMPTY }

    fun toTodayState(): TodayState = when (status) {
        Status.OK -> TodayState.Content(groups)
        Status.LOGGED_OUT -> TodayState.LoggedOut
        Status.NO_GROUPS -> TodayState.NoGroups
        // EMPTY는 아직 적재 전 — 사용자에겐 오류와 구분이 의미 없다
        Status.ERROR, Status.EMPTY -> TodayState.Error
    }

    fun toWeekState(): WeekState = when (status) {
        Status.OK -> WeekState.Content(weekDoneDays, todayIndex)
        Status.LOGGED_OUT -> WeekState.LoggedOut
        Status.NO_GROUPS -> WeekState.NoGroups
        Status.ERROR, Status.EMPTY -> WeekState.Error
    }

    fun toJson(): String {
        val groupArray = JSONArray()
        groups.forEach { g ->
            groupArray.put(
                JSONObject()
                    .put("id", g.groupId)
                    .put("name", g.name)
                    .put("members", g.memberCount)
                    .put("wrote", g.wroteCount)
                    .put("mine", g.didIWrite)
            )
        }
        val weekArray = JSONArray()
        weekDoneDays.sorted().forEach { weekArray.put(it) }

        return JSONObject()
            .put("status", status.name)
            .put("groups", groupArray)
            .put("week", weekArray)
            .put("todayIndex", todayIndex)
            .toString()
    }

    companion object {
        val EMPTY = WidgetSnapshot(Status.EMPTY)

        fun fromJson(raw: String?): WidgetSnapshot {
            if (raw.isNullOrBlank()) return EMPTY
            return runCatching {
                val root = JSONObject(raw)

                val groupArray = root.optJSONArray("groups") ?: JSONArray()
                val groups = (0 until groupArray.length()).map { i ->
                    val o = groupArray.getJSONObject(i)
                    GroupStatus(
                        groupId = o.getString("id"),
                        name = o.getString("name"),
                        memberCount = o.getInt("members"),
                        wroteCount = o.getInt("wrote"),
                        didIWrite = o.getBoolean("mine")
                    )
                }

                val weekArray = root.optJSONArray("week") ?: JSONArray()
                val week = (0 until weekArray.length()).map { weekArray.getInt(it) }.toSet()

                WidgetSnapshot(
                    status = Status.valueOf(root.getString("status")),
                    groups = groups,
                    weekDoneDays = week,
                    todayIndex = root.optInt("todayIndex", 0)
                )
            }.getOrDefault(EMPTY)
        }
    }
}

// --- 로딩 ---

internal object WidgetData {

    /** 세 위젯이 필요한 데이터를 한 번에 조회한다 */
    suspend fun load(context: Context): WidgetSnapshot {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        val uid = entryPoint.getCurrentUserIdUseCase()()
            ?: return WidgetSnapshot(WidgetSnapshot.Status.LOGGED_OUT)

        val groups = entryPoint.getGroupsUseCase()().getOrNull()
            ?: return WidgetSnapshot(WidgetSnapshot.Status.ERROR)

        if (groups.isEmpty()) return WidgetSnapshot(WidgetSnapshot.Status.NO_GROUPS)

        val today = startOfToday()
        val tomorrow = addDays(today, 1)
        val weekStart = startOfWeek()
        val weekEnd = addDays(weekStart, 7)

        val getRecords = entryPoint.getRecordsByGroupUseCase()
        val getDates = entryPoint.getMyRecordDatesUseCase()

        // 그룹 수만큼 쿼리가 나가지만 한 사용자의 그룹은 보통 1~3개다.
        // userId + createdAt 범위만으로 조회하면 새 복합 인덱스가 필요해지므로
        // 그룹별 조회로 기존 인덱스를 재사용한다. 병렬로 받는다.
        val perGroup = coroutineScope {
            groups.map { group ->
                async {
                    val todayRecords = getRecords(group.id, today, tomorrow)
                        .getOrDefault(emptyList())
                    val writers = todayRecords.map { it.userId }.toSet()

                    val myWeekDates = getDates(group.id, weekStart, weekEnd)
                        .getOrDefault(emptyList())

                    GroupStatus(
                        groupId = group.id,
                        name = group.name,
                        memberCount = group.memberCount,
                        wroteCount = writers.size,
                        didIWrite = writers.contains(uid)
                    ) to myWeekDates
                }
            }.awaitAll()
        }

        // 그룹이 달라도 같은 날이면 하나로 — "그날 운동했나"가 기준
        val weekDays = perGroup
            .flatMap { it.second }
            .map { dayIndexInWeek(it) }
            .toSet()

        return WidgetSnapshot(
            status = WidgetSnapshot.Status.OK,
            groups = perGroup.map { it.first },
            weekDoneDays = weekDays,
            todayIndex = dayIndexInWeek(System.currentTimeMillis())
        )
    }

    // --- 날짜 헬퍼 ---

    private fun startOfToday(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun addDays(millis: Long, days: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            add(Calendar.DAY_OF_MONTH, days)
        }.timeInMillis

    /** 이번 주 월요일 00:00 — 통계·캘린더와 주 시작 기준을 통일 */
    private fun startOfWeek(): Long =
        addDays(startOfToday(), -dayIndexInWeek(System.currentTimeMillis()))

    /** 월=0 ... 일=6 */
    private fun dayIndexInWeek(millis: Long): Int {
        val dow = Calendar.getInstance().apply { timeInMillis = millis }
            .get(Calendar.DAY_OF_WEEK)
        return (dow + 5) % 7
    }
}

// --- 인텐트 (세 위젯 공용) ---

internal fun mainActivityIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

internal fun groupIntent(groupId: String): Intent =
    // 알림과 같은 App Links 경로. 이미 멤버이므로 GroupJoin이 상세로 리다이렉트한다.
    Intent(
        Intent.ACTION_VIEW,
        "https://whatdoing-c4457.web.app/group/$groupId".toUri()
    ).apply {
        setPackage("com.jinsupark.helpumta")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }