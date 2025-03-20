package com.camgist.snooze.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.camgist.snooze.ui.navigation.NavGraph

@Composable
fun SnoozlooApp(alarmId: Long = -1L) {
    val navController = rememberNavController()
    NavGraph(navController = navController, alarmId = alarmId)
} 