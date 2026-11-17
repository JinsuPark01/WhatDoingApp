package com.jinsupark.helpumta.ui.screen.group

import com.jinsupark.helpumta.domain.model.Group
import com.jinsupark.helpumta.domain.model.GroupStats
import com.jinsupark.helpumta.domain.model.WorkoutRecord

object GroupDetailContract {

    enum class Tab { RECORDS, STATS }

    data class UiState(
        val groupId: String = "",
        val currentUserId: String? = null,
        val group: Group? = null,
        val records: List<WorkoutRecord> = emptyList(),
        val hasWroteToday: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val recordsErrorMessage: String? = null,
        val isLeaving: Boolean = false,
        val selectedDate: Long = 0L,   // 선택된 날짜 (로컬 00:00 millis)

        // 통계 탭
        val selectedTab: Tab = Tab.RECORDS,
        val stats: GroupStats? = null,          // null이면 아직 미로드
        val isStatsLoading: Boolean = false,
        val statsErrorMessage: String? = null
    )

    sealed interface Intent {
        data class LoadGroupDetail(val groupId: String) : Intent
        data object NavigateToRecord : Intent
        data object LeaveGroup : Intent
        data class SelectDate(val utcMillis: Long) : Intent   // 달력에서 절대 날짜 선택
        data class MoveDay(val offset: Int) : Intent          // 화살표 상대 이동
        data object RefreshToToday : Intent
        data class DeleteRecord(val recordId: String) : Intent
        data object AlreadyWroteToday : Intent
        data class SelectTab(val tab: Tab) : Intent
    }

    sealed interface SideEffect {
        data class NavigateToRecord(val groupId: String) : SideEffect
        data class ShowToast(val message: String) : SideEffect
        data object NavigateToHome : SideEffect
    }
}