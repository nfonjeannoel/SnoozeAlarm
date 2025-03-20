package com.camgist.snooze.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.camgist.snooze.domain.usecase.GetAlarmByIdUseCase
import com.camgist.snooze.domain.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase by inject()
    private val alarmScheduler: AlarmScheduler by inject()
    
    companion object {
        private const val TAG = "AlarmReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AndroidAlarmScheduler.ALARM_ID, -1)
        
        Log.d(TAG, "Alarm triggered: $alarmId")
        
        if (alarmId != -1L) {
            // BroadcastReceivers have a limited lifetime, so we need to use goAsync()
            val pendingResult = goAsync()
            
            // Use a SupervisorJob so errors don't cancel the entire coroutine
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            
            scope.launch {
                try {
                    val alarm = getAlarmByIdUseCase(alarmId)
                    
                    if (alarm != null) {
                        Log.d(TAG, "Processing alarm: $alarm")
                        
                        // If this is a repeating alarm, reschedule for next occurrence
                        if (alarm.repeatDays.isNotEmpty()) {
                            alarmScheduler.scheduleAlarm(alarm)
                            Log.d(TAG, "Rescheduled repeating alarm for next occurrence")
                        }
                        
                        // Start the alarm service
                        val serviceIntent = Intent(context, AlarmService::class.java).apply {
                            putExtra(AndroidAlarmScheduler.ALARM_ID, alarmId)
                        }
                        
                        try {
                            context.startForegroundService(serviceIntent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error starting foreground service: ${e.message}", e)
                            // Fallback to regular service start if foreground fails
                            try {
                                context.startService(serviceIntent)
                            } catch (e2: Exception) {
                                Log.e(TAG, "Error starting service as fallback: ${e2.message}", e2)
                            }
                        }
                    } else {
                        Log.e(TAG, "Alarm with ID $alarmId not found")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing alarm: ${e.message}", e)
                } finally {
                    // Always finish the pending result
                    pendingResult.finish()
                }
            }
        } else {
            Log.e(TAG, "Received intent with invalid alarm ID")
        }
    }
} 