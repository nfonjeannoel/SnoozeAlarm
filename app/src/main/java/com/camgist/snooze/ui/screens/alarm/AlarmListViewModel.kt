package com.camgist.snooze.ui.screens.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.domain.AlarmScheduler
import com.camgist.snooze.data.repository.AlarmRepository
import com.camgist.snooze.domain.usecase.GetAlarmsUseCase
import com.camgist.snooze.domain.usecase.ToggleAlarmEnabledUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val getAlarmsUseCase: GetAlarmsUseCase,
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    
    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms
    
    init {
        loadAlarms()
    }
    
    private fun loadAlarms() {
        getAlarmsUseCase()
            .onEach { alarmList ->
                _alarms.value = alarmList
            }
            .launchIn(viewModelScope)
    }
    
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
} 