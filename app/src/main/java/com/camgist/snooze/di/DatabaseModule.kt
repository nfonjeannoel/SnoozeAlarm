package com.camgist.snooze.di

import android.content.Context
import androidx.room.Room
import com.camgist.snooze.data.db.AlarmDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    // Provide Room database instance
    single {
        Room.databaseBuilder(
            androidContext(),
            AlarmDatabase::class.java,
            "alarm_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    // Provide DAOs
    single { get<AlarmDatabase>().alarmDao() }
} 