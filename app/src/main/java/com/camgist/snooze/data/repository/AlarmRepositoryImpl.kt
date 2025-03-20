package com.camgist.snooze.data.repository

import com.camgist.snooze.data.db.dao.AlarmDao
import com.camgist.snooze.data.model.Alarm
import kotlinx.coroutines.flow.Flow

class AlarmRepositoryImpl(
    private val alarmDao: AlarmDao
) : AlarmRepository {
    
    override fun getAllAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarms()
    }
    
    override suspend fun getAlarmById(id: Long): Alarm? {
        return alarmDao.getAlarmById(id)
    }
    
    override suspend fun insertAlarm(alarm: Alarm): Long {
        return alarmDao.insertAlarm(alarm)
    }
    
    override suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
    }
    
    override suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
    }
    
    override suspend fun updateAlarmEnabled(id: Long, isEnabled: Boolean) {
        alarmDao.updateAlarmEnabled(id, isEnabled)
    }
    
    override suspend fun getEnabledAlarms(): List<Alarm> {
        return alarmDao.getEnabledAlarms()
    }
}