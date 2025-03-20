package com.camgist.snooze.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.camgist.snooze.domain.usecase.RescheduleAlarmsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootCompletedReceiver : BroadcastReceiver(), KoinComponent {
    private val rescheduleAlarmsUseCase: RescheduleAlarmsUseCase by inject()
    
    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device boot completed, rescheduling alarms")
            
            // Use a SupervisorJob so errors don't cancel the entire coroutine
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            
            // BroadcastReceivers have a limited lifetime, so we need to use goAsync()
            val pendingResult = goAsync()
            
            scope.launch {
                try {
                    // Reschedule all enabled alarms after device reboot
                    val count = rescheduleAlarmsUseCase()
                    Log.i(TAG, "Successfully rescheduled $count alarms")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule alarms after boot: ${e.message}", e)
                } finally {
                    // Make sure we always call finish() on the pendingResult
                    pendingResult.finish()
                }
            }
        } else {
            Log.d(TAG, "Received action: ${intent.action}, ignoring")
        }
    }
} 