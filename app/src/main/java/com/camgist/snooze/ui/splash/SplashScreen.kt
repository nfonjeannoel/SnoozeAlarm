package com.camgist.snooze.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToAlarmList: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        // TODO: Add app logo and animation
    }
    
    // Navigate to AlarmList after a delay
    LaunchedEffect(key1 = true) {
        delay(2000) // 2 seconds
        onNavigateToAlarmList()
    }
}