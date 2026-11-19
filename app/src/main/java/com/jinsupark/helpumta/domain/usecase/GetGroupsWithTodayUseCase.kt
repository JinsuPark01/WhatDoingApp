package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.GroupToday
import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.util.addDays
import com.jinsupark.helpumta.domain.util.startOfToday
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * 내 그룹 목록 + 각 그룹의 오늘 현황.
 *
 * 그룹 수만큼 쿼리가 나가지만 한 사용자의 그룹은 보통 1~3개다.
 * userId + createdAt 범위로 한 번에 조회하면 새 복합 인덱스가 필요해지므로
 * 기존 인덱스를 재사용하는 그룹별 조회를 병렬로 돌린다. (WidgetData와 같은 판단)
 */
class GetGroupsWithTodayUseCase @Inject constructor(
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getRecordsByGroupUseCase: GetRecordsByGroupUseCase,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<List<GroupToday>> {
        val uid = authRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("로그인이 필요합니다"))

        val groups = getGroupsUseCase().getOrElse { return Result.failure(it) }
        if (groups.isEmpty()) return Result.success(emptyList())

        val today = startOfToday()
        val tomorrow = addDays(today, 1)

        // runCatching을 쓰지 않는다.
        // runCatching은 CancellationException까지 잡아서 Result.failure로 바꾼다.
        // 코루틴이 취소된 것뿐인데 "실패"로 보고되면 화면에 엉뚱한 에러가 뜨고,
        // 더 나쁘게는 취소 신호가 상위로 전파되지 않아 코루틴이 좀비로 남는다.
        return try {
            val result = coroutineScope {
                groups.map { group ->
                    async {
                        val writers = getRecordsByGroupUseCase(group.id, today, tomorrow)
                            .getOrDefault(emptyList())
                            .map { it.userId }
                            .toSet()

                        GroupToday(
                            group = group,
                            wroteCount = writers.size,
                            didIWrite = uid in writers
                        )
                    }
                }.awaitAll()
            }
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}