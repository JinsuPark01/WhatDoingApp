package com.jinsupark.helpumta.di

import com.jinsupark.helpumta.data.repository.AuthRepositoryImpl
import com.jinsupark.helpumta.data.repository.GroupRepositoryImpl
import com.jinsupark.helpumta.data.repository.RecordRepositoryImpl
import com.jinsupark.helpumta.data.repository.UserRepositoryImpl
import com.jinsupark.helpumta.domain.repository.AuthRepository
import com.jinsupark.helpumta.domain.repository.GroupRepository
import com.jinsupark.helpumta.domain.repository.RecordRepository
import com.jinsupark.helpumta.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(
        groupRepositoryImpl: GroupRepositoryImpl
    ): GroupRepository

    @Binds
    @Singleton
    abstract fun bindRecordRepository(
        recordRepositoryImpl: RecordRepositoryImpl
    ): RecordRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
}