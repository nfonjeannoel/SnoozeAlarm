package com.camgist.snooze.ui.screens.alarm

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

data class RingtoneInfo(
    val title: String,
    val uri: Uri
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneSelectionScreen(
    onBackClick: () -> Unit,
    onRingtoneSelected: (String) -> Unit,
    initialSelectedRingtone: String = "Default"
) {
    val context = LocalContext.current
    
    // Load system ringtones
    val ringtones = remember {
        loadSystemRingtones(context)
    }
    
    var selectedRingtone by remember { mutableStateOf(initialSelectedRingtone) }
    var playingRingtone by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    // Stop playing when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Ringtone") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Button(
                    onClick = { onRingtoneSelected(selectedRingtone) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Select")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(ringtones) { ringtone ->
                RingtoneItem(
                    name = ringtone.title,
                    isSelected = ringtone.uri.toString() == selectedRingtone,
                    isPlaying = ringtone.uri.toString() == playingRingtone,
                    onSelect = { 
                        selectedRingtone = ringtone.uri.toString()
                    },
                    onPlay = {
                        if (playingRingtone == ringtone.uri.toString()) {
                            // Stop playing
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                            playingRingtone = null
                        } else {
                            // Stop current if any
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            
                            // Play this ringtone
                            playRingtone(context, ringtone.uri) { player ->
                                mediaPlayer = player
                                playingRingtone = ringtone.uri.toString()
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun loadSystemRingtones(context: Context): List<RingtoneInfo> {
    val ringtoneList = mutableListOf<RingtoneInfo>()
    
    // Add default option
    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    ringtoneList.add(RingtoneInfo("Default Alarm", defaultUri))
    
    try {
        // Get all alarm sounds
        val manager = RingtoneManager(context)
        manager.setType(RingtoneManager.TYPE_ALARM)
        val cursor = manager.cursor
        
        // If no alarm sounds found, try to get notification sounds
        if (cursor.count == 0) {
            manager.setType(RingtoneManager.TYPE_NOTIFICATION)
            cursor.requery()
        }
        
        // If still no sounds, try to get ringtones
        if (cursor.count == 0) {
            manager.setType(RingtoneManager.TYPE_RINGTONE)
            cursor.requery()
        }
        
        // Extract ringtones
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val ringtoneUri = manager.getRingtoneUri(cursor.position)
            
            // Skip duplicates (by URI)
            if (ringtoneList.none { it.uri.toString() == ringtoneUri.toString() }) {
                ringtoneList.add(RingtoneInfo(title, ringtoneUri))
            }
        }
    } catch (e: Exception) {
        // In case of any error, ensure we have at least the default
        if (ringtoneList.isEmpty()) {
            ringtoneList.add(RingtoneInfo("Default", defaultUri))
        }
    }
    
    return ringtoneList
}

private fun playRingtone(context: Context, uri: Uri, onPrepared: (MediaPlayer) -> Unit) {
    try {
        val player = MediaPlayer()
        player.setDataSource(context, uri)
        player.setOnPreparedListener { mp ->
            mp.start()
            onPrepared(player)
        }
        player.setOnCompletionListener { mp ->
            mp.start() // Loop manually
        }
        player.prepareAsync()
    } catch (e: Exception) {
        // Handle error
    }
}

@Composable
fun RingtoneItem(
    name: String,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Surface(
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon and name
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.width(16.dp))
                
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // Play button and selection indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play button
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Selection checkmark
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
} 