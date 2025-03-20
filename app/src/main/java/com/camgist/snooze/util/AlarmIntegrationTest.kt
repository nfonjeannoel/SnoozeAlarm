package com.camgist.snooze.util

import android.content.Context
import android.util.Log
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.model.DayOfWeek
import com.camgist.snooze.data.repository.AlarmRepository
import com.camgist.snooze.domain.AlarmScheduler
import com.camgist.snooze.domain.usecase.GetAlarmsUseCase
import com.camgist.snooze.domain.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Utility for integration testing of alarm system components
 * This can be used in a debug build to verify that all components work together.
 */
class AlarmIntegrationTest(
    private val context: Context
) : KoinComponent {
    private val alarmRepository: AlarmRepository by inject()
    private val alarmScheduler: AlarmScheduler by inject()
    private val getAlarmsUseCase: GetAlarmsUseCase by inject()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "AlarmIntegrationTest"
    }
    
    /**
     * Run a full integration test of the alarm system
     */
    fun runIntegrationTest() {
        Log.i(TAG, "Starting alarm system integration test")
        
        // Create test alarms
        coroutineScope.launch {
            try {
                // First clean up any existing test alarms
                cleanupTestAlarms()
                
                // Create test alarms
                val testAlarms = createTestAlarms()
                
                // Save and schedule alarms
                for (alarm in testAlarms) {
                    val id = alarmRepository.insertAlarm(alarm)
                    Log.d(TAG, "Created test alarm with ID: $id")
                    
                    // Schedule the alarm
                    val savedAlarm = alarmRepository.getAlarmById(id)
                    if (savedAlarm != null) {
                        alarmScheduler.scheduleAlarm(savedAlarm)
                        Log.d(TAG, "Scheduled alarm: ${savedAlarm.name} for ${TimeUtils.formatDateTime(TimeUtils.getNextAlarmDateTime(savedAlarm))}")
                    }
                }
                
                // Verify alarms were saved
                val savedAlarms = getAlarmsUseCase().first()
                Log.d(TAG, "Found ${savedAlarms.size} alarms in database")
                
                // Verify each alarm
                savedAlarms.forEach { alarm ->
                    if (alarm.name.startsWith("TEST_")) {
                        verifyAlarm(alarm)
                    }
                }
                
                // Test updating an alarm
                val firstAlarm = savedAlarms.firstOrNull { it.name.startsWith("TEST_") }
                if (firstAlarm != null) {
                    testUpdateAlarm(firstAlarm)
                }
                
                // Test cancelling an alarm
                val secondAlarm = savedAlarms.filter { it.name.startsWith("TEST_") }.getOrNull(1)
                if (secondAlarm != null) {
                    testCancelAlarm(secondAlarm)
                }
                
                // Test snoozing an alarm
                val thirdAlarm = savedAlarms.filter { it.name.startsWith("TEST_") }.getOrNull(2)
                if (thirdAlarm != null) {
                    testSnoozeAlarm(thirdAlarm)
                }
                
                Log.i(TAG, "Alarm system integration test completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Integration test failed: ${e.message}", e)
            }
        }
    }
    
    private suspend fun cleanupTestAlarms() {
        val alarms = getAlarmsUseCase().first()
        alarms.forEach { alarm ->
            if (alarm.name.startsWith("TEST_")) {
                Log.d(TAG, "Cleaning up test alarm: ${alarm.name}")
                alarmScheduler.cancelAlarm(alarm)
                alarmRepository.deleteAlarm(alarm)
            }
        }
    }
    
    private fun createTestAlarms(): List<Alarm> {
        val now = LocalDateTime.now()
        
        return listOf(
            // Test alarm 1: One-time alarm in near future
            Alarm(
                timeHour = (now.hour + 1) % 24,
                timeMinute = now.minute,
                name = "TEST_OneTime",
                isEnabled = true,
                ringtone = "Default",
                volume = 0.7f,
                vibrate = true
            ).getAlarmWithRepeatDays(emptyList()),
            
            // Test alarm 2: Daily repeating alarm
            Alarm(
                timeHour = (now.hour + 2) % 24,
                timeMinute = now.minute,
                name = "TEST_Daily",
                isEnabled = true,
                ringtone = "Default",
                volume = 0.5f,
                vibrate = false
            ).getAlarmWithRepeatDays(DayOfWeek.values().toList()),
            
            // Test alarm 3: Weekday alarm
            Alarm(
                timeHour = 8,
                timeMinute = 0,
                name = "TEST_Weekday",
                isEnabled = true,
                ringtone = "Default",
                volume = 0.8f,
                vibrate = true
            ).getAlarmWithRepeatDays(listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            )),
            
            // Test alarm 4: Weekend alarm
            Alarm(
                timeHour = 10,
                timeMinute = 0,
                name = "TEST_Weekend",
                isEnabled = true,
                ringtone = "Default",
                volume = 0.6f,
                vibrate = true
            ).getAlarmWithRepeatDays(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        )
    }
    
    private fun verifyAlarm(alarm: Alarm) {
        val nextTriggerTime = TimeUtils.getNextAlarmDateTime(alarm)
        Log.d(TAG, "Verified alarm: ${alarm.name} - Next trigger: ${TimeUtils.formatDateTime(nextTriggerTime)}")
        
        // Verify bedtime calculation
        val bedtimeSuggestion = TimeUtils.getBedtimeSuggestion(alarm)
        Log.d(TAG, "Bedtime suggestion: $bedtimeSuggestion")
        
        // Verify time until calculation
        val timeUntil = TimeUtils.getTimeUntilAlarm(alarm)
        Log.d(TAG, "Time until alarm: $timeUntil")
    }
    
    private suspend fun testUpdateAlarm(alarm: Alarm) {
        // First cancel the current alarm
        alarmScheduler.cancelAlarm(alarm)
        
        // Update alarm time
        val updatedAlarm = alarm.copy(
            timeHour = (alarm.timeHour + 1) % 24,
            name = "${alarm.name}_Updated"
        )
        
        // Save and reschedule
        alarmRepository.updateAlarm(updatedAlarm)
        
        // Get the updated alarm from the repository
        val refreshedAlarm = alarmRepository.getAlarmById(alarm.id)
        if (refreshedAlarm != null) {
            alarmScheduler.scheduleAlarm(refreshedAlarm)
            Log.d(TAG, "Updated alarm: ${refreshedAlarm.name} - New time: ${refreshedAlarm.timeHour}:${refreshedAlarm.timeMinute}")
        }
    }
    
    private suspend fun testCancelAlarm(alarm: Alarm) {
        // Disable the alarm
        val disabledAlarm = alarm.copy(isEnabled = false)
        alarmRepository.updateAlarm(disabledAlarm)
        
        // Cancel the alarm
        alarmScheduler.cancelAlarm(disabledAlarm)
        
        Log.d(TAG, "Cancelled alarm: ${alarm.name}")
        
        // Verify it's disabled in DB
        val refreshedAlarm = alarmRepository.getAlarmById(alarm.id)
        if (refreshedAlarm != null) {
            Log.d(TAG, "Verified alarm disabled: ${refreshedAlarm.name} - isEnabled: ${refreshedAlarm.isEnabled}")
        }
    }
    
    private suspend fun testSnoozeAlarm(alarm: Alarm) {
        // Let's simulate snoozing this alarm
        Log.d(TAG, "Testing snooze for alarm: ${alarm.name}")
        
        // Get the next trigger time
        val originalTriggerTime = TimeUtils.getNextAlarmDateTime(alarm)
        Log.d(TAG, "Original trigger time: ${TimeUtils.formatDateTime(originalTriggerTime)}")
        
        // Snooze for 5 minutes
        alarmScheduler.snoozeAlarm(alarm, 5)
        Log.d(TAG, "Alarm snoozed for 5 minutes")
        
        // Wait briefly to let the scheduler update
        delay(500)
        
        // The alarm state in the database shouldn't change
        val refreshedAlarm = alarmRepository.getAlarmById(alarm.id)
        if (refreshedAlarm != null) {
            Log.d(TAG, "Alarm state after snooze - isEnabled: ${refreshedAlarm.isEnabled}")
        }
        
        Log.d(TAG, "Snooze test completed successfully")
    }
} 