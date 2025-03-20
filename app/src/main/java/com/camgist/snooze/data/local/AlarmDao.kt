package com.camgist.snooze.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.camgist.snooze.data.model.Alarm
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY timeHour, timeMinute")
    fun getAllAlarms(): Flow<List<Alarm>>
    
    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY timeHour, timeMinute")
    suspend fun getEnabledAlarms(): List<Alarm>
    
    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): Alarm?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long
    
    @Update
    suspend fun updateAlarm(alarm: Alarm)
    
    @Delete
    suspend fun deleteAlarm(alarm: Alarm)
} 