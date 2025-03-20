package com.camgist.snooze.data.db.converters

import androidx.room.TypeConverter
import com.camgist.snooze.data.model.DayOfWeek

class DayOfWeekListConverter {
    @TypeConverter
    fun fromDayOfWeekList(dayOfWeekList: List<DayOfWeek>?): String {
        if (dayOfWeekList.isNullOrEmpty()) {
            return ""
        }
        return dayOfWeekList.joinToString(",") { it.value.toString() }
    }

    @TypeConverter
    fun toDayOfWeekList(dayOfWeekString: String?): List<DayOfWeek> {
        if (dayOfWeekString.isNullOrEmpty()) {
            return emptyList()
        }
        return dayOfWeekString.split(",")
            .filter { it.isNotEmpty() }
            .map { 
                try {
                    DayOfWeek.from(it.toInt())
                } catch (e: Exception) {
                    // Default to Monday if there's an error
                    DayOfWeek.MONDAY
                }
            }
    }
} 