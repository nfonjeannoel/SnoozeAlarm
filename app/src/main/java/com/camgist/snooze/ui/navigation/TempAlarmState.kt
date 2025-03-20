package com.camgist.snooze.ui.navigation

import android.os.Parcelable
import com.camgist.snooze.data.model.DayOfWeek
import kotlinx.parcelize.Parcelize

@Parcelize
data class TempAlarmState(
    val hour: Int = 8,
    val minute: Int = 0,
    val name: String = "",
    val repeatDays: List<DayOfWeek> = emptyList(),
    val ringtone: String = "Default",
    val volume: Float = 0.5f,
    val vibrate: Boolean = false
) : Parcelable 