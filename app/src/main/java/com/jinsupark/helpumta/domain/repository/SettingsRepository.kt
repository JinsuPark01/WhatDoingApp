package com.jinsupark.helpumta.domain.repository

import com.jinsupark.helpumta.domain.model.HomeLayout
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val homeLayout: Flow<HomeLayout>
    suspend fun setHomeLayout(layout: HomeLayout)
}