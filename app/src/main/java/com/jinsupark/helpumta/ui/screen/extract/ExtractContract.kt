package com.jinsupark.helpumta.ui.screen.extract

import android.graphics.Bitmap
import com.jinsupark.helpumta.domain.model.ExtractSlot
import com.jinsupark.helpumta.domain.model.ExtractStyle

object ExtractContract {

    data class UiState(
        val slots: List<ExtractSlot> = emptyList(),
        val groupName: String = "",          // 헤더용. 못 불러오면 빈 문자열 → 날짜만 표시
        val selectedDate: Long = 0L,
        val style: ExtractStyle = ExtractStyle(),
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    )

    sealed interface Intent {
        data class Load(val groupId: String, val dateMillis: Long) : Intent
        data class UpdateStyle(val style: ExtractStyle) : Intent
        data class Save(val bitmap: Bitmap) : Intent
    }

    sealed interface SideEffect {
        data class ShowToast(val message: String) : SideEffect
        data object NavigateBack : SideEffect
    }
}