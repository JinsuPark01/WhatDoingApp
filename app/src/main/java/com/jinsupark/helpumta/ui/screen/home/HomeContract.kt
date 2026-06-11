package com.jinsupark.helpumta.ui.screen.home

import com.jinsupark.helpumta.domain.model.Group

object HomeContract {

    data class UiState(
        val isLoading: Boolean = false,
        val groups: List<Group> = emptyList(),
        val errorMessage: String? = null
    )

    sealed interface Intent {
        data object LoadGroups : Intent
        data class NavigateToGroup(val groupId: String) : Intent
        data object NavigateToCreateGroup : Intent
    }

    sealed interface SideEffect {
        data class NavigateToGroup(val groupId: String) : SideEffect
        data object NavigateToCreateGroup : SideEffect
    }
}