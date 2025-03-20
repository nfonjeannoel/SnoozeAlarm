package com.camgist.snooze.domain.usecase

import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow

class GetAlarmsUseCase(private val repository: AlarmRepository) {
    operator fun invoke(): Flow<List<Alarm>> {
        return repository.getAllAlarms()
    }
} 