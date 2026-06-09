package com.example.whatdoing.ui.screen.extract

import android.graphics.Bitmap
import com.example.whatdoing.domain.model.ExtractSlot

object ExtractContract {

    data class UiState(
        val slots: List<ExtractSlot> = emptyList(),
        val selectedDate: Long = 0L,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,   // 추가
        val errorMessage: String? = null
    )

    sealed interface Intent {
        data class Load(val groupId: String, val dateMillis: Long) : Intent
        data class Save(val bitmap: Bitmap) : Intent   // 추가
    }

    sealed interface SideEffect {
        data class ShowToast(val message: String) : SideEffect
        data object NavigateBack : SideEffect
    }
}