package com.jinsupark.helpumta.ui.screen.extract

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.data.util.ImageSaver
import com.jinsupark.helpumta.domain.model.ExtractStyle
import com.jinsupark.helpumta.domain.usecase.GetDailyExtractUseCase
import com.jinsupark.helpumta.domain.usecase.GetExtractStyleUseCase
import com.jinsupark.helpumta.domain.usecase.GetGroupDetailUseCase
import com.jinsupark.helpumta.domain.usecase.SaveExtractStyleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ExtractViewModel @Inject constructor(
    private val getDailyExtractUseCase: GetDailyExtractUseCase,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val getExtractStyleUseCase: GetExtractStyleUseCase,
    private val saveExtractStyleUseCase: SaveExtractStyleUseCase,
    private val imageSaver: ImageSaver
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<ExtractContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    init {
        observeStyle()
    }

    fun handleIntent(intent: ExtractContract.Intent) {
        when (intent) {
            is ExtractContract.Intent.Load -> load(intent.groupId, intent.dateMillis)
            is ExtractContract.Intent.UpdateStyle -> updateStyle(intent.style)
            is ExtractContract.Intent.Save -> save(intent.bitmap)
        }
    }

    // 저장소가 단일 진실 소스 — 바꾸면 저장되고, 저장된 값이 흘러와 화면에 반영된다
    private fun observeStyle() {
        viewModelScope.launch {
            getExtractStyleUseCase().collect { style ->
                _uiState.update { it.copy(style = style) }
            }
        }
    }

    private fun updateStyle(style: ExtractStyle) {
        viewModelScope.launch { saveExtractStyleUseCase(style) }
    }

    private fun load(groupId: String, dateMillis: Long) {
        val start = startOfDay(dateMillis)
        val end = endOfDay(start)

        _uiState.update { it.copy(isLoading = true, selectedDate = start, errorMessage = null) }

        viewModelScope.launch {
            supervisorScope {
                val slotsDeferred = async { getDailyExtractUseCase(groupId, start, end) }
                val groupDeferred = async { getGroupDetailUseCase(groupId) }

                val slotsResult = slotsDeferred.await()
                // 그룹명은 헤더 장식이라 실패해도 추출 자체는 진행 (날짜만 표시)
                val groupName = groupDeferred.await().getOrNull()?.name.orEmpty()

                slotsResult.fold(
                    onSuccess = { slots ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            slots = slots,
                            groupName = groupName
                        )}
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
    }

    private fun save(bitmap: Bitmap) {
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