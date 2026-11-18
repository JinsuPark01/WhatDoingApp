package com.jinsupark.helpumta.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.usecase.GetGroupDetailUseCase
import com.jinsupark.helpumta.domain.usecase.GetGroupsUseCase
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
    private val getGroupsUseCase: GetGroupsUseCase,
    private val joinGroupUseCase: JoinGroupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupJoinContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<GroupJoinContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: GroupJoinContract.Intent) {
        when (intent) {
            is GroupJoinContract.Intent.Initialize -> {
                // 같은 groupId만 막음 (다른 groupId는 갱신 허용)
                if (_uiState.value.groupId == intent.groupId) return
                initialize(intent.groupId)
            }
            GroupJoinContract.Intent.SubmitJoin -> joinGroup()
        }
    }

    private fun initialize(groupId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    groupId = groupId,
                    errorMessage = null
                )
            }

            // 이 화면은 두 경로로 들어온다.
            //  1) 초대 링크 (https://.../group/{id})  — 보통 비멤버
            //  2) 리마인더 알림 탭                    — 항상 멤버
            // 이미 멤버라면 "참여하시겠습니까?"를 보여줄 이유가 없으므로 상세로 넘긴다.
            //
            // 조회가 실패하면 차단하지 않고 기존 조인 흐름으로 진행한다.
            // JoinGroupUseCase가 중복 가입을 막아주므로 최악의 경우에도 안전하다.
            val alreadyMember = getGroupsUseCase()
                .getOrNull()
                ?.any { it.id == groupId } == true

            if (alreadyMember) {
                // isLoading을 유지한 채 이동 → 조인 UI가 깜빡이지 않는다
                _sideEffect.emit(GroupJoinContract.SideEffect.NavigateToGroup(groupId))
                return@launch
            }

            loadGroupInfo(groupId)
        }
    }

    private suspend fun loadGroupInfo(groupId: String) {
        getGroupDetailUseCase(groupId).fold(
            onSuccess = { group ->
                _uiState.update { it.copy(isLoading = false, group = group) }
            },
            onFailure = {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "그룹을 찾을 수 없어요")
                }
            }
        )
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
                    // joined를 토스트에도 반영한다.
                    // 기존 코드는 이미 멤버여도 "참여했어요!"를 띄워 거짓 피드백을 줬다.
                    _sideEffect.emit(
                        GroupJoinContract.SideEffect.ShowToast(
                            if (joined) "그룹에 참여했어요!" else "이미 참여한 그룹이에요"
                        )
                    )
                    _sideEffect.emit(GroupJoinContract.SideEffect.NavigateToGroup(groupId))
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            errorMessage = e.message ?: "그룹 참여에 실패했어요"
                        )
                    }
                }
            )
        }
    }
}