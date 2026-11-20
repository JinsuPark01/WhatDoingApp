package com.jinsupark.helpumta.ui.screen.group

object GroupCreateContract {

    data class UiState(
        val groupId: String? = null,         // null이면 생성, 있으면 수정
        val name: String = "",
        val description: String = "",
        // 원격 URL(기존 이미지) 또는 로컬 URI(새로 고른 이미지). null이면 이미지 없음
        val imageUri: String? = null,
        val isInitializing: Boolean = false, // 수정 모드에서 기존 정보 로드 중
        val isLoading: Boolean = false
    ) {
        val isEditMode: Boolean
            get() = groupId != null

        val isSubmitEnabled: Boolean
            get() = name.isNotBlank()
    }

    sealed interface Intent {
        data class Initialize(val groupId: String?) : Intent
        data class UpdateName(val name: String) : Intent
        data class UpdateDescription(val description: String) : Intent
        data class UpdateImage(val uri: String?) : Intent
        data object Submit : Intent
    }

    sealed interface SideEffect {
        data class NavigateToGroup(val groupId: String) : SideEffect
        data object GroupUpdated : SideEffect
        data object NavigateBack : SideEffect
        data class ShowToast(val message: String) : SideEffect
    }
}