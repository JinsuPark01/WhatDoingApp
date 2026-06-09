package com.example.whatdoing.ui.screen.group

import com.example.whatdoing.domain.model.Group
import com.example.whatdoing.domain.model.WorkoutRecord

object GroupDetailContract {

    data class UiState(
        val groupId: String = "",
        val group: Group? = null,
        val records: List<WorkoutRecord> = emptyList(),
        val hasWroteToday: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val recordsErrorMessage: String? = null,
        val isLeaving: Boolean = false,
        val selectedDate: Long = 0L   // 선택된 날짜 (로컬 00:00 millis)
    )

    sealed interface Intent {
        data class LoadGroupDetail(val groupId: String) : Intent
        data object NavigateToRecord : Intent
        data object LeaveGroup : Intent
        data class SelectDate(val utcMillis: Long) : Intent   // 달력에서 절대 날짜 선택
        data class MoveDay(val offset: Int) : Intent          // 화살표 상대 이동
        data object RefreshToToday : Intent
    }

    sealed interface SideEffect {
        data class NavigateToRecord(val groupId: String) : SideEffect
        data class ShowToast(val message: String) : SideEffect
        data object NavigateToHome : SideEffect
    }
}