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
        val isLeaving: Boolean = false   // 추가: 나가기 진행 중
    )

    sealed interface Intent {
        data class LoadGroupDetail(val groupId: String) : Intent
        data object NavigateToRecord : Intent
        data object LeaveGroup : Intent   // 추가
    }

    sealed interface SideEffect {
        data class NavigateToRecord(val groupId: String) : SideEffect
        data class ShowToast(val message: String) : SideEffect
        data object NavigateToHome : SideEffect   // 추가: 나간 후 홈으로
    }
}