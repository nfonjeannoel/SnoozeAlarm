package com.camgist.snooze.ui.screens.alarm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.camgist.snooze.data.model.Alarm
import com.camgist.snooze.domain.util.TimeUtils
import com.camgist.snooze.data.model.DayOfWeek
import java.util.*
import com.camgist.snooze.ui.alarmdetail.AlarmDetailViewModel
import com.camgist.snooze.ui.navigation.TempAlarmState
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDetailScreen(
    alarmId: Long,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRingtoneClick: () -> Unit,
    navController: NavController = androidx.navigation.compose.rememberNavController()
) {
    val viewModel: AlarmDetailViewModel = getViewModel { parametersOf(alarmId) }
    val alarm by viewModel.alarmState.collectAsState()
    
    // Initialize UI state with remembered values to preserve across recompositions
    var hour by rememberSaveable { mutableStateOf(8) }
    var minute by rememberSaveable { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var repeatDays by rememberSaveable { mutableStateOf<List<DayOfWeek>>(emptyList()) }
    var ringtone by rememberSaveable { mutableStateOf("Default") }
    var volume by rememberSaveable { mutableStateOf(0.5f) }
    var vibrate by rememberSaveable { mutableStateOf(false) }
    
    // First, check if we have saved state from navigation
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    
    // Try to restore from temp state if available
    LaunchedEffect(Unit) {
        savedStateHandle?.get<TempAlarmState>("current_alarm_state")?.let { tempState ->
            hour = tempState.hour
            minute = tempState.minute
            name = tempState.name
            repeatDays = tempState.repeatDays
            ringtone = tempState.ringtone
            volume = tempState.volume
            vibrate = tempState.vibrate
            
            // Update the viewModel with these values
            viewModel.updateTime(hour, minute)
            viewModel.updateName(name)
            if (ringtone != alarm.ringtone) {
                viewModel.updateRingtone(ringtone)
            }
            viewModel.updateVolume(volume)
            if (vibrate != alarm.vibrate) {
                viewModel.toggleVibration()
            }
            // Update repeat days
            val currentDays = alarm.repeatDays
            repeatDays.forEach { day ->
                if (!currentDays.contains(day)) {
                    viewModel.toggleDayOfWeek(day)
                }
            }
            currentDays.forEach { day ->
                if (!repeatDays.contains(day)) {
                    viewModel.toggleDayOfWeek(day)
                }
            }
        }
    }
    
    // When alarm data is loaded, if we don't have saved state, update UI from the alarm
    LaunchedEffect(alarm) {
        if (savedStateHandle?.get<TempAlarmState>("current_alarm_state") == null) {
            hour = alarm.timeHour
            minute = alarm.timeMinute
            name = alarm.name
            repeatDays = alarm.repeatDays
            ringtone = alarm.ringtone
            volume = alarm.volume
            vibrate = alarm.vibrate
        }
    }
    
    // Observe ringtone selection from RingtoneSelectionScreen
    LaunchedEffect(Unit) {
        savedStateHandle?.get<String>("selected_ringtone")?.let { selectedRingtone ->
            ringtone = selectedRingtone
            viewModel.updateRingtone(selectedRingtone)
            // Clear the saved state to avoid reusing it if we come back to this screen
            savedStateHandle.remove<String>("selected_ringtone")
        }
    }
    
    var showTimePicker by remember { mutableStateOf(false) }
    val isNewAlarm = alarmId == -1L
    
    // Save current state before navigating away
    fun saveCurrentStateBeforeNavigation() {
        val tempState = TempAlarmState(
            hour = hour,
            minute = minute,
            name = name,
            repeatDays = repeatDays,
            ringtone = ringtone,
            volume = volume,
            vibrate = vibrate
        )
        savedStateHandle?.set("current_alarm_state", tempState)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewAlarm) "Add Alarm" else "Edit Alarm") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNewAlarm) {
                        IconButton(onClick = {
                            viewModel.deleteAlarm()
                            onBackClick()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Alarm")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Button(
                    onClick = {
                        // Update all values in viewModel
                        viewModel.updateTime(hour, minute)
                        viewModel.updateName(name)
                        viewModel.updateRingtone(ringtone)
                        viewModel.updateVolume(volume)
                        if (vibrate != alarm.vibrate) {
                            viewModel.toggleVibration()
                        }
                        // Update repeat days by toggling days as needed
                        val currentDays = alarm.repeatDays
                        repeatDays.forEach { day ->
                            if (!currentDays.contains(day)) {
                                viewModel.toggleDayOfWeek(day)
                            }
                        }
                        currentDays.forEach { day ->
                            if (!repeatDays.contains(day)) {
                                viewModel.toggleDayOfWeek(day)
                            }
                        }
                        // Save the alarm
                        viewModel.saveAlarm {
                            onSaveClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Save Alarm")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Time selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = TimeUtils.formatTime12Hour(hour, minute),
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Alarm name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Repeat days
            Text(
                text = "Repeat",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            DaysOfWeekSelector(
                selectedDays = repeatDays,
                onSelectionChanged = { repeatDays = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Ringtone selection
            ListItem(
                headlineContent = { Text("Ringtone") },
                supportingContent = { Text(ringtone) },
                leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                modifier = Modifier.clickable { 
                    saveCurrentStateBeforeNavigation()
                    onRingtoneClick() 
                }
            )
            
            Divider()
            
            // Volume slider
            ListItem(
                headlineContent = { Text("Volume") },
                leadingContent = { Icon(Icons.Default.VolumeUp, contentDescription = null) },
                trailingContent = {
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        modifier = Modifier.width(200.dp)
                    )
                }
            )
            
            Divider()
            
            // Vibration toggle
            ListItem(
                headlineContent = { Text("Vibrate") },
                leadingContent = { Icon(Icons.Default.Vibration, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it }
                    )
                }
            )
        }
    }
    
    // Time picker dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { selectedHour, selectedMinute ->
                hour = selectedHour
                minute = selectedMinute
                showTimePicker = false
            }
        )
    }
}

@Composable
fun DaysOfWeekSelector(
    selectedDays: List<DayOfWeek>,
    onSelectionChanged: (List<DayOfWeek>) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DayOfWeek.values().forEach { day ->
            val isSelected = selectedDays.contains(day)
            DaySelectionChip(
                day = day,
                isSelected = isSelected,
                onClick = {
                    val newSelection = if (isSelected) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                    onSelectionChanged(newSelection)
                }
            )
        }
    }
}

@Composable
fun DaySelectionChip(
    day: DayOfWeek,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Map our custom DayOfWeek to a label
    val dayText = when(day) {
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
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayText,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set alarm time") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Simple time picker UI
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker
                    NumberPicker(
                        value = hour,
                        onValueChange = { hour = it },
                        range = 0..23
                    )
                    
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    // Minute picker
                    NumberPicker(
                        value = minute,
                        onValueChange = { minute = it },
                        range = 0..59
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(hour, minute) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = {
                val newValue = if (value + 1 > range.last) range.first else value + 1
                onValueChange(newValue)
            }
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
        }
        
        Text(
            text = String.format("%02d", value),
            style = MaterialTheme.typography.headlineLarge
        )
        
        IconButton(
            onClick = {
                val newValue = if (value - 1 < range.first) range.last else value - 1
                onValueChange(newValue)
            }
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
        }
    }
} 