package com.jinsupark.helpumta.widget

import com.jinsupark.helpumta.domain.usecase.GetCurrentUserIdUseCase
import com.jinsupark.helpumta.domain.usecase.GetGroupsUseCase
import com.jinsupark.helpumta.domain.usecase.GetMyRecordDatesUseCase
import com.jinsupark.helpumta.domain.usecase.GetRecordsByGroupUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * GlanceAppWidget은 ViewModel도 없고 Hilt가 직접 주입할 수 있는 컴포넌트도 아니다.
 * 그래서 진입점을 따로 만들어 UseCase를 꺼내 쓴다.
 *
 * 위젯 → UseCase → Repository 방향은 UI와 동일하므로
 * 위젯이 Firestore를 직접 건드리지 않는다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getCurrentUserIdUseCase(): GetCurrentUserIdUseCase
    fun getGroupsUseCase(): GetGroupsUseCase
    fun getRecordsByGroupUseCase(): GetRecordsByGroupUseCase
    fun getMyRecordDatesUseCase(): GetMyRecordDatesUseCase
}