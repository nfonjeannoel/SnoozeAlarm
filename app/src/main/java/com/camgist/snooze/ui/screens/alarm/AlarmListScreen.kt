package com.camgist.snooze.ui.screens.alarm

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel = koinViewModel(),
    onAlarmClick: (Long) -> Unit,
    onAddAlarmClick: () -> Unit
) {
    val alarms by viewModel.alarms.collectAsState()
    val scrollState = rememberLazyListState()
    
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Your Alarms",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlarmClick,
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Add Alarm",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (alarms.isEmpty()) {
                EmptyAlarmList()
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = alarms,
                        key = { it.id }
                    ) { alarm ->
                        AnimatedAlarmCard(
                            alarm = alarm,
                            onClick = { onAlarmClick(alarm.id) },
                            onToggleEnabled = { viewModel.toggleAlarmEnabled(alarm) }
                        )
                    }
                }
            }
            
            // Gradient scrim at the bottom for better FAB visibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated moon icon with pulsating effect
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Nightlight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp * scale)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        ),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            
            Text(
                text = "No alarms yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Add your first alarm to never miss an important moment!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {},
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Add Alarm")
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedAlarmCard(
    alarm: Alarm,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        onClick = { 
            isExpanded = !isExpanded
            onClick() 
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (alarm.isEnabled) 1f else 0.7f
            )
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (alarm.isEnabled) 4.dp else 1.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    thumbContent = {
                        if (alarm.isEnabled) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
            
            // Alarm time with shadow for better visibility
            Text(
                text = formatTime(alarm.timeHour, alarm.timeMinute),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = if (alarm.isEnabled) 
                    MaterialTheme.colorScheme.onSurfaceVariant 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            // Repeat days
            if (alarm.repeatDays.isNotEmpty()) {
                DaysOfWeekRow(alarm.repeatDays, alarm.isEnabled)
            }
            
            // Next occurrence text
            val nextOccurrence = formatNextOccurrence(alarm)
            Text(
                text = nextOccurrence,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    // Bedtime suggestion
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        
                        val bedtimeSuggestion = getBedtimeSuggestion(alarm.timeHour, alarm.timeMinute)
                        Text(
                            text = bedtimeSuggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DaysOfWeekRow(repeatDays: List<DayOfWeek>, isEnabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DayOfWeek.values().forEach { day ->
            val isSelected = repeatDays.contains(day)
            DayChip(
                day = day,
                isSelected = isSelected,
                isEnabled = isEnabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DayChip(
    day: DayOfWeek,
    isSelected: Boolean,
    isEnabled: Boolean,
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
    
    val backgroundColor = when {
        isSelected && isEnabled -> MaterialTheme.colorScheme.primary
        isSelected && !isEnabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val contentColor = when {
        isSelected && isEnabled -> MaterialTheme.colorScheme.onPrimary
        isSelected && !isEnabled -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.labelMedium
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