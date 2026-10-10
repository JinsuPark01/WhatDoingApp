package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.HomeLayout
import com.jinsupark.helpumta.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHomeLayoutUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<HomeLayout> = settingsRepository.homeLayout
}