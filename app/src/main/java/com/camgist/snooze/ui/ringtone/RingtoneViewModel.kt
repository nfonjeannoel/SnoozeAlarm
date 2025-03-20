package com.camgist.snooze.ui.ringtone

import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RingtoneViewModel(
    currentRingtone: String
) : ViewModel() {
    
    private val _selectedRingtone = MutableStateFlow(currentRingtone)
    val selectedRingtone: StateFlow<String> = _selectedRingtone.asStateFlow()
    
    private var currentlyPlayingRingtone: Ringtone? = null
    
    fun selectRingtone(ringtoneUri: String) {
        stopCurrentRingtone()
        _selectedRingtone.update { ringtoneUri }
    }
    
    fun playRingtonePreview(ringtoneUri: String, context: android.content.Context) {
        stopCurrentRingtone()
        
        if (ringtoneUri == "Silent") {
            // Don't play anything for silent option
            return
        }
        
        try {
            val uri = Uri.parse(ringtoneUri)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone.play()
            currentlyPlayingRingtone = ringtone
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    fun stopCurrentRingtone() {
        currentlyPlayingRingtone?.stop()
        currentlyPlayingRingtone = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopCurrentRingtone()
    }
} 