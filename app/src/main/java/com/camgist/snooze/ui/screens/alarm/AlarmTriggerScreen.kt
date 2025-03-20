package com.camgist.snooze.ui.screens.alarm

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.camgist.snooze.domain.util.TimeUtils
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext

@Composable
fun AlarmTriggerScreen(
    alarmId: Long,
    onDismiss: () -> Unit,
    viewModel: AlarmTriggerViewModel = koinViewModel { parametersOf(alarmId) }
) {
    // Get the context inside the Composable function body
    val context = LocalContext.current
    
    // Update the ViewModel with the context
    LaunchedEffect(Unit) {
        if (viewModel is AlarmTriggerViewModel) {
            (viewModel as? AlarmTriggerViewModel)?.updateContext(context)
        }
    }
    
    val alarm by viewModel.alarm.collectAsState()
    var showSnoozeOptions by remember { mutableStateOf(false) }
    
    // Animation for the pulsing effect
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Current time for display
    val currentTime = remember { mutableStateOf(LocalDateTime.now()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }
    
    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime.value = LocalDateTime.now()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section with alarm info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                // Current time and date
                Text(
                    text = currentTime.value.format(timeFormatter),
                    style = MaterialTheme.typography.displayLarge
                )
                Text(
                    text = currentTime.value.format(dateFormatter),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Alarm icon with animation
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Alarm name or default text
                Text(
                    text = alarm?.name?.takeIf { it.isNotEmpty() } ?: "Time to wake up!",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            }
            
            // Bottom section with actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                // Dismiss button
                Button(
                    onClick = {
                        viewModel.dismissAlarm()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AlarmOff,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dismiss")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Snooze button
                Button(
                    onClick = { showSnoozeOptions = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Snooze")
                }
            }
        }
    }
    
    // Snooze options dialog
    if (showSnoozeOptions) {
        SnoozeOptionsDialog(
            onDismiss = { showSnoozeOptions = false },
            onSnooze = { minutes ->
                viewModel.snoozeAlarm(minutes)
                showSnoozeOptions = false
                onDismiss()
            }
        )
    }
}

@Composable
fun SnoozeOptionsDialog(
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    val snoozeOptions = listOf(5, 10, 15, 30, 60)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Snooze for") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                snoozeOptions.forEach { minutes ->
                    Button(
                        onClick = { onSnooze(minutes) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (minutes < 60) "$minutes minutes" else "1 hour",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 