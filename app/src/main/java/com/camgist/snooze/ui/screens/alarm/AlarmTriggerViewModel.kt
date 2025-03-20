package com.camgist.snooze.ui.screens.alarm

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camgist.snooze.alarm.AlarmService
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.domain.AlarmScheduler
import com.camgist.snooze.domain.usecase.GetAlarmByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlarmTriggerViewModel(
    private val alarmId: Long,
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase,
    private val alarmScheduler: AlarmScheduler,
    private var context: Context? = null
) : ViewModel() {
    
    private val _alarm = MutableStateFlow<Alarm?>(null)
    val alarm: StateFlow<Alarm?> = _alarm
    
    init {
        loadAlarm()
    }
    
    fun updateContext(context: Context) {
        this.context = context
    }
    
    private fun loadAlarm() {
        viewModelScope.launch {
            _alarm.value = getAlarmByIdUseCase(alarmId)
        }
    }
    
    fun snoozeAlarm(minutes: Int) {
        val currentAlarm = _alarm.value ?: return
        val currentContext = context ?: return
        
        // Send intent to AlarmService to snooze
        val intent = Intent(currentContext, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra(AlarmService.EXTRA_SNOOZE_MINUTES, minutes)
        }
        currentContext.startService(intent)
        
        // This is also handled by the service, but we call it here too for redundancy
        alarmScheduler.snoozeAlarm(currentAlarm, minutes)
    }
    
    fun dismissAlarm() {
        val currentContext = context ?: return
        
        // Send intent to AlarmService to dismiss
        val intent = Intent(currentContext, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        currentContext.startService(intent)
    }
} 