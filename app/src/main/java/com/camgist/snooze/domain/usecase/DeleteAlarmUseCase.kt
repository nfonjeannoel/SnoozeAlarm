package com.camgist.snooze.domain.usecase

import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.repository.AlarmRepository
import com.camgist.snooze.domain.AlarmScheduler

class DeleteAlarmUseCase(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(alarm: Alarm) {
        repository.deleteAlarm(alarm)
        alarmScheduler.cancelAlarm(alarm)
    }
} 