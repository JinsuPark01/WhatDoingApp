package com.example.whatdoing.ui.screen.record

object RecordContract {

    data class UiState(
        val groupId: String = "",
        val workoutType: String = "",
        val workoutDuration: String = "",
        val imageUri: String? = null,
        val comment: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    ) {
        val isSubmitEnabled: Boolean
            get() = workoutType.isNotBlank() &&
                    (workoutDuration.toIntOrNull() ?: 0) > 0
    }

    sealed interface Intent {
        data class Initialize(val groupId: String) : Intent
        data class UpdateWorkoutType(val type: String) : Intent
        data class UpdateDuration(val duration: String) : Intent
        data class UpdateImage(val uri: String?) : Intent
        data class UpdateComment(val comment: String) : Intent
        data object SubmitRecord : Intent
    }

    sealed interface SideEffect {
        data object NavigateBack : SideEffect
        data class ShowToast(val message: String) : SideEffect
    }
}