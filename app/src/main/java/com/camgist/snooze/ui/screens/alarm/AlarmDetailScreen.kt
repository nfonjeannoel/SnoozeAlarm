package com.camgist.snooze.ui.screens.alarm

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
            LargeTopAppBar(
                title = { 
                    Text(
                        if (isNewAlarm) "Add Alarm" else "Edit Alarm",
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    if (!isNewAlarm) {
                        IconButton(onClick = {
                            viewModel.deleteAlarm()
                            onBackClick()
                        }) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = "Delete Alarm",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.error
                ),
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            ) {
                ElevatedButton(
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
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        "Save Alarm", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Time selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = MaterialTheme.shapes.large,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    .clip(MaterialTheme.shapes.large)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        showTimePicker = true 
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated shadow effect
                    val infiniteTransition = rememberInfiniteTransition(label = "glow")
                    val shadowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow"
                    )
                    
                    Text(
                        text = TimeUtils.formatTime12Hour(hour, minute),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Name field with enhanced appearance
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { 
                    Text(
                        "Alarm name (optional)",
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = MaterialTheme.shapes.medium
                    )
                    .clip(MaterialTheme.shapes.medium),
                leadingIcon = { 
                    Icon(
                        Icons.Default.Label, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    ) 
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
            
            // Section divider
            SectionTitle(title = "Repeat", icon = Icons.Rounded.Repeat)
            
            // Repeat days selector with enhanced styling
            DaysOfWeekSelector(
                selectedDays = repeatDays,
                onSelectionChanged = { repeatDays = it }
            )
            
            // Section divider
            SectionTitle(title = "Sound", icon = Icons.Rounded.MusicNote)
            
            // Settings cards
            SettingsCard(
                title = "Ringtone",
                subtitle = ringtone,
                leadingIcon = Icons.Rounded.LibraryMusic,
                onClick = { 
                    saveCurrentStateBeforeNavigation()
                    onRingtoneClick() 
                }
            )
            
            SettingsCard(
                title = "Volume",
                leadingIcon = Icons.Rounded.VolumeUp,
                trailingContent = {
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        modifier = Modifier.width(180.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            )
            
            SettingsCard(
                title = "Vibrate",
                leadingIcon = Icons.Rounded.Vibration,
                trailingContent = {
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        thumbContent = {
                            if (vibrate) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }
            )
            
            // Add some bottom padding for better UX with the floating button
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    
    // Time picker dialog
    if (showTimePicker) {
        EnhancedTimePickerDialog(
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
fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Divider(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Leading icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Trailing content (if provided)
            if (trailingContent != null) {
                trailingContent()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun DaysOfWeekSelector(
    selectedDays: List<DayOfWeek>,
    onSelectionChanged: (List<DayOfWeek>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
    
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "color"
    )
    
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.onPrimary 
        else 
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "textColor"
    )
    
    val animatedSize by animateDpAsState(
        targetValue = if (isSelected) 44.dp else 40.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "size"
    )
    
    Surface(
        shape = CircleShape,
        color = animatedColor,
        modifier = Modifier
            .size(animatedSize)
            .shadow(
                elevation = if (isSelected) 6.dp else 0.dp,
                shape = CircleShape,
                spotColor = if (isSelected) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else
                    Color.Transparent
            )
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayText,
                color = animatedTextColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Set Alarm Time",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {                
                // Digital clock display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = String.format("%02d", hour),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Text(
                                text = ":",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            
                            Text(
                                text = String.format("%02d", minute),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Time pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker
                    EnhancedNumberPicker(
                        value = hour,
                        onValueChange = { hour = it },
                        range = 0..23,
                        label = "Hour",
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Minute picker
                    EnhancedNumberPicker(
                        value = minute,
                        onValueChange = { minute = it },
                        range = 0..59,
                        label = "Minute",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(hour, minute) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Set Alarm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EnhancedNumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        val newValue = if (value + 1 > range.last) range.first else value + 1
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp, 
                        contentDescription = "Increase",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = String.format("%02d", value),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                IconButton(
                    onClick = {
                        val newValue = if (value - 1 < range.first) range.last else value - 1
                        onValueChange(newValue)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, 
                        contentDescription = "Decrease",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
} 