package com.example.whatdoing.di

import com.example.whatdoing.data.repository.AuthRepositoryImpl
import com.example.whatdoing.data.repository.GroupRepositoryImpl
import com.example.whatdoing.domain.repository.AuthRepository
import com.example.whatdoing.domain.repository.GroupRepository
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
}