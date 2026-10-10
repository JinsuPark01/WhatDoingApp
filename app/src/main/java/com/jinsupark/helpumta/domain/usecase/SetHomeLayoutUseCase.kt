package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.HomeLayout
import com.jinsupark.helpumta.domain.repository.SettingsRepository
import javax.inject.Inject

class SetHomeLayoutUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(layout: HomeLayout) =
        settingsRepository.setHomeLayout(layout)
}