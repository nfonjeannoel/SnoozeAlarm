package com.camgist.snooze.data.repository

import com.camgist.snooze.data.model.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<Alarm>>
    
    suspend fun getEnabledAlarms(): List<Alarm>
    
    suspend fun getAlarmById(id: Long): Alarm?
    
    suspend fun insertAlarm(alarm: Alarm): Long
    
    suspend fun updateAlarm(alarm: Alarm)
    
    suspend fun deleteAlarm(alarm: Alarm)
    
    suspend fun updateAlarmEnabled(id: Long, isEnabled: Boolean)
} 