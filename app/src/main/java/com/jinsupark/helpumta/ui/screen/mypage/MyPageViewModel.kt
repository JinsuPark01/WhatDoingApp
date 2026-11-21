package com.jinsupark.helpumta.ui.screen.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.model.HomeLayout
import com.jinsupark.helpumta.domain.usecase.DeleteAccountUseCase
import com.jinsupark.helpumta.domain.usecase.GetAuthProviderUseCase
import com.jinsupark.helpumta.domain.usecase.GetHomeLayoutUseCase
import com.jinsupark.helpumta.domain.usecase.GetMyPhotoUrlUseCase
import com.jinsupark.helpumta.domain.usecase.GetUserProfileUseCase
import com.jinsupark.helpumta.domain.usecase.LogoutUseCase
import com.jinsupark.helpumta.domain.usecase.ReauthenticateUseCase
import com.jinsupark.helpumta.domain.usecase.SetHomeLayoutUseCase
import com.jinsupark.helpumta.domain.usecase.UpdateNicknameUseCase
import com.jinsupark.helpumta.domain.usecase.UpdateProfileImageUseCase
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
class MyPageViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getMyPhotoUrlUseCase: GetMyPhotoUrlUseCase,
    private val updateNicknameUseCase: UpdateNicknameUseCase,
    private val updateProfileImageUseCase: UpdateProfileImageUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getAuthProviderUseCase: GetAuthProviderUseCase,
    private val reauthenticateUseCase: ReauthenticateUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val getHomeLayoutUseCase: GetHomeLayoutUseCase,
    private val setHomeLayoutUseCase: SetHomeLayoutUseCase,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<MyPageContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    init {
        handleIntent(MyPageContract.Intent.LoadUserInfo)
        observeHomeLayout()
    }

    fun handleIntent(intent: MyPageContract.Intent) {
        when (intent) {
            MyPageContract.Intent.LoadUserInfo -> loadUserInfo()
            MyPageContract.Intent.Logout -> logout()
            is MyPageContract.Intent.DeleteWithPassword ->
                reauthThenDelete { reauthenticateUseCase.withPassword(intent.password) }
            is MyPageContract.Intent.DeleteWithGoogle ->
                reauthThenDelete { reauthenticateUseCase.withGoogle(intent.idToken) }
            MyPageContract.Intent.GoogleReauthCancelled -> {
                _uiState.update { it.copy(isDeleting = false) }
                viewModelScope.launch {
                    _sideEffect.emit(
                        MyPageContract.SideEffect.ShowToast("구글 인증에 실패했어요. 다시 시도해주세요.")
                    )
                }
            }
            is MyPageContract.Intent.ChangeHomeLayout ->
                changeHomeLayout(intent.layout)
            is MyPageContract.Intent.UpdatePhoto -> updatePhoto(intent.uri)
            is MyPageContract.Intent.UpdateNickname -> updateNickname(intent.nickname)
        }
    }

    // Screen에서 Credential Manager 팝업을 띄우기 직전에 호출
    fun onGoogleReauthStarted() {
        _uiState.update { it.copy(isDeleting = true) }
    }

    private fun observeHomeLayout() {
        viewModelScope.launch {
            getHomeLayoutUseCase().collect { layout ->
                _uiState.update { it.copy(homeLayout = layout) }
            }
        }
    }

    private fun changeHomeLayout(layout: HomeLayout) {
        viewModelScope.launch {
            setHomeLayoutUseCase(layout)
        }
    }

    private fun loadUserInfo() {
        val userProfile = getUserProfileUseCase()
        if (userProfile != null) {
            _uiState.update {
                it.copy(
                    nickname = userProfile.nickname,
                    email = userProfile.email,
                    authProvider = getAuthProviderUseCase()
                )
            }
        }

        // 사진은 Firestore에 있어 비동기. 실패해도 이니셜이 보이므로 에러를 노출하지 않는다
        viewModelScope.launch {
            getMyPhotoUrlUseCase().onSuccess { url ->
                _uiState.update { it.copy(photoUrl = url) }
            }
        }
    }

    private fun updatePhoto(uri: String?) {
        if (_uiState.value.isUpdatingPhoto) return
        _uiState.update { it.copy(isUpdatingPhoto = true) }

        viewModelScope.launch {
            updateProfileImageUseCase(uri).fold(
                onSuccess = { newUrl ->
                    _uiState.update { it.copy(isUpdatingPhoto = false, photoUrl = newUrl) }
                },
                onFailure = {
                    _uiState.update { it.copy(isUpdatingPhoto = false) }
                    _sideEffect.emit(
                        MyPageContract.SideEffect.ShowToast("프로필 사진 변경에 실패했어요")
                    )
                }
            )
        }
    }

    private fun updateNickname(nickname: String) {
        if (_uiState.value.isUpdatingNickname) return
        _uiState.update { it.copy(isUpdatingNickname = true) }

        viewModelScope.launch {
            updateNicknameUseCase(nickname).fold(
                onSuccess = {
                    // Auth displayName이 원본 → 바뀐 값을 원본에서 다시 읽는다
                    val updated = getUserProfileUseCase()?.nickname ?: nickname.trim()
                    _uiState.update { it.copy(isUpdatingNickname = false, nickname = updated) }
                    // 위젯에 이름이 표시되므로 갱신
                    widgetUpdater.refresh()
                    _sideEffect.emit(MyPageContract.SideEffect.ShowToast("닉네임을 변경했어요"))
                },
                onFailure = {
                    _uiState.update { it.copy(isUpdatingNickname = false) }
                    _sideEffect.emit(
                        MyPageContract.SideEffect.ShowToast("닉네임 변경에 실패했어요. 다시 시도해주세요.")
                    )
                }
            )
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            // 로그아웃 '후'에 갱신해야 위젯이 "로그인이 필요해요"로 바뀐다.
            // 안 하면 홈 화면에 이전 사용자의 그룹명과 기록 현황이 그대로 남는다.
            widgetUpdater.refresh()
            _sideEffect.emit(MyPageContract.SideEffect.NavigateToLogin)
        }
    }

    private fun reauthThenDelete(reauth: suspend () -> Result<Unit>) {
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            runReauthAndDelete(reauth)
        }
    }

    // 재인증 → 삭제 공통 로직
    private suspend fun runReauthAndDelete(reauth: suspend () -> Result<Unit>) {
        reauth().fold(
            onSuccess = {
                deleteAccountUseCase().fold(
                    onSuccess = {
                        _uiState.update { it.copy(isDeleting = false) }
                        widgetUpdater.refresh()
                        _sideEffect.emit(MyPageContract.SideEffect.NavigateToLogin)
                    },
                    onFailure = {
                        _uiState.update { it.copy(isDeleting = false) }
                        _sideEffect.emit(
                            MyPageContract.SideEffect.ShowToast("계정 삭제에 실패했어요. 다시 시도해주세요.")
                        )
                    }
                )
            },
            onFailure = {
                _uiState.update { it.copy(isDeleting = false) }
                _sideEffect.emit(
                    MyPageContract.SideEffect.ShowToast("본인 확인에 실패했어요. 비밀번호를 확인해주세요.")
                )
            }
        )
    }
}