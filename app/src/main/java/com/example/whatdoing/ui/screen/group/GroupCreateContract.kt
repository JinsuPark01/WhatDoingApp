package com.example.whatdoing.ui.screen.group

object GroupCreateContract {

    data class UiState(
        val name: String = "",
        val description: String = "",
        val imageUri: String? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) {
        val isCreateEnabled: Boolean
            get() = name.isNotBlank()
    }

    sealed interface Intent {
        data class UpdateName(val name: String) : Intent
        data class UpdateDescription(val description: String) : Intent
        data class UpdateImage(val uri: String?) : Intent
        data object SubmitCreate : Intent
    }

    sealed interface SideEffect {
        data class NavigateToGroup(val groupId: String) : SideEffect
        data class ShowToast(val message: String) : SideEffect
    }
}