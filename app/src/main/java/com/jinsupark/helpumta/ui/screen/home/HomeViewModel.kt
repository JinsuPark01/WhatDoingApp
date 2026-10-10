package com.jinsupark.helpumta.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.usecase.GetGroupsUseCase
import com.jinsupark.helpumta.domain.usecase.GetHomeLayoutUseCase
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
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getHomeLayoutUseCase: GetHomeLayoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<HomeContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    init {
        observeHomeLayout()
        handleIntent(HomeContract.Intent.LoadGroups)
    }

    fun handleIntent(intent: HomeContract.Intent) {
        when (intent) {
            HomeContract.Intent.LoadGroups -> loadGroups()
            is HomeContract.Intent.NavigateToGroup -> navigateToGroup(intent.groupId)
            HomeContract.Intent.NavigateToCreateGroup -> navigateToCreateGroup()
        }
    }

    // DataStore가 Flow를 주므로 설정이 바뀌면 홈이 자동 반영됨
    private fun observeHomeLayout() {
        viewModelScope.launch {
            getHomeLayoutUseCase().collect { layout ->
                _uiState.update { it.copy(homeLayout = layout) }
            }
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