package com.example.whatdoing.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatdoing.domain.usecase.GetGroupDetailUseCase
import com.example.whatdoing.domain.usecase.GetRecordsByGroupUseCase
import com.example.whatdoing.domain.usecase.HasWroteTodayUseCase
import com.example.whatdoing.domain.usecase.LeaveGroupUseCase
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
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val getRecordsByGroupUseCase: GetRecordsByGroupUseCase,
    private val hasWroteTodayUseCase: HasWroteTodayUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase
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
            is GroupDetailContract.Intent.SelectDate -> {
                val start = utcMillisToLocalStartOfDay(intent.utcMillis)
                changeDate(start)
            }
            is GroupDetailContract.Intent.MoveDay -> {
                val start = addDays(_uiState.value.selectedDate, intent.offset)
                changeDate(start)
            }
        }
    }

    private fun loadGroupDetail(groupId: String) {
        val currentState = _uiState.value
        val isSameGroup = currentState.groupId == groupId && currentState.group != null
        val today = startOfDay(System.currentTimeMillis())

        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = !isSameGroup,
                groupId = groupId,
                selectedDate = today,   // 진입 시 항상 오늘
                errorMessage = null,
                recordsErrorMessage = null
            )}

            supervisorScope {
                val groupDeferred = if (isSameGroup) {
                    null
                } else {
                    async { getGroupDetailUseCase(groupId) }
                }
                val recordsDeferred = async {
                    getRecordsByGroupUseCase(groupId, today, endOfDay(today))
                }
                val wroteDeferred = async { hasWroteTodayUseCase(groupId) }

                val groupResult = groupDeferred?.await()
                val recordsResult = recordsDeferred.await()
                val wroteResult = wroteDeferred.await()

                if (isSameGroup) {
                    _uiState.update { it.copy(
                        isLoading = false,
                        records = recordsResult.getOrDefault(emptyList()),
                        recordsErrorMessage = if (recordsResult.isFailure) "기록을 불러오지 못했습니다" else null,
                        hasWroteToday = wroteResult.getOrDefault(false)
                    )}
                } else {
                    groupResult?.fold(
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
    }

    // 날짜 변경 공통 (달력/화살표 둘 다 여기로)
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

    // DatePicker가 준 UTC millis → 로컬 그 날짜의 00:00 (시간대 어긋남 방지)
    private fun utcMillisToLocalStartOfDay(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return Calendar.getInstance().apply {
            clear()
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
        }.timeInMillis
    }
}