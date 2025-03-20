package com.camgist.snooze.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.camgist.snooze.data.db.dao.AlarmDao
import com.camgist.snooze.data.model.Alarm

@Database(entities = [Alarm::class], version = 1, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
} 