package com.jinsupark.helpumta.data.repository

import com.jinsupark.helpumta.data.local.SettingsDataStore
import com.jinsupark.helpumta.domain.model.HomeLayout
import com.jinsupark.helpumta.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val homeLayout: Flow<HomeLayout> = settingsDataStore.homeLayout

    override suspend fun setHomeLayout(layout: HomeLayout) {
        settingsDataStore.setHomeLayout(layout)
    }
}