package com.camgist.snooze.di

import android.app.AlarmManager
import android.content.Context
import com.camgist.snooze.alarm.AlarmScheduler
import com.camgist.snooze.alarm.AndroidAlarmScheduler
import com.camgist.snooze.domain.AlarmScheduler as DomainAlarmScheduler
import com.camgist.snooze.domain.usecase.DeleteAlarmUseCase
import com.camgist.snooze.domain.usecase.GetAlarmByIdUseCase
import com.camgist.snooze.domain.usecase.GetAlarmsUseCase
import com.camgist.snooze.domain.usecase.RescheduleAlarmsUseCase
import com.camgist.snooze.domain.usecase.SaveAlarmUseCase
import com.camgist.snooze.domain.usecase.ToggleAlarmEnabledUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    // Alarm manager
    single { androidContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    
    // Alarm scheduler
    single<DomainAlarmScheduler> { AndroidAlarmScheduler(androidContext()) }
    
    // Add the concrete AlarmScheduler implementation
    single { AlarmScheduler(androidContext(), get()) }
    
    // Use cases
    single { GetAlarmsUseCase(get()) }
    single { GetAlarmByIdUseCase(get()) }
    single { SaveAlarmUseCase(get(), get()) }
    single { DeleteAlarmUseCase(get(), get()) }
    single { ToggleAlarmEnabledUseCase(get(), get(), get()) }
    single { RescheduleAlarmsUseCase(get(), get()) }
} 