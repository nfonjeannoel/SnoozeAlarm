package com.camgist.snooze.ui.alarmlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camgist.snooze.alarm.AlarmScheduler
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.repository.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    
    val alarms = alarmRepository.getAllAlarms().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun toggleAlarmEnabled(alarm: Alarm) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
            alarmRepository.updateAlarm(updatedAlarm)
            if (updatedAlarm.isEnabled) {
                alarmScheduler.scheduleAlarm(updatedAlarm)
            } else {
                alarmScheduler.cancelAlarm(updatedAlarm)
            }
        }
    }
    
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.deleteAlarm(alarm)
            alarmScheduler.cancelAlarm(alarm)
        }
    }
} 