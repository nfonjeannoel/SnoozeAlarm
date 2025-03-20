package com.camgist.snooze.di

import com.camgist.snooze.ui.alarmdetail.AlarmDetailViewModel
import com.camgist.snooze.ui.screens.alarm.AlarmListViewModel
import com.camgist.snooze.ui.screens.alarm.AlarmTriggerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    // Use viewModelOf syntax for auto-injection of dependencies
    viewModelOf(::AlarmListViewModel)
    
    // For ViewModels with parameters, we use viewModel with lambda
    viewModel { params ->
        AlarmDetailViewModel(params.get(), get(), get())
    }
    
    viewModel { params ->
        AlarmTriggerViewModel(
            alarmId = params.get(),
            getAlarmByIdUseCase = get(),
            alarmScheduler = get()
        )
    }
} 