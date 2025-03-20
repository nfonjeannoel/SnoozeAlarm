package com.camgist.snooze.util

import android.util.Log
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.model.DayOfWeek
import com.camgist.snooze.domain.util.TimeUtils
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Utility class for testing alarm calculations and edge cases
 * This can be used during development for debugging and testing.
 */
object AlarmTestUtil {
    private const val TAG = "AlarmTestUtil"
    
    /**
     * Test calculation of next alarm times for various scenarios
     */
    fun testAlarmCalculations() {
        Log.d(TAG, "Starting alarm calculation tests")
        
        val currentTime = LocalDateTime.now()
        val currentHour = currentTime.hour
        val currentMinute = currentTime.minute
        
        // Test one-time alarm in the future
        testOneTimeAlarmFuture()
        
        // Test one-time alarm in the past (should set for tomorrow)
        testOneTimeAlarmPast()
        
        // Test daily repeating alarm
        testDailyAlarm()
        
        // Test weekday alarm
        testWeekdayAlarm()
        
        // Test weekend alarm
        testWeekendAlarm()
        
        // Test specific day alarm
        testSpecificDayAlarm()
        
        Log.d(TAG, "Alarm calculation tests complete")
    }
    
    private fun testOneTimeAlarmFuture() {
        val now = LocalDateTime.now()
        val futureTime = now.plusHours(2)
        
        val alarm = Alarm(
            id = 1,
            timeHour = futureTime.hour,
            timeMinute = futureTime.minute,
            name = "Future one-time alarm",
            isEnabled = true
        )
        
        val nextAlarmTime = TimeUtils.getNextAlarmDateTime(alarm)
        Log.d(TAG, "One-time future alarm: ${formatTestOutput(alarm, nextAlarmTime)}")
    }
    
    private fun testOneTimeAlarmPast() {
        val now = LocalDateTime.now()
        val pastTime = now.minusHours(2)
        
        val alarm = Alarm(
            id = 2,
            timeHour = pastTime.hour,
            timeMinute = pastTime.minute,
            name = "Past one-time alarm",
            isEnabled = true
        )
        
        val nextAlarmTime = TimeUtils.getNextAlarmDateTime(alarm)
        Log.d(TAG, "One-time past alarm: ${formatTestOutput(alarm, nextAlarmTime)}")
    }
    
    private fun testDailyAlarm() {
        val now = LocalDateTime.now()
        val alarmTime = now.plusMinutes(30)
        
        val alarm = Alarm(
            id = 3,
            timeHour = alarmTime.hour,
            timeMinute = alarmTime.minute,
            name = "Daily alarm",
            isEnabled = true
        ).getAlarmWithRepeatDays(DayOfWeek.values().toList())
        
        val nextAlarmTime = TimeUtils.getNextAlarmDateTime(alarm)
        Log.d(TAG, "Daily alarm: ${formatTestOutput(alarm, nextAlarmTime)}")
    }
    
    private fun testWeekdayAlarm() {
        val now = LocalDateTime.now()
        val alarmTime = LocalTime.of(8, 0)
        
        val alarm = Alarm(
            id = 4,
            timeHour = alarmTime.hour,
            timeMinute = alarmTime.minute,
            name = "Weekday alarm",
            isEnabled = true
        ).getAlarmWithRepeatDays(listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        ))
        
        val nextAlarmTime = TimeUtils.getNextAlarmDateTime(alarm)
        Log.d(TAG, "Weekday alarm: ${formatTestOutput(alarm, nextAlarmTime)}")
    }
    
    private fun testWeekendAlarm() {
        val now = LocalDateTime.now()
        val alarmTime = LocalTime.of(10, 0)
        
        val alarm = Alarm(
            id = 5,
            timeHour = alarmTime.hour,
            timeMinute = alarmTime.minute,
            name = "Weekend alarm",
            isEnabled = true
        ).getAlarmWithRepeatDays(listOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        ))
        
        val nextAlarmTime = TimeUtils.getNextAlarmDateTime(alarm)
        Log.d(TAG, "Weekend alarm: ${formatTestOutput(alarm, nextAlarmTime)}")
    }
    
    private fun testSpecificDayAlarm() {
        val now = LocalDateTime.now()
        // Get a day 2 days from today
        val currentDayOfWeek = now.dayOfWeek
        val dayOffset = (currentDayOfWeek.value + 2) % 7
        val targetDay = java.time.DayOfWeek.of(if (dayOffset == 0) 7 else dayOffset)
        
        val appDayOfWeek = TimeUtils.mapToDayOfWeek(targetDay)
        
        val alarm = Alarm(
            id = 6,
            timeHour = 7,
            timeMinute = 30,
            name = "Specific day alarm",
            isEnabled = true
        ).getAlarmWithRepeatDays(listOf(appDayOfWeek))
        
        val nextAlarmTime = TimeUtils.getNextAlarmDateTime(alarm)
        Log.d(TAG, "Specific day alarm (${appDayOfWeek}): ${formatTestOutput(alarm, nextAlarmTime)}")
    }
    
    private fun formatTestOutput(alarm: Alarm, nextAlarmTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a")
        val now = LocalDateTime.now()
        
        return "ALARM: ${alarm.name} (${alarm.timeHour}:${alarm.timeMinute}) " +
               "NEXT: ${formatter.format(nextAlarmTime)} " +
               "(in ${formatTimeDifference(now, nextAlarmTime)})"
    }
    
    private fun formatTimeDifference(from: LocalDateTime, to: LocalDateTime): String {
        val diffSeconds = java.time.Duration.between(from, to).seconds
        
        val days = diffSeconds / (24 * 3600)
        val hours = (diffSeconds % (24 * 3600)) / 3600
        val minutes = (diffSeconds % 3600) / 60
        
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}min"
            hours > 0 -> "${hours}h ${minutes}min"
            else -> "${minutes}min"
        }
    }
} 