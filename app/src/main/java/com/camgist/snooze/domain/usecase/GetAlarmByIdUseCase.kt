package com.camgist.snooze.domain.usecase

import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.repository.AlarmRepository

class GetAlarmByIdUseCase(private val repository: AlarmRepository) {
    suspend operator fun invoke(id: Long): Alarm? {
        return repository.getAlarmById(id)
    }
} 