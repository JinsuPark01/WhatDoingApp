package com.jinsupark.helpumta.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.usecase.GetGroupDetailUseCase
import com.jinsupark.helpumta.domain.usecase.JoinGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupJoinViewModel @Inject constructor(
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val joinGroupUseCase: JoinGroupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupJoinContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<GroupJoinContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: GroupJoinContract.Intent) {
        when (intent) {
            is GroupJoinContract.Intent.Initialize -> {
                // 5번 - 같은 groupId만 막음 (다른 groupId는 갱신 허용)
                if (_uiState.value.groupId == intent.groupId) return
                loadGroupInfo(intent.groupId)
            }
            GroupJoinContract.Intent.SubmitJoin -> joinGroup()
        }
    }

    private fun loadGroupInfo(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                groupId = groupId,
                errorMessage = null
            )}

            val result = getGroupDetailUseCase(groupId)

            result.fold(
                onSuccess = { group ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        group = group
                    )}
                },
                onFailure = {
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "그룹을 찾을 수 없어요"
                    )}
                }
            )
        }
    }

    private fun joinGroup() {
        if (_uiState.value.isJoining) return

        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, errorMessage = null) }

            val groupId = _uiState.value.groupId
            val result = joinGroupUseCase(groupId)

            result.fold(
                onSuccess = { joined ->
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            // 새로 가입한 경우에만 +1
                            group = if (joined) {
                                it.group?.copy(
                                    memberCount = it.group.memberCount + 1
                                )
                            } else {
                                it.group
                            }
                        )
                    }
                    _sideEffect.emit(GroupJoinContract.SideEffect.ShowToast("그룹에 참여했어요!"))
                    _sideEffect.emit(GroupJoinContract.SideEffect.NavigateToGroup(groupId))
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isJoining = false,
                        errorMessage = e.message ?: "그룹 참여에 실패했어요"
                    )}
                }
            )
        }
    }
}