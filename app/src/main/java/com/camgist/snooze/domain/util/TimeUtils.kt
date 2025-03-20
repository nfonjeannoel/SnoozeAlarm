package com.camgist.snooze.domain.util

import android.text.format.DateFormat
import android.util.Log
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.model.DayOfWeek
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object TimeUtils {
    private const val TAG = "TimeUtils"
    
    private val TIME_FORMATTER_12H = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val TIME_FORMATTER_24H = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    
    /**
     * Calculate the next occurrence of an alarm
     * Handles edge cases like:
     * - One-time alarms vs repeating alarms
     * - DST changes
     * - Valid time verification
     */
    fun getNextAlarmDateTime(alarm: Alarm): LocalDateTime {
        val now = LocalDateTime.now()
        val alarmTime = LocalTime.of(alarm.timeHour, alarm.timeMinute)
        
        // If there are no repeat days, just calculate the next occurrence
        if (alarm.repeatDays.isEmpty()) {
            var alarmDateTime = now.with(alarmTime)
            
            // If alarm time is earlier than current time, set it for tomorrow
            if (alarmDateTime.isBefore(now)) {
                alarmDateTime = alarmDateTime.plusDays(1)
            }
            
            Log.d(TAG, "Next one-time alarm at: ${formatDateTime(alarmDateTime)}")
            return alarmDateTime
        }
        
        // Handle repeating alarms
        val currentDayOfWeek = mapToDayOfWeek(now.dayOfWeek)
        val sortedDays = alarm.repeatDays.sortedBy { it.ordinal }
        
        // Check if alarm should trigger today
        if (sortedDays.contains(currentDayOfWeek)) {
            val todayDateTime = now.with(alarmTime)
            if (todayDateTime.isAfter(now)) {
                Log.d(TAG, "Next repeat alarm today at: ${formatDateTime(todayDateTime)}")
                return todayDateTime
            }
        }
        
        // Find the next day that has an alarm
        val orderedDaysOfWeek = sortedDays.map { mapToJavaDayOfWeek(it) }
        
        // Try to find the next day in the current week
        for (dayOfWeek in orderedDaysOfWeek) {
            val nextAlarmDate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(dayOfWeek))
            
            // Skip if it's the same day (we already checked "today" above)
            if (nextAlarmDate.isEqual(now.toLocalDate())) {
                continue
            }
            
            val nextAlarmDateTime = LocalDateTime.of(nextAlarmDate, alarmTime)
            if (nextAlarmDateTime.isAfter(now)) {
                Log.d(TAG, "Next repeat alarm on ${dayOfWeek.name} at: ${formatDateTime(nextAlarmDateTime)}")
                return nextAlarmDateTime
            }
        }
        
        // If we reach here, we need to go to the first day in the next week
        val firstEnabledDay = orderedDaysOfWeek.first()
        val nextAlarmDateTime = now.with(
            TemporalAdjusters.next(firstEnabledDay)
        ).with(alarmTime)
        
        Log.d(TAG, "Next repeat alarm next week on ${firstEnabledDay.name}: ${formatDateTime(nextAlarmDateTime)}")
        return nextAlarmDateTime
    }
    
    /**
     * Format time in 12-hour format with AM/PM
     */
    fun formatTime12Hour(hour: Int, minute: Int): String {
        val time = LocalTime.of(hour, minute)
        return TIME_FORMATTER_12H.format(time)
    }
    
    /**
     * Format time in 24-hour format
     */
    fun formatTime24Hour(hour: Int, minute: Int): String {
        val time = LocalTime.of(hour, minute)
        return TIME_FORMATTER_24H.format(time)
    }
    
    /**
     * Format time based on user's system preference (12/24 hour)
     */
    fun formatTimeBasedOnSystemPreference(context: android.content.Context, hour: Int, minute: Int): String {
        val is24Hour = DateFormat.is24HourFormat(context)
        return if (is24Hour) formatTime24Hour(hour, minute) else formatTime12Hour(hour, minute)
    }
    
    /**
     * Calculate and format the time until the next alarm
     * Returns a human-readable string like "Alarm in 2d 5h 30min"
     */
    fun getTimeUntilAlarm(alarm: Alarm): String {
        val nextAlarmTime = getNextAlarmDateTime(alarm)
        val now = LocalDateTime.now()
        
        val diffSeconds = Duration.between(now, nextAlarmTime).seconds
        
        // Handle edge case where for some reason the alarm time is in the past
        if (diffSeconds < 0) {
            Log.w(TAG, "Warning: Alarm time is in the past, recalculating...")
            return "Alarm soon"
        }
        
        val days = diffSeconds / (24 * 3600)
        val hours = (diffSeconds % (24 * 3600)) / 3600
        val minutes = (diffSeconds % 3600) / 60
        
        return when {
            days > 0 -> "Alarm in ${days}d ${hours}h ${minutes}min"
            hours > 0 -> "Alarm in ${hours}h ${minutes}min"
            minutes > 0 -> "Alarm in ${minutes}min"
            else -> "Alarm now"
        }
    }
    
    /**
     * Calculate and return a bedtime suggestion based on the alarm time
     * Returns a formatted string suggesting when to sleep for 8 hours
     */
    fun getBedtimeSuggestion(alarm: Alarm): String {
        val alarmTime = LocalTime.of(alarm.timeHour, alarm.timeMinute)
        val bedtime = alarmTime.minusHours(8)
        return "Go to bed at ${TIME_FORMATTER_12H.format(bedtime)} to get 8h of sleep"
    }
    
    /**
     * Helper function for AlarmListScreen to get formatted next occurrence string
     */
    fun formatNextOccurrence(alarm: Alarm): String {
        return getTimeUntilAlarm(alarm)
    }
    
    /**
     * Helper function for AlarmListScreen to get bedtime suggestion from hour and minute
     */
    fun getBedtimeSuggestion(hour: Int, minute: Int): String {
        val alarmTime = LocalTime.of(hour, minute)
        val bedtime = alarmTime.minusHours(8)
        return "Go to bed at ${TIME_FORMATTER_12H.format(bedtime)} to get 8h of sleep"
    }
    
    /**
     * Format a LocalDateTime to a human-readable string
     */
    fun formatDateTime(dateTime: LocalDateTime): String {
        return DATE_TIME_FORMATTER.format(dateTime)
    }
    
    /**
     * Convert a LocalDateTime to milliseconds since epoch
     * Used for scheduling with AlarmManager
     */
    fun toEpochMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    
    /**
     * Convert milliseconds since epoch to LocalDateTime
     */
    fun fromEpochMillis(millis: Long): LocalDateTime {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }
    
    /**
     * Check if an alarm should trigger today
     */
    fun shouldTriggerToday(alarm: Alarm): Boolean {
        val now = LocalDateTime.now()
        val currentDay = mapToDayOfWeek(now.dayOfWeek)
        val alarmTime = LocalTime.of(alarm.timeHour, alarm.timeMinute)
        
        return alarm.repeatDays.contains(currentDay) && 
               now.toLocalTime().isBefore(alarmTime)
    }
    
    /**
     * Calculate days until the next alarm
     */
    fun getDaysUntilAlarm(alarm: Alarm): Long {
        val now = LocalDateTime.now()
        val nextAlarmTime = getNextAlarmDateTime(alarm)
        
        return ChronoUnit.DAYS.between(
            now.toLocalDate(),
            nextAlarmTime.toLocalDate()
        )
    }
    
    // Utility functions to convert between our DayOfWeek and java.time.DayOfWeek
    fun mapToJavaDayOfWeek(day: DayOfWeek): JavaDayOfWeek {
        return when(day) {
            DayOfWeek.MONDAY -> JavaDayOfWeek.MONDAY
            DayOfWeek.TUESDAY -> JavaDayOfWeek.TUESDAY
            DayOfWeek.WEDNESDAY -> JavaDayOfWeek.WEDNESDAY
            DayOfWeek.THURSDAY -> JavaDayOfWeek.THURSDAY
            DayOfWeek.FRIDAY -> JavaDayOfWeek.FRIDAY
            DayOfWeek.SATURDAY -> JavaDayOfWeek.SATURDAY
            DayOfWeek.SUNDAY -> JavaDayOfWeek.SUNDAY
        }
    }
    
    fun mapToDayOfWeek(day: JavaDayOfWeek): DayOfWeek {
        return when(day) {
            JavaDayOfWeek.MONDAY -> DayOfWeek.MONDAY
            JavaDayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            JavaDayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            JavaDayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            JavaDayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            JavaDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            JavaDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
    }
    
    /**
     * Get short day name (Mon, Tue, etc.) for a day of week
     */
    fun getShortDayName(day: DayOfWeek): String {
        return when(day) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        }
    }
    
    /**
     * Get a single character day representation for UI
     */
    fun getDayLetter(day: DayOfWeek): String {
        return when(day) {
            DayOfWeek.MONDAY -> "M"
            DayOfWeek.TUESDAY -> "T"
            DayOfWeek.WEDNESDAY -> "W"
            DayOfWeek.THURSDAY -> "T"
            DayOfWeek.FRIDAY -> "F"
            DayOfWeek.SATURDAY -> "S"
            DayOfWeek.SUNDAY -> "S"
        }
    }
} 