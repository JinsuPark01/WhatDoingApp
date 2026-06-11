package com.jinsupark.helpumta.ui.screen.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.usecase.CreateRecordUseCase
import com.jinsupark.helpumta.domain.usecase.GetRecordByIdUseCase
import com.jinsupark.helpumta.domain.usecase.UpdateRecordUseCase
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
    private val createRecordUseCase: CreateRecordUseCase,
    private val updateRecordUseCase: UpdateRecordUseCase,      // 추가
    private val getRecordByIdUseCase: GetRecordByIdUseCase     // 추가
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecordContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<RecordContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: RecordContract.Intent) {
        when (intent) {
            is RecordContract.Intent.Initialize -> {
                if (_uiState.value.groupId.isNotBlank()) return  // 중복 방어
                _uiState.update { it.copy(
                    groupId = intent.groupId,
                    recordId = intent.recordId
                )}
                // 수정 모드면 기존 기록 로드
                if (intent.recordId != null) {
                    loadRecord(intent.recordId)
                }
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
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val state = _uiState.value
            val result = if (state.isEditMode) {
                updateRecordUseCase(
                    recordId = state.recordId!!,
                    workoutType = state.workoutType,
                    workoutDuration = state.workoutDuration.toInt(),
                    imageUri = state.imageUri,
                    comment = state.comment
                ).map { state.recordId }  // Result<Unit> → Result<String> 맞추기 (아래 설명)
            } else {
                createRecordUseCase(
                    groupId = state.groupId,
                    workoutType = state.workoutType,
                    workoutDuration = state.workoutDuration.toInt(),
                    imageUri = state.imageUri,
                    comment = state.comment
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    val msg = if (state.isEditMode) "기록을 수정했어요!" else "기록을 저장했어요!"
                    _sideEffect.emit(RecordContract.SideEffect.ShowToast(msg))
                    _sideEffect.emit(RecordContract.SideEffect.NavigateBack)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "저장에 실패했습니다"
                    )}
                }
            )
        }
    }

    private fun loadRecord(recordId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitializing = true) }
            getRecordByIdUseCase(recordId).fold(
                onSuccess = { record ->
                    _uiState.update { it.copy(
                        isInitializing = false,
                        workoutType = record.workoutType,
                        workoutDuration = record.workoutDuration.toString(),
                        imageUri = record.imageUrl.ifBlank { null },  // 기존 URL을 imageUri로
                        comment = record.comment
                    )}
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isInitializing = false,
                        errorMessage = e.message ?: "기록을 불러오지 못했습니다"
                    )}
                }
            )
        }
    }
}