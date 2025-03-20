package com.camgist.snooze.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.domain.AlarmScheduler
import com.camgist.snooze.domain.util.TimeUtils
import java.time.LocalDateTime
import java.time.ZoneId

class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    override fun scheduleAlarm(alarm: Alarm) {
        val nextAlarmTime = TimeUtils.getNextAlarmDateTime(alarm)
        val pendingIntent = createPendingIntent(alarm)
        
        scheduleExactAlarm(nextAlarmTime, pendingIntent)
    }

    override fun cancelAlarm(alarm: Alarm) {
        val pendingIntent = createPendingIntent(alarm)
        alarmManager.cancel(pendingIntent)
    }

    override fun snoozeAlarm(alarm: Alarm, minutes: Int) {
        val pendingIntent = createPendingIntent(alarm)
        
        val snoozeTime = LocalDateTime.now().plusMinutes(minutes.toLong())
        scheduleExactAlarm(snoozeTime, pendingIntent)
    }
    
    private fun scheduleExactAlarm(dateTime: LocalDateTime, pendingIntent: PendingIntent) {
        val timeInMillis = dateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }
    
    private fun createPendingIntent(alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(ALARM_ID, alarm.id)
        }
        
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    companion object {
        const val ALARM_ID = "ALARM_ID"
    }
} 