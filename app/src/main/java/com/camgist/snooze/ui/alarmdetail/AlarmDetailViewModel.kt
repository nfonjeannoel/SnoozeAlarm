package com.camgist.snooze.ui.alarmdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camgist.snooze.alarm.AlarmScheduler
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.data.model.DayOfWeek
import com.camgist.snooze.data.repository.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlarmDetailViewModel(
    private val alarmId: Long?,
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    
    private val _alarmState = MutableStateFlow(
        Alarm(
            timeHour = 8,
            timeMinute = 0
        )
    )
    val alarmState: StateFlow<Alarm> = _alarmState.asStateFlow()
    
    init {
        if (alarmId != null && alarmId != 0L) {
            loadAlarm(alarmId)
        }
    }
    
    private fun loadAlarm(id: Long) {
        viewModelScope.launch {
            val alarm = alarmRepository.getAlarmById(id)
            alarm?.let {
                _alarmState.value = it
            }
        }
    }
    
    fun updateTime(hour: Int, minute: Int) {
        _alarmState.update { it.copy(timeHour = hour, timeMinute = minute) }
    }
    
    fun updateName(name: String) {
        _alarmState.update { it.copy(name = name) }
    }
    
    fun toggleDayOfWeek(day: DayOfWeek) {
        _alarmState.update { currentAlarm ->
            val updatedDays = if (currentAlarm.repeatDays.contains(day)) {
                currentAlarm.repeatDays - day
            } else {
                currentAlarm.repeatDays + day
            }
            currentAlarm.getAlarmWithRepeatDays(updatedDays)
        }
    }
    
    fun updateRingtone(ringtone: String) {
        _alarmState.update { it.copy(ringtone = ringtone) }
    }
    
    fun updateVolume(volume: Float) {
        _alarmState.update { it.copy(volume = volume) }
    }
    
    fun toggleVibration() {
        _alarmState.update { it.copy(vibrate = !it.vibrate) }
    }
    
    fun saveAlarm(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val alarm = _alarmState.value
            val id = if (alarm.id == 0L) {
                alarmRepository.insertAlarm(alarm)
            } else {
                alarmRepository.updateAlarm(alarm)
                alarm.id
            }
            
            // Schedule the alarm if it's enabled
            if (alarm.isEnabled) {
                alarmScheduler.scheduleAlarm(alarm.copy(id = id))
            }
            
            onSuccess()
        }
    }
    
    fun deleteAlarm() {
        viewModelScope.launch {
            val alarm = _alarmState.value
            alarmRepository.deleteAlarm(alarm)
            // Cancel the alarm in the scheduler
            if (alarm.isEnabled) {
                alarmScheduler.cancelAlarm(alarm)
            }
        }
    }
} 