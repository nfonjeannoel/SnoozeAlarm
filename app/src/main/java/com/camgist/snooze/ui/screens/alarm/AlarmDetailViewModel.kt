package com.camgist.snooze.ui.screens.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.domain.usecase.DeleteAlarmUseCase
import com.camgist.snooze.domain.usecase.GetAlarmByIdUseCase
import com.camgist.snooze.domain.usecase.SaveAlarmUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.camgist.snooze.data.model.DayOfWeek

class AlarmDetailViewModel(
    private val alarmId: Long,
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val saveAlarmUseCase: SaveAlarmUseCase,
    private val deleteAlarmUseCase: DeleteAlarmUseCase
) : ViewModel() {
    
    private val _alarm = MutableStateFlow<Alarm?>(null)
    val alarm: StateFlow<Alarm?> = _alarm
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    init {
        if (alarmId != -1L) {
            loadAlarm()
        } else {
            _alarm.value = Alarm(
                timeHour = 8,
                timeMinute = 0,
                isEnabled = true
            )
        }
    }
    
    private fun loadAlarm() {
        viewModelScope.launch {
            _isLoading.value = true
            _alarm.value = getAlarmByIdUseCase(alarmId)
            _isLoading.value = false
        }
    }
    
    fun saveAlarm(
        hour: Int,
        minute: Int,
        name: String,
        repeatDays: List<DayOfWeek>,
        ringtone: String,
        volume: Float,
        vibrate: Boolean
    ) {
        val currentAlarm = _alarm.value ?: return
        
        val updatedAlarm = currentAlarm.copy(
            timeHour = hour,
            timeMinute = minute,
            name = name,
            ringtone = ringtone,
            volume = volume,
            vibrate = vibrate,
            isEnabled = true
        ).getAlarmWithRepeatDays(repeatDays)
        
        viewModelScope.launch {
            saveAlarmUseCase(updatedAlarm)
        }
    }
    
    fun deleteAlarm() {
        val currentAlarm = _alarm.value ?: return
        
        viewModelScope.launch {
            deleteAlarmUseCase(currentAlarm)
        }
    }
} 