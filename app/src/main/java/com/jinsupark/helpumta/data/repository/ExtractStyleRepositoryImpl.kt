package com.jinsupark.helpumta.data.repository

import com.jinsupark.helpumta.data.local.ExtractStyleDataStore
import com.jinsupark.helpumta.domain.model.ExtractStyle
import com.jinsupark.helpumta.domain.repository.ExtractStyleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExtractStyleRepositoryImpl @Inject constructor(
    private val extractStyleDataStore: ExtractStyleDataStore
) : ExtractStyleRepository {

    override fun getStyle(): Flow<ExtractStyle> = extractStyleDataStore.style

    override suspend fun saveStyle(style: ExtractStyle) {
        extractStyleDataStore.save(style)
    }
}