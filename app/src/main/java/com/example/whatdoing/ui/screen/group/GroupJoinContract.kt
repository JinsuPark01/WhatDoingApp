package com.example.whatdoing.ui.screen.group

import com.example.whatdoing.domain.model.Group

object GroupJoinContract {

    data class UiState(
        val groupId: String = "",
        val group: Group? = null,
        val isLoading: Boolean = false,
        val isJoining: Boolean = false,
        val errorMessage: String? = null
    )

    sealed interface Intent {
        data class Initialize(val groupId: String) : Intent
        data object SubmitJoin : Intent
    }

    sealed interface SideEffect {
        data class NavigateToGroup(val groupId: String) : SideEffect
        data class ShowToast(val message: String) : SideEffect
    }
}