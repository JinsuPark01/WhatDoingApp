package com.example.whatdoing.ui.screen.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatdoing.domain.usecase.CreateRecordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val createRecordUseCase: CreateRecordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<RecordContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: RecordContract.Intent) {
        when (intent) {
            is RecordContract.Intent.Initialize -> {
                // 3번 - 중복 방어
                if (_uiState.value.groupId.isNotBlank()) return
                _uiState.update { it.copy(groupId = intent.groupId) }
            }
            is RecordContract.Intent.UpdateWorkoutType -> {
                _uiState.update { it.copy(
                    workoutType = intent.type,
                    errorMessage = null
                )}
            }
            is RecordContract.Intent.UpdateDuration -> {
                val filtered = intent.duration.filter { it.isDigit() }
                _uiState.update { it.copy(workoutDuration = filtered) }
            }
            is RecordContract.Intent.UpdateImage -> {
                _uiState.update { it.copy(imageUri = intent.uri) }
            }
            is RecordContract.Intent.UpdateComment -> {
                _uiState.update { it.copy(comment = intent.comment) }
            }
            RecordContract.Intent.SubmitRecord -> submitRecord()
        }
    }

    private fun submitRecord() {
        // 1번 - 중복 제출 방어
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                errorMessage = null
            )}

            val state = _uiState.value
            val result = createRecordUseCase(
                groupId = state.groupId,
                workoutType = state.workoutType,
                workoutDuration = state.workoutDuration.toInt(),
                imageUri = state.imageUri,
                comment = state.comment
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    // 7번 - 토스트 표시
                    _sideEffect.emit(RecordContract.SideEffect.ShowToast("기록을 저장했어요!"))
                    _sideEffect.emit(RecordContract.SideEffect.NavigateBack)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "기록 저장에 실패했습니다"
                    )}
                }
            )
        }
    }
}