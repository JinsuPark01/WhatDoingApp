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
        val recordsErrorMessage: String? = null
    )

    sealed interface Intent {
        data class LoadGroupDetail(val groupId: String) : Intent
        data object NavigateToRecord : Intent
    }

    sealed interface SideEffect {
        data class NavigateToRecord(val groupId: String) : SideEffect
        data class ShowToast(val message: String) : SideEffect
    }
}