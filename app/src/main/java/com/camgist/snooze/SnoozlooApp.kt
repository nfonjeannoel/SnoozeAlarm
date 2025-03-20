package com.camgist.snooze

import android.app.Application
import android.util.Log
import com.camgist.snooze.di.appModule
import com.camgist.snooze.di.databaseModule
import com.camgist.snooze.di.repositoryModule
import com.camgist.snooze.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SnoozlooApp : Application() {
    companion object {
        private const val TAG = "SnoozlooApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Koin dependency injection
            startKoin {
                androidLogger(Level.ERROR) // Use ERROR level to avoid crashes on Koin startup
                androidContext(this@SnoozlooApp)
                modules(listOf(
                    appModule,
                    databaseModule,
                    repositoryModule,
                    viewModelModule
                ))
            }
            Log.d(TAG, "Koin initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Koin: ${e.message}", e)
        }
    }
} 