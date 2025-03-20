package com.camgist.snooze.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.camgist.snooze.ui.screens.alarm.AlarmDetailScreen
import com.camgist.snooze.ui.screens.alarm.AlarmListScreen
import com.camgist.snooze.ui.screens.alarm.AlarmTriggerScreen
import com.camgist.snooze.ui.screens.alarm.RingtoneSelectionScreen

@Composable
fun NavGraph(navController: NavHostController, alarmId: Long = -1L) {
    // Handle direct navigation to alarm trigger screen if an alarm ID is provided
    LaunchedEffect(alarmId) {
        if (alarmId != -1L) {
            navController.navigate(Screen.AlarmTrigger.createRoute(alarmId)) {
                popUpTo(Screen.AlarmList.route)
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = Screen.AlarmList.route
    ) {
        composable(Screen.AlarmList.route) {
            AlarmListScreen(
                onAlarmClick = { alarmId ->
                    navController.navigate(Screen.AlarmDetail.createRoute(alarmId))
                },
                onAddAlarmClick = {
                    navController.navigate(Screen.AlarmDetail.route)
                }
            )
        }
        
        composable(
            route = Screen.AlarmDetail.route + "?alarmId={alarmId}",
            arguments = listOf(
                navArgument("alarmId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
            AlarmDetailScreen(
                alarmId = alarmId,
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveClick = {
                    navController.popBackStack()
                },
                onRingtoneClick = {
                    navController.navigate(Screen.RingtoneSelection.route)
                },
                navController = navController
            )
        }
        
        composable(Screen.RingtoneSelection.route) {
            // Get current values before navigating to ringtone selection
            val currentValues = navController.previousBackStackEntry?.savedStateHandle?.get<TempAlarmState>("current_alarm_state")
            
            RingtoneSelectionScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRingtoneSelected = { ringtone ->
                    // Update the ringtone in the current values
                    val updatedValues = currentValues?.copy(ringtone = ringtone) ?: TempAlarmState(ringtone = ringtone)
                    navController.previousBackStackEntry?.savedStateHandle?.set("current_alarm_state", updatedValues)
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_ringtone", ringtone)
                    navController.popBackStack()
                },
                initialSelectedRingtone = currentValues?.ringtone ?: "Default"
            )
        }
        
        composable(
            route = Screen.AlarmTrigger.route + "/{alarmId}",
            arguments = listOf(
                navArgument("alarmId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
            AlarmTriggerScreen(
                alarmId = alarmId,
                onDismiss = {
                    navController.popBackStack(Screen.AlarmList.route, false)
                }
            )
        }
    }
} 