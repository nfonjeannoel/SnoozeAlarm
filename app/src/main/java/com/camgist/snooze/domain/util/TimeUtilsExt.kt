package com.camgist.snooze.domain.util

import android.content.Context
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.model.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

// Extension functions for easier access to TimeUtils methods from UI

/**
 * Get formatted next occurrence string for an alarm.
 */
fun Alarm.formatNextOccurrence(): String {
    return TimeUtils.formatNextOccurrence(this)
}

/**
 * Get bedtime suggestion based on alarm time.
 */
fun Alarm.getBedtimeSuggestion(): String {
    return TimeUtils.getBedtimeSuggestion(this)
}

/**
 * Get the next scheduled date/time for the alarm.
 */
fun Alarm.getNextAlarmDateTime(): LocalDateTime {
    return TimeUtils.getNextAlarmDateTime(this)
}

/**
 * Format time using system preference (12/24 hour)
 */
fun Alarm.formatTime(context: Context): String {
    return TimeUtils.formatTimeBasedOnSystemPreference(context, timeHour, timeMinute)
}

/**
 * Get human-readable string for days of the week.
 */
fun Alarm.getRepeatDaysText(): String {
    if (repeatDays.isEmpty()) {
        return "One time alarm"
    }
    
    if (repeatDays.size == 7) {
        return "Every day"
    }
    
    if (repeatDays.size == 5 && 
        repeatDays.containsAll(listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        ))
    ) {
        return "Every weekday"
    }
    
    if (repeatDays.size == 2 && 
        repeatDays.containsAll(listOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        ))
    ) {
        return "Weekends"
    }
    
    return repeatDays.joinToString(", ") { TimeUtils.getShortDayName(it) }
}

/**
 * Check if alarm will trigger today.
 */
fun Alarm.triggersToday(): Boolean {
    return TimeUtils.shouldTriggerToday(this)
}

/**
 * Get days until the next alarm.
 */
fun Alarm.daysUntilNextTrigger(): Long {
    return TimeUtils.getDaysUntilAlarm(this)
}

/**
 * Get bedtime suggestion from hour and minute.
 */
fun getBedtimeSuggestion(hour: Int, minute: Int): String {
    return TimeUtils.getBedtimeSuggestion(hour, minute)
}

/**
 * Format time in 12-hour format.
 */
fun LocalTime.format12Hour(): String {
    return TimeUtils.formatTime12Hour(hour, minute)
}

/**
 * Format time in 24-hour format.
 */
fun LocalTime.format24Hour(): String {
    return TimeUtils.formatTime24Hour(hour, minute)
}

/**
 * Format LocalDateTime as a human-readable string.
 */
fun LocalDateTime.formatReadable(): String {
    return TimeUtils.formatDateTime(this)
}

/**
 * Convert LocalDateTime to milliseconds since epoch.
 */
fun LocalDateTime.toEpochMillis(): Long {
    return TimeUtils.toEpochMillis(this)
}

/**
 * Get the system's time preference format.
 */
fun Context.is24HourFormat(): Boolean {
    return android.text.format.DateFormat.is24HourFormat(this)
} 