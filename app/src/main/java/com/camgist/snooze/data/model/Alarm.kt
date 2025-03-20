package com.camgist.snooze.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timeHour: Int,
    val timeMinute: Int,
    val name: String = "",
    val isEnabled: Boolean = true,
    val repeatDaysString: String = "",
    val ringtone: String = "Default",
    val volume: Float = 0.5f, // 50% default
    val vibrate: Boolean = false
) {
    // Extension properties to work with the repeatDays as a List<DayOfWeek>
    val repeatDays: List<DayOfWeek>
        get() = if (repeatDaysString.isEmpty()) {
            emptyList()
        } else {
            repeatDaysString.split(",")
                .filter { it.isNotEmpty() }
                .mapNotNull { dayString -> 
                    try {
                        dayString.toIntOrNull()?.let { DayOfWeek.from(it) }
                    } catch (e: Exception) {
                        null
                    }
                }
        }
        
    // Helper function to set repeatDays
    fun getAlarmWithRepeatDays(days: List<DayOfWeek>): Alarm {
        val daysString = days.joinToString(",") { it.value.toString() }
        return this.copy(repeatDaysString = daysString)
    }
} 