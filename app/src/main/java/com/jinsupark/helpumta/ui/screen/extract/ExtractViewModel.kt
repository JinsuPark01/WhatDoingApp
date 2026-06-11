package com.jinsupark.helpumta.ui.screen.extract

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.data.util.ImageSaver
import com.jinsupark.helpumta.domain.usecase.GetDailyExtractUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ExtractViewModel @Inject constructor(
    private val getDailyExtractUseCase: GetDailyExtractUseCase,
    private val imageSaver: ImageSaver
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<ExtractContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: ExtractContract.Intent) {
        when (intent) {
            is ExtractContract.Intent.Load -> load(intent.groupId, intent.dateMillis)
            is ExtractContract.Intent.Save -> save(intent.bitmap)
        }
    }

    private fun load(groupId: String, dateMillis: Long) {
        val start = startOfDay(dateMillis)
        val end = endOfDay(start)

        _uiState.update { it.copy(isLoading = true, selectedDate = start, errorMessage = null) }

        viewModelScope.launch {
            getDailyExtractUseCase(groupId, start, end).fold(
                onSuccess = { slots ->
                    _uiState.update { it.copy(isLoading = false, slots = slots) }
                },
                onFailure = {
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "추출 데이터를 불러오지 못했어요"
                    )}
                }
            )
        }
    }

    private fun save(bitmap: android.graphics.Bitmap) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val fileName = "helpumta_${System.currentTimeMillis()}"
            imageSaver.saveToGallery(bitmap, fileName).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false) }
                    _sideEffect.emit(ExtractContract.SideEffect.ShowToast("갤러리에 저장했어요!"))
                    _sideEffect.emit(ExtractContract.SideEffect.NavigateBack)
                },
                onFailure = {
                    _uiState.update { it.copy(isSaving = false) }
                    _sideEffect.emit(ExtractContract.SideEffect.ShowToast("저장에 실패했어요. 다시 시도해주세요."))
                }
            )
        }
    }

    private fun startOfDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun endOfDay(startMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = startMillis
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
}