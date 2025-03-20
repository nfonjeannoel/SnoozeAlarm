package com.camgist.snooze.ui.navigation

sealed class Screen(val route: String) {
    data object AlarmList : Screen("alarm_list")
    data object AlarmDetail : Screen("alarm_detail") {
        fun createRoute(alarmId: Long): String {
            return "$route?alarmId=$alarmId"
        }
    }
    data object AlarmTrigger : Screen("alarm_trigger") {
        fun createRoute(alarmId: Long): String {
            return "$route/$alarmId"
        }
    }
    data object RingtoneSelection : Screen("ringtone_selection")
} 