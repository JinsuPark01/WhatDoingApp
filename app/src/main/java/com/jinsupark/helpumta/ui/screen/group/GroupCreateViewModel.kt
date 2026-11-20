package com.jinsupark.helpumta.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.model.GroupPolicy
import com.jinsupark.helpumta.domain.usecase.CreateGroupUseCase
import com.jinsupark.helpumta.domain.usecase.GetGroupDetailUseCase
import com.jinsupark.helpumta.domain.usecase.UpdateGroupUseCase
import com.jinsupark.helpumta.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupCreateViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
    private val updateGroupUseCase: UpdateGroupUseCase,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupCreateContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<GroupCreateContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    // 생성 모드는 groupId가 null이라 상태값으로 중복 호출을 구분할 수 없다
    private var initialized = false

    fun handleIntent(intent: GroupCreateContract.Intent) {
        when (intent) {
            is GroupCreateContract.Intent.Initialize -> {
                if (initialized) return
                initialized = true
                _uiState.update { it.copy(groupId = intent.groupId) }
                intent.groupId?.let { loadGroup(it) }
            }
            is GroupCreateContract.Intent.UpdateName -> {
                val current = _uiState.value.name
                if (acceptLength(intent.name, current, GroupPolicy.MAX_NAME_LENGTH)) {
                    _uiState.update { it.copy(name = intent.name) }
                }
            }
            is GroupCreateContract.Intent.UpdateDescription -> {
                val current = _uiState.value.description
                if (acceptLength(intent.description, current, GroupPolicy.MAX_DESCRIPTION_LENGTH)) {
                    _uiState.update { it.copy(description = intent.description) }
                }
            }
            is GroupCreateContract.Intent.UpdateImage -> {
                _uiState.update { it.copy(imageUri = intent.uri) }
            }
            GroupCreateContract.Intent.Submit -> submit()
        }
    }

    /**
     * 제한 이내면 허용, 넘으면 무시 (잘라내지 않는다 — 한글 조합 중 강제로 줄이면 조합이 깨짐).
     * 단, 제한이 생기기 전에 만든 그룹은 이미 길 수 있으므로 줄어드는 변경은 항상 허용.
     */
    private fun acceptLength(new: String, current: String, max: Int): Boolean =
        new.length <= max || new.length < current.length

    private fun loadGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitializing = true) }
            getGroupDetailUseCase(groupId).fold(
                onSuccess = { group ->
                    _uiState.update { it.copy(
                        isInitializing = false,
                        name = group.name,
                        description = group.description,
                        imageUri = group.imageUrl.ifBlank { null }  // 기존 URL을 imageUri로
                    )}
                },
                onFailure = {
                    _uiState.update { it.copy(isInitializing = false) }
                    _sideEffect.emit(GroupCreateContract.SideEffect.ShowToast("그룹 정보를 불러오지 못했어요"))
                    _sideEffect.emit(GroupCreateContract.SideEffect.NavigateBack)
                }
            )
        }
    }

    private fun submit() {
        if (_uiState.value.isLoading) return
        if (_uiState.value.isEditMode) updateGroup() else createGroup()
    }

    private fun createGroup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val state = _uiState.value
            val result = createGroupUseCase(
                name = state.name,
                description = state.description,
                imageUri = state.imageUri
            )

            result.fold(
                onSuccess = { groupId ->
                    _uiState.update { it.copy(isLoading = false) }
                    // 그룹 목록이 바뀌었으므로 위젯 갱신
                    widgetUpdater.refresh()
                    _sideEffect.emit(GroupCreateContract.SideEffect.NavigateToGroup(groupId))
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                    _sideEffect.emit(GroupCreateContract.SideEffect.ShowToast("그룹 생성에 실패했어요"))
                }
            )
        }
    }

    private fun updateGroup() {
        val groupId = _uiState.value.groupId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val state = _uiState.value
            val result = updateGroupUseCase(
                groupId = groupId,
                name = state.name,
                description = state.description,
                imageUri = state.imageUri
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    // 위젯에 그룹 이름이 표시되므로 갱신
                    widgetUpdater.refresh()
                    _sideEffect.emit(GroupCreateContract.SideEffect.ShowToast("그룹 정보를 수정했어요"))
                    _sideEffect.emit(GroupCreateContract.SideEffect.GroupUpdated)
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                    _sideEffect.emit(GroupCreateContract.SideEffect.ShowToast("그룹 정보 수정에 실패했어요"))
                }
            )
        }
    }
}