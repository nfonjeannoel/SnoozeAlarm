package com.camgist.snooze.domain.usecase

import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.repository.AlarmRepository
import com.camgist.snooze.domain.AlarmScheduler

class ToggleAlarmEnabledUseCase(
    private val repository: AlarmRepository,
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(alarmId: Long, isEnabled: Boolean) {
        repository.updateAlarmEnabled(alarmId, isEnabled)
        
        val alarm = getAlarmByIdUseCase(alarmId) ?: return
        
        if (isEnabled) {
            alarmScheduler.scheduleAlarm(alarm)
        } else {
            alarmScheduler.cancelAlarm(alarm)
        }
    }
} 