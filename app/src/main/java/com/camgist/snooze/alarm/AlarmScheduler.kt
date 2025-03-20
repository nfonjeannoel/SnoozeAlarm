package com.camgist.snooze.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.model.DayOfWeek
import java.util.Calendar

class AlarmScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager
) {
    fun scheduleAlarm(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm)
            return
        }
        
        val triggerTime = calculateNextTriggerTime(alarm)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Schedule the alarm with AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
    
    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    fun calculateNextTriggerTime(alarm: Alarm): Long {
        val calendar = Calendar.getInstance()
        
        // If no repeat days are set, schedule for the next occurrence
        if (alarm.repeatDays.isEmpty()) {
            calendar.set(Calendar.HOUR_OF_DAY, alarm.timeHour)
            calendar.set(Calendar.MINUTE, alarm.timeMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            // If the time is in the past, schedule for the next day
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            return calendar.timeInMillis
        }
        
        // For repeating alarms, find the next occurrence based on the repeat days
        val currentDay = mapToAppDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
        val currentTimeInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val alarmTimeInMinutes = alarm.timeHour * 60 + alarm.timeMinute
        
        // Check if alarm should trigger today
        val shouldTriggerToday = alarm.repeatDays.contains(currentDay) && currentTimeInMinutes < alarmTimeInMinutes
        
        if (shouldTriggerToday) {
            // Set time for today
            calendar.set(Calendar.HOUR_OF_DAY, alarm.timeHour)
            calendar.set(Calendar.MINUTE, alarm.timeMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
        
        // Find the next day in the week when the alarm should trigger
        var daysToAdd = 1
        var nextDay = addDays(currentDay, daysToAdd)
        
        while (!alarm.repeatDays.contains(nextDay) && daysToAdd < 7) {
            daysToAdd++
            nextDay = addDays(currentDay, daysToAdd)
        }
        
        // Set time for the next occurrence
        calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
        calendar.set(Calendar.HOUR_OF_DAY, alarm.timeHour)
        calendar.set(Calendar.MINUTE, alarm.timeMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar.timeInMillis
    }
    
    private fun mapToAppDayOfWeek(calendarDayOfWeek: Int): DayOfWeek {
        return when (calendarDayOfWeek) {
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.TUESDAY -> DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> DayOfWeek.THURSDAY
            Calendar.FRIDAY -> DayOfWeek.FRIDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            Calendar.SUNDAY -> DayOfWeek.SUNDAY
            else -> throw IllegalArgumentException("Invalid day of week: $calendarDayOfWeek")
        }
    }
    
    private fun addDays(day: DayOfWeek, daysToAdd: Int): DayOfWeek {
        val days = DayOfWeek.values()
        val currentIndex = days.indexOf(day)
        val newIndex = (currentIndex + daysToAdd) % days.size
        return days[newIndex]
    }
} 