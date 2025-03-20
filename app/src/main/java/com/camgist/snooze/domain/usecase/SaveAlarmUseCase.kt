package com.camgist.snooze.domain.usecase

import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.repository.AlarmRepository
import com.camgist.snooze.domain.AlarmScheduler

class SaveAlarmUseCase(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(alarm: Alarm): Long {
        val alarmId = if (alarm.id == 0L) {
            repository.insertAlarm(alarm)
        } else {
            repository.updateAlarm(alarm)
            alarm.id
        }
        
        if (alarm.isEnabled) {
            alarmScheduler.scheduleAlarm(alarm.copy(id = alarmId))
        }
        
        return alarmId
    }
} 