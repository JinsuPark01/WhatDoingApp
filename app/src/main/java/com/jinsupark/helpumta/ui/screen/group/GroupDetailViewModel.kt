package com.jinsupark.helpumta.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinsupark.helpumta.domain.usecase.DeleteRecordUseCase
import com.jinsupark.helpumta.domain.usecase.GetCurrentUserIdUseCase
import com.jinsupark.helpumta.domain.usecase.GetGroupDetailUseCase
import com.jinsupark.helpumta.domain.usecase.GetGroupStatsUseCase
import com.jinsupark.helpumta.domain.usecase.GetMyRecordDatesUseCase
import com.jinsupark.helpumta.domain.usecase.GetRecordsByGroupUseCase
import com.jinsupark.helpumta.domain.usecase.HasWroteTodayUseCase
import com.jinsupark.helpumta.domain.usecase.LeaveGroupUseCase
import com.jinsupark.helpumta.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val getRecordsByGroupUseCase: GetRecordsByGroupUseCase,
    private val hasWroteTodayUseCase: HasWroteTodayUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val getGroupStatsUseCase: GetGroupStatsUseCase,
    private val getMyRecordDatesUseCase: GetMyRecordDatesUseCase,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailContract.UiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<GroupDetailContract.SideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun handleIntent(intent: GroupDetailContract.Intent) {
        when (intent) {
            is GroupDetailContract.Intent.LoadGroupDetail -> loadGroupDetail(intent.groupId)
            GroupDetailContract.Intent.NavigateToRecord -> navigateToRecord()
            GroupDetailContract.Intent.LeaveGroup -> leaveGroup()
            is GroupDetailContract.Intent.SelectDate -> changeDate(intent.startMillis)
            is GroupDetailContract.Intent.MoveDay -> {
                val start = addDays(_uiState.value.selectedDate, intent.offset)
                changeDate(start)
            }
            GroupDetailContract.Intent.RefreshToToday -> refreshToToday()
            GroupDetailContract.Intent.RefreshGroup -> refreshGroup()
            is GroupDetailContract.Intent.DeleteRecord -> deleteRecord(intent.recordId)
            GroupDetailContract.Intent.AlreadyWroteToday -> {
                viewModelScope.launch {
                    _sideEffect.emit(
                        GroupDetailContract.SideEffect.ShowToast("오늘 기록은 이미 작성했어요")
                    )
                }
            }
            is GroupDetailContract.Intent.SelectTab -> selectTab(intent.tab)
            GroupDetailContract.Intent.OpenCalendar -> openCalendar()
            is GroupDetailContract.Intent.MoveMonth -> moveMonth(intent.offset)
        }
    }

    private fun loadGroupDetail(groupId: String) {
        val currentState = _uiState.value
        val isSameGroup = currentState.groupId == groupId && currentState.group != null

        // 재진입(같은 그룹)이면 상태 유지 — 새로고침은 RefreshToToday로만
        if (isSameGroup) return

        // 여기 아래는 항상 첫 진입 / 새 그룹
        val today = startOfDay(System.currentTimeMillis())

        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                groupId = groupId,
                currentUserId = getCurrentUserIdUseCase(),   // UseCase 경유
                selectedDate = today,
                errorMessage = null,
                recordsErrorMessage = null,
                calendarMarks = emptyMap()
            )}

            supervisorScope {
                val groupDeferred = async { getGroupDetailUseCase(groupId) }
                val recordsDeferred = async {
                    getRecordsByGroupUseCase(groupId, today, endOfDay(today))
                }
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
                            recordsErrorMessage = if (recordsResult.isFailure) "기록을 불러오지 못했습니다" else null,
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

    // 그룹 정보만 다시 조회 — 기록/통계/캘린더 상태는 건드리지 않는다
    private fun refreshGroup() {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return

        viewModelScope.launch {
            // 실패해도 기존 정보가 그대로 보이므로 에러를 노출하지 않는다
            getGroupDetailUseCase(groupId).onSuccess { group ->
                _uiState.update { it.copy(group = group) }
            }
        }
    }

    // 날짜 변경 공통 (캘린더/화살표 둘 다 여기로)
    private fun changeDate(startMillis: Long) {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return

        // 미래 차단: 오늘 이후로는 못 감
        val today = startOfDay(System.currentTimeMillis())
        if (startMillis > today) return

        _uiState.update { it.copy(selectedDate = startMillis, recordsErrorMessage = null) }

        viewModelScope.launch {
            getRecordsByGroupUseCase(groupId, startMillis, endOfDay(startMillis)).fold(
                onSuccess = { records ->
                    _uiState.update { it.copy(records = records) }
                },
                onFailure = {
                    _uiState.update { it.copy(recordsErrorMessage = "기록을 불러오지 못했습니다") }
                }
            )
        }
    }

    private fun navigateToRecord() {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return

        viewModelScope.launch {
            _sideEffect.emit(GroupDetailContract.SideEffect.NavigateToRecord(groupId))
        }
    }

    private fun leaveGroup() {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return
        if (_uiState.value.isLeaving) return

        _uiState.update { it.copy(isLeaving = true) }

        viewModelScope.launch {
            leaveGroupUseCase(groupId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLeaving = false) }
                    widgetUpdater.refresh()
                    _sideEffect.emit(GroupDetailContract.SideEffect.NavigateToHome)
                },
                onFailure = {
                    _uiState.update { it.copy(isLeaving = false) }
                    _sideEffect.emit(
                        GroupDetailContract.SideEffect.ShowToast("그룹 나가기에 실패했어요. 다시 시도해주세요.")
                    )
                }
            )
        }
    }

    private fun refreshToToday() {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return

        val today = startOfDay(System.currentTimeMillis())
        // 기록이 새로 작성됐을 수 있으니 통계·캘린더 캐시도 무효화
        _uiState.update { it.copy(
            selectedDate = today,
            recordsErrorMessage = null,
            stats = null,
            calendarMarks = emptyMap()
        )}

        viewModelScope.launch {
            supervisorScope {
                val recordsDeferred = async {
                    getRecordsByGroupUseCase(groupId, today, endOfDay(today))
                }
                val wroteDeferred = async { hasWroteTodayUseCase(groupId) }
                val recordsResult = recordsDeferred.await()
                val wroteResult = wroteDeferred.await()

                _uiState.update { it.copy(
                    records = recordsResult.getOrDefault(emptyList()),
                    recordsErrorMessage = if (recordsResult.isFailure) "기록을 불러오지 못했습니다" else null,
                    hasWroteToday = wroteResult.getOrDefault(false)
                )}
                widgetUpdater.refresh()
            }
        }
    }

    // --- 캘린더 ---

    private fun openCalendar() {
        val base = _uiState.value.selectedDate.takeIf { it > 0L } ?: System.currentTimeMillis()
        val month = startOfMonth(base)
        _uiState.update { it.copy(calendarMonth = month) }
        loadCalendarMarks(month)
    }

    private fun moveMonth(offset: Int) {
        val next = addMonths(_uiState.value.calendarMonth, offset)
        // 미래 달 차단
        if (next > startOfMonth(System.currentTimeMillis())) return

        _uiState.update { it.copy(calendarMonth = next) }
        loadCalendarMarks(next)
    }

    private fun loadCalendarMarks(month: Long) {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return

        // 이미 조회한 달이면 재조회하지 않는다 (통계 캐싱과 같은 방식)
        if (_uiState.value.calendarMarks.containsKey(month)) return

        _uiState.update { it.copy(isCalendarLoading = true) }

        viewModelScope.launch {
            getMyRecordDatesUseCase(groupId, month, startOfNextMonth(month)).fold(
                onSuccess = { createdAtList ->
                    val days = createdAtList.map { dayOfMonth(it) }.toSet()
                    _uiState.update { it.copy(
                        isCalendarLoading = false,
                        calendarMarks = it.calendarMarks + (month to days)
                    )}
                },
                onFailure = {
                    // 실패해도 날짜 선택 자체는 가능하므로 에러를 노출하지 않는다.
                    // 점만 안 찍힐 뿐이고, 다음에 열면 다시 시도된다.
                    _uiState.update { it.copy(isCalendarLoading = false) }
                }
            )
        }
    }

    // --- 날짜 헬퍼 ---

    private fun startOfDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun endOfDay(startMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = startMillis
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

    private fun addDays(startMillis: Long, offset: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = startMillis
            add(Calendar.DAY_OF_MONTH, offset)
        }.timeInMillis

    private fun startOfMonth(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun startOfNextMonth(monthStart: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = monthStart
            add(Calendar.MONTH, 1)
        }.timeInMillis

    private fun addMonths(monthStart: Long, offset: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = monthStart
            add(Calendar.MONTH, offset)
        }.timeInMillis

    private fun dayOfMonth(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }
            .get(Calendar.DAY_OF_MONTH)

    private fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            deleteRecordUseCase(recordId).fold(
                onSuccess = {
                    val target = _uiState.value.records.find { it.id == recordId }
                    val wasToday = target != null &&
                            isSameDay(target.createdAt, System.currentTimeMillis())

                    _uiState.update { state ->
                        state.copy(
                            records = state.records.filter { it.id != recordId },
                            // 오늘 기록을 지웠으면 다시 작성 가능
                            hasWroteToday = if (wasToday) false else state.hasWroteToday,
                            // 기록이 바뀌었으니 통계·캘린더 캐시 무효화
                            stats = null,
                            calendarMarks = emptyMap()
                        )
                    }
                    widgetUpdater.refresh()
                    _sideEffect.emit(GroupDetailContract.SideEffect.ShowToast("기록을 삭제했어요"))
                },
                onFailure = {
                    _sideEffect.emit(
                        GroupDetailContract.SideEffect.ShowToast("기록 삭제에 실패했어요. 다시 시도해주세요.")
                    )
                }
            )
        }
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = a }
        val calB = Calendar.getInstance().apply { timeInMillis = b }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
                calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    private fun selectTab(tab: GroupDetailContract.Tab) {
        _uiState.update { it.copy(selectedTab = tab) }

        // 통계 탭 첫 진입에만 로드 (이후엔 캐싱된 값 사용)
        if (tab == GroupDetailContract.Tab.STATS &&
            _uiState.value.stats == null &&
            !_uiState.value.isStatsLoading
        ) {
            loadStats()
        }
    }

    private fun loadStats() {
        val groupId = _uiState.value.groupId
        if (groupId.isBlank()) return

        _uiState.update { it.copy(isStatsLoading = true, statsErrorMessage = null) }

        viewModelScope.launch {
            getGroupStatsUseCase(groupId).fold(
                onSuccess = { stats ->
                    _uiState.update { it.copy(isStatsLoading = false, stats = stats) }
                },
                onFailure = {
                    _uiState.update { it.copy(
                        isStatsLoading = false,
                        statsErrorMessage = "통계를 불러오지 못했습니다"
                    )}
                }
            )
        }
    }
}