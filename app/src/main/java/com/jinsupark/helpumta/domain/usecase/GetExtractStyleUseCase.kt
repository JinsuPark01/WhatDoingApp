package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.ExtractStyle
import com.jinsupark.helpumta.domain.repository.ExtractStyleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExtractStyleUseCase @Inject constructor(
    private val repository: ExtractStyleRepository
) {
    operator fun invoke(): Flow<ExtractStyle> = repository.getStyle()
}