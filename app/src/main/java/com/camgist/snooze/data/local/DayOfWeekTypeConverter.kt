package com.camgist.snooze.data.local

import androidx.room.TypeConverter
import com.camgist.snooze.data.model.DayOfWeek

class DayOfWeekTypeConverter {
    @TypeConverter
    fun fromString(value: String?): List<DayOfWeek> {
        return value?.split(",")
            ?.filter { it.isNotEmpty() }
            ?.map { DayOfWeek.valueOf(it) }
            ?: emptyList()
    }
    
    @TypeConverter
    fun toString(days: List<DayOfWeek>?): String {
        return days?.joinToString(",") { it.name } ?: ""
    }
} 