package com.example.whatdoing.di

import com.example.whatdoing.data.repository.AuthRepositoryImpl
import com.example.whatdoing.data.repository.GroupRepositoryImpl
import com.example.whatdoing.data.repository.RecordRepositoryImpl
import com.example.whatdoing.data.repository.UserRepositoryImpl
import com.example.whatdoing.domain.repository.AuthRepository
import com.example.whatdoing.domain.repository.GroupRepository
import com.example.whatdoing.domain.repository.RecordRepository
import com.example.whatdoing.domain.repository.UserRepository
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