package com.example.whatdoing.ui.screen.record

object RecordContract {

    data class UiState(
        val groupId: String = "",
        val recordId: String? = null,        // 추가: null=작성, 있음=수정
        val workoutType: String = "",
        val workoutDuration: String = "",
        val imageUri: String? = null,
        val comment: String = "",
        val isLoading: Boolean = false,
        val isInitializing: Boolean = false,  // 추가: 수정 시 기존 데이터 로딩
        val errorMessage: String? = null
    ) {
        val isEditMode: Boolean get() = recordId != null   // 추가: 모드 판별

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