package com.camgist.snooze.domain.usecase

import com.camgist.snooze.data.repository.AlarmRepository
import com.camgist.snooze.domain.AlarmScheduler

class RescheduleAlarmsUseCase(
    private val repository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke() {
        val enabledAlarms = repository.getEnabledAlarms()
        enabledAlarms.forEach { alarm ->
            alarmScheduler.scheduleAlarm(alarm)
        }
    }
} 