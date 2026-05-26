package com.example.whatdoing.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatdoing.domain.usecase.GetGroupDetailUseCase
import com.example.whatdoing.domain.usecase.GetRecordsByGroupUseCase
import com.example.whatdoing.domain.usecase.HasWroteTodayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val getRecordsByGroupUseCase: GetRecordsByGroupUseCase,
    private val hasWroteTodayUseCase: HasWroteTodayUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<GroupDetailContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: GroupDetailContract.Intent) {
        when (intent) {
            is GroupDetailContract.Intent.LoadGroupDetail -> loadGroupDetail(intent.groupId)
            GroupDetailContract.Intent.NavigateToRecord -> navigateToRecord()
        }
    }

    private fun loadGroupDetail(groupId: String) {
        if (_uiState.value.groupId == groupId && _uiState.value.group != null) return

        viewModelScope.launch {
            // 3번 - 이전 에러 메시지 초기화
            _uiState.update { it.copy(
                isLoading = true,
                groupId = groupId,
                errorMessage = null,
                recordsErrorMessage = null
            )}

            // 2번 - supervisorScope로 변경 (하나 실패해도 다른 거 진행)
            supervisorScope {
                val groupDeferred = async { getGroupDetailUseCase(groupId) }
                val recordsDeferred = async { getRecordsByGroupUseCase(groupId) }
                val wroteDeferred = async { hasWroteTodayUseCase(groupId) }

                val groupResult = groupDeferred.await()
                val recordsResult = recordsDeferred.await()
                val wroteResult = wroteDeferred.await()

                groupResult.fold(
                    onSuccess = { group ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            group = group,
                            records = recordsResult.getOrDefault(emptyList()),
                            // 4번 - isFailure로 명확하게
                            recordsErrorMessage = if (recordsResult.isFailure) {
                                "기록을 불러오지 못했습니다"
                            } else null,
                            hasWroteToday = wroteResult.getOrDefault(false)
                        )}
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "그룹을 불러올 수 없습니다"
                        )}
                    }
                )
            }
        }
    }

    private fun navigateToRecord() {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return

        viewModelScope.launch {
            _sideEffect.emit(GroupDetailContract.SideEffect.NavigateToRecord(groupId))
        }
    }
}