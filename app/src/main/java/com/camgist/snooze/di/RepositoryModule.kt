package com.camgist.snooze.di

import com.camgist.snooze.data.repository.AlarmRepository
import com.camgist.snooze.data.repository.AlarmRepositoryImpl
import org.koin.dsl.module

val repositoryModule = module {
    // Provide repository implementations
    single<AlarmRepository> { AlarmRepositoryImpl(get()) }
} 