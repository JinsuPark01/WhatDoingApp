package com.jinsupark.helpumta.domain.usecase

import com.jinsupark.helpumta.domain.model.ExtractStyle
import com.jinsupark.helpumta.domain.repository.ExtractStyleRepository
import javax.inject.Inject

class SaveExtractStyleUseCase @Inject constructor(
    private val repository: ExtractStyleRepository
) {
    suspend operator fun invoke(style: ExtractStyle) = repository.saveStyle(style)
}