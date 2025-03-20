package com.camgist.snooze.ui.screens.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.domain.util.formatNextOccurrence
import com.camgist.snooze.domain.util.getBedtimeSuggestion
import org.koin.androidx.compose.koinViewModel
import com.camgist.snooze.data.model.DayOfWeek
import com.camgist.snooze.domain.util.TimeUtils.formatNextOccurrence
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel = koinViewModel(),
    onAlarmClick: (Long) -> Unit,
    onAddAlarmClick: () -> Unit
) {
    val alarms by viewModel.alarms.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Alarms") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlarmClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { paddingValues ->
        if (alarms.isEmpty()) {
            EmptyAlarmList(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(alarms) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onClick = { onAlarmClick(alarm.id) },
                        onToggleEnabled = { viewModel.toggleAlarmEnabled(alarm) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAlarmList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "It's empty! Add the first alarm so you don't miss an important moment!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AlarmCard(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Alarm name and toggle switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (alarm.name.isNotEmpty()) {
                    Text(
                        text = alarm.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }
            
            // Alarm time
            Text(
                text = formatTime(alarm.timeHour, alarm.timeMinute),
                style = MaterialTheme.typography.displaySmall
            )
            
            // Next occurrence text
            val nextOccurrence = formatNextOccurrence(alarm)
            Text(
                text = nextOccurrence,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Repeat days
            if (alarm.repeatDays.isNotEmpty()) {
                DaysOfWeekRow(alarm.repeatDays)
            }
            
            // Bedtime suggestion
            val bedtimeSuggestion = getBedtimeSuggestion(alarm.timeHour, alarm.timeMinute)
            Text(
                text = bedtimeSuggestion,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DaysOfWeekRow(repeatDays: List<DayOfWeek>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DayOfWeek.values().forEach { day ->
            val isSelected = repeatDays.contains(day)
            DayChip(
                day = day,
                isSelected = isSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DayChip(
    day: DayOfWeek,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    // Map our custom DayOfWeek to a label
    val dayLabel = when(day) {
        DayOfWeek.MONDAY -> "M"
        DayOfWeek.TUESDAY -> "T"
        DayOfWeek.WEDNESDAY -> "W"
        DayOfWeek.THURSDAY -> "T"
        DayOfWeek.FRIDAY -> "F"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "S"
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Helper function to format time based on system's 12/24 hour format
@Composable
private fun formatTime(hour: Int, minute: Int): String {
    val context = LocalContext.current
    val is24HourFormat = remember(context) {
        android.text.format.DateFormat.is24HourFormat(context)
    }
    val formatter = DateTimeFormatter.ofPattern(
        if (is24HourFormat) "HH:mm" else "h:mm a"
    )
    val time = java.time.LocalTime.of(hour, minute)
    return time.format(formatter)
} 