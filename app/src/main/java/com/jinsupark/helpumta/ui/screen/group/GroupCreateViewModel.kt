package com.jinsupark.helpumta.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.usecase.CreateGroupUseCase
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
    private val createGroupUseCase: CreateGroupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupCreateContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<GroupCreateContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: GroupCreateContract.Intent) {
        when (intent) {
            is GroupCreateContract.Intent.UpdateName -> {
                _uiState.update { it.copy(
                    name = intent.name,
                    errorMessage = null
                )}
            }
            is GroupCreateContract.Intent.UpdateDescription -> {
                _uiState.update { it.copy(description = intent.description) }
            }
            is GroupCreateContract.Intent.UpdateImage -> {
                _uiState.update { it.copy(imageUri = intent.uri) }
            }
            GroupCreateContract.Intent.SubmitCreate -> createGroup()
        }
    }

    private fun createGroup() {
        if (_uiState.value.isLoading) return

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
                    _sideEffect.emit(GroupCreateContract.SideEffect.NavigateToGroup(groupId))
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "그룹 생성에 실패했습니다"
                    )}
                }
            )
        }
    }
}