package com.example.whatdoing.ui.screen.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatdoing.domain.usecase.GetUserProfileUseCase
import com.example.whatdoing.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<MyPageContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    init {
        handleIntent(MyPageContract.Intent.LoadUserInfo)
    }

    fun handleIntent(intent: MyPageContract.Intent) {
        when (intent) {
            MyPageContract.Intent.LoadUserInfo -> loadUserInfo()
            MyPageContract.Intent.Logout -> logout()
        }
    }

    private fun loadUserInfo() {
        val userProfile = getUserProfileUseCase()
        if (userProfile != null) {
            _uiState.update { it.copy(
                nickname = userProfile.nickname,
                email = userProfile.email
            )}
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _sideEffect.emit(MyPageContract.SideEffect.NavigateToLogin)
        }
    }
}