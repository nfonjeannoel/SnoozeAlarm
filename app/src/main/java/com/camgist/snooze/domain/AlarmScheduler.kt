package com.camgist.snooze.domain

import com.camgist.snooze.data.model.Alarm

interface AlarmScheduler {
    fun scheduleAlarm(alarm: Alarm)
    fun cancelAlarm(alarm: Alarm)
    fun snoozeAlarm(alarm: Alarm, minutes: Int)
} 