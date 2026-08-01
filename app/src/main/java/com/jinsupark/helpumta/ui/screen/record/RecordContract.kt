package com.jinsupark.helpumta.ui.screen.record

object RecordContract {

    data class UiState(
        val groupId: String = "",
        val recordId: String? = null,
        val workoutType: String = "",
        val workoutDuration: String = "",
        val imageUri: String? = null,
        val comment: String = "",
        val isLoading: Boolean = false,
        val isInitializing: Boolean = false
    ) {
        val isEditMode: Boolean get() = recordId != null

        val isSubmitEnabled: Boolean
            get() = workoutType.isNotBlank() &&
                    (workoutDuration.toIntOrNull() ?: 0) > 0
    }

    sealed interface Intent {
        data class Initialize(val groupId: String, val recordId: String?) : Intent  // recordId 추가
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