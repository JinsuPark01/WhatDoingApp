package com.example.whatdoing.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatdoing.domain.usecase.GetGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getGroupsUseCase: GetGroupsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<HomeContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    init {
        handleIntent(HomeContract.Intent.LoadGroups)
    }

    fun handleIntent(intent: HomeContract.Intent) {
        when (intent) {
            HomeContract.Intent.LoadGroups -> loadGroups()
            is HomeContract.Intent.NavigateToGroup -> navigateToGroup(intent.groupId)
            HomeContract.Intent.NavigateToCreateGroup -> navigateToCreateGroup()
        }
    }

    private fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = getGroupsUseCase()

            result.fold(
                onSuccess = { groups ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        groups = groups
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

    private fun navigateToGroup(groupId: String) {
        viewModelScope.launch {
            _sideEffect.emit(HomeContract.SideEffect.NavigateToGroup(groupId))
        }
    }

    private fun navigateToCreateGroup() {
        viewModelScope.launch {
            _sideEffect.emit(HomeContract.SideEffect.NavigateToCreateGroup)
        }
    }
}