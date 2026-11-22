package com.jinsupark.helpumta.domain.repository

import com.jinsupark.helpumta.domain.model.ExtractStyle
import kotlinx.coroutines.flow.Flow

interface ExtractStyleRepository {
    fun getStyle(): Flow<ExtractStyle>
    suspend fun saveStyle(style: ExtractStyle)
}