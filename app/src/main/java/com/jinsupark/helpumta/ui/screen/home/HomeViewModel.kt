package com.jinsupark.helpumta.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.usecase.GetGroupsWithTodayUseCase
import com.jinsupark.helpumta.domain.usecase.GetHomeLayoutUseCase
import com.jinsupark.helpumta.domain.usecase.RegisterFcmTokenUseCase
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
class HomeViewModel @Inject constructor(
    private val getGroupsWithTodayUseCase: GetGroupsWithTodayUseCase,
    private val getHomeLayoutUseCase: GetHomeLayoutUseCase,
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<HomeContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    // 알림 권한 요청은 세션당 1회만.
    // 안드로이드는 2번 거부당하면 다이얼로그를 영구 차단하므로,
    // 남은 1번은 마이페이지 토글(사용자가 알림을 명확히 원하는 순간)에 남겨둔다.
    private var notificationPermissionAsked = false

    init {
        observeHomeLayout()
        handleIntent(HomeContract.Intent.LoadGroups)
        registerFcmToken()
        refreshWidgets()
    }

    fun handleIntent(intent: HomeContract.Intent) {
        when (intent) {
            HomeContract.Intent.LoadGroups -> loadGroups()
            is HomeContract.Intent.NavigateToGroup -> navigateToGroup(intent.groupId)
            HomeContract.Intent.NavigateToCreateGroup -> navigateToCreateGroup()
        }
    }

    // 홈은 로그인/회원가입/자동로그인 모든 경로가 거치는 지점 →
    // FCM 토큰 등록을 여기 한 곳에서 처리한다.
    // 사용자 동작이 아니므로 Intent로 만들지 않고 init에서 직접 호출.
    private fun registerFcmToken() {
        viewModelScope.launch {
            // 실패해도 사용자에게 알리지 않음 (알림 수신 여부는 사용자가 한 행동이 아님)
            registerFcmTokenUseCase()
        }
    }

    // 로그아웃 상태에서 로그인하면 위젯이 "로그인이 필요해요"인 채로 남는다.
    // init은 세션당 1회 수준이라 비용 부담 없이 그 경로를 덮을 수 있다.
    private fun refreshWidgets() {
        viewModelScope.launch {
            widgetUpdater.refresh()
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

            val result = getGroupsWithTodayUseCase()

            result.fold(
                onSuccess = { groups ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        groups = groups
                    )}
                    // 그룹이 생긴 뒤에야 리마인더가 의미를 가짐 → 이 시점에 권한 요청
                    maybeRequestNotificationPermission(hasGroup = groups.isNotEmpty())
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

    private suspend fun maybeRequestNotificationPermission(hasGroup: Boolean) {
        if (!hasGroup || notificationPermissionAsked) return
        notificationPermissionAsked = true
        _sideEffect.emit(HomeContract.SideEffect.RequestNotificationPermission)
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