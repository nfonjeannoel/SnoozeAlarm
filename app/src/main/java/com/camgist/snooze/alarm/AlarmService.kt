package com.camgist.snooze.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.camgist.snooze.MainActivity
import com.camgist.snooze.R
import com.camgist.snooze.domain.AlarmScheduler
import com.camgist.snooze.domain.usecase.GetAlarmByIdUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlarmService : Service(), KoinComponent {
    private val getAlarmByIdUseCase: GetAlarmByIdUseCase by inject()
    private val alarmScheduler: AlarmScheduler by inject()
    
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val vibrator by lazy { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarmId: Long = -1
    private var targetVolume: Float = 0.5f
    private var currentVolume: Float = 0.1f
    // Audio focus request for O+ devices
    private var audioFocusRequest: AudioFocusRequest? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val volumeHandler = Handler(Looper.getMainLooper())
    private val volumeRunnable = object : Runnable {
        override fun run() {
            if (currentVolume < targetVolume) {
                currentVolume = minOf(currentVolume + 0.01f, targetVolume)
                mediaPlayer?.setVolume(currentVolume, currentVolume)
                volumeHandler.postDelayed(this, 100)
            }
        }
    }
    
    private val CHANNEL_ID = "alarm_channel"
    private val NOTIFICATION_ID = 1
    
    companion object {
        private const val TAG = "AlarmService"
        
        const val ACTION_SNOOZE = "com.camgist.snooze.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.camgist.snooze.ACTION_DISMISS"
        const val EXTRA_SNOOZE_MINUTES = "EXTRA_SNOOZE_MINUTES"
        
        // Default snooze time in minutes
        const val DEFAULT_SNOOZE_TIME = 5
        
        // Using same constant as AndroidAlarmScheduler
        const val ALARM_ID = "ALARM_ID"
        
        // Wake lock timeout - 10 minutes, in milliseconds
        private const val WAKE_LOCK_TIMEOUT = 10 * 60 * 1000L 
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlarmService created")
        createNotificationChannel()
        
        // Create wake lock to ensure device stays awake
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Snoozeloo:AlarmWakeLock"
        ).apply {
            setReferenceCounted(false)  // Not reference counted
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        // Acquire wake lock if not already held
        acquireWakeLock()
        
        // Check if this is a dismiss or snooze action
        when (intent?.action) {
            ACTION_DISMISS -> {
                Log.d(TAG, "Dismissing alarm")
                // Explicitly stop the ringtone and vibration
                stopRingtone()
                stopVibration()
                stopSelf()
                return Service.START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_TIME)
                Log.d(TAG, "Snoozing alarm for $snoozeMinutes minutes")
                snoozeCurrentAlarm(snoozeMinutes)
                return Service.START_NOT_STICKY
            }
        }
        
        val alarmId = intent?.getLongExtra(ALARM_ID, -1L) ?: -1L
        if (alarmId != -1L) {
            currentAlarmId = alarmId
            
            // Start as foreground service with notification
            val notification = createNotification(alarmId)
            startForeground(NOTIFICATION_ID, notification.build())
            
            // Load alarm details and play sound/vibration
            serviceScope.launch {
                try {
                    val alarm = getAlarmByIdUseCase(alarmId)
                    if (alarm != null) {
                        Log.d(TAG, "Starting alarm: $alarm")
                        
                        // Start alarm fullscreen activity
                        val fullScreenIntent = Intent(this@AlarmService, MainActivity::class.java).apply {
                            putExtra(ALARM_ID, alarmId)
                            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(fullScreenIntent)
                        
                        // Play ringtone with the alarm's volume setting
                        playRingtone(alarm.ringtone, alarm.volume)
                        
                        // Vibrate if enabled
                        if (alarm.vibrate) {
                            startVibration()
                        }
                    } else {
                        Log.e(TAG, "Alarm not found: $alarmId")
                        stopSelf()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling alarm: ${e.message}", e)
                    // Try to get default alarm tone if there was an error
                    playRingtone("Default", 0.5f)
                    stopSelf()
                }
            }
        } else {
            Log.e(TAG, "Invalid alarm ID")
            stopSelf()
        }
        
        return Service.START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "AlarmService destroyed")
        
        stopRingtone()
        stopVibration()
        
        // Release wake lock
        releaseWakeLock()
        
        // Cancel any pending tasks
        volumeHandler.removeCallbacksAndMessages(null)
        
        // Cancel the coroutine scope
        serviceScope.cancel()
        
        super.onDestroy()
    }
    
    private fun snoozeCurrentAlarm(minutes: Int) {
        if (currentAlarmId != -1L) {
            serviceScope.launch {
                try {
                    val alarm = getAlarmByIdUseCase(currentAlarmId)
                    if (alarm != null) {
                        alarmScheduler.snoozeAlarm(alarm, minutes)
                        Log.d(TAG, "Alarm snoozed for $minutes minutes")
                    } else {
                        Log.e(TAG, "Cannot snooze: alarm $currentAlarmId not found")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error snoozing alarm: ${e.message}", e)
                }
            }
        }
        
        // Make sure to stop ringtone and vibration before stopping the service
        stopRingtone()
        stopVibration()
        
        stopSelf()
    }
    
    private fun acquireWakeLock() {
        wakeLock?.let {
            if (!it.isHeld) {
                Log.d(TAG, "Acquiring wake lock")
                it.acquire(WAKE_LOCK_TIMEOUT)
            }
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                Log.d(TAG, "Releasing wake lock")
                it.release()
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for triggered alarms"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(alarmId: Long): NotificationCompat.Builder {
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_TIME)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this, 1, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarm")
            .setContentText("Your alarm is ringing!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher_foreground, "Dismiss", dismissPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Snooze 5 min", snoozePendingIntent)
    }
    
    private fun playRingtone(ringtonePath: String, volume: Float) {
        stopRingtone()
        
        // Save target volume and start low
        targetVolume = volume
        currentVolume = 0.1f
        
        Log.d(TAG, "Playing ringtone: $ringtonePath")
        
        val ringtoneUri = try {
            if (ringtonePath == "Default" || ringtonePath.isEmpty()) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            } else {
                // Map the ringtone name to actual system ringtones for demonstration
                // In a real app, you would store actual URIs instead of just names
                when (ringtonePath) {
                    "Gentle Rise", "Morning Mist", "Ocean Waves", 
                    "Chirping Birds", "Digital Alarm", "Soft Bells", 
                    "Nature Sounds", "Piano Melody", "Guitar Acoustic" -> {
                        // For demo purposes, we'll still use the default alarm sound
                        // but in a real app, these would be different sound files
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    }
                    else -> {
                        // Try to parse it as a URI in case it's a full URI path
                        try {
                            Uri.parse(ringtonePath)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse ringtone URI: $ringtonePath", e)
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ringtone URI, using default", e)
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        
        try {
            // Request audio focus
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()
                
                // We know audioFocusRequest is not null at this point
                audioManager.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
            
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmService, ringtoneUri)
                    setVolume(currentVolume, currentVolume)
                    isLooping = true
                    prepare()
                    start()
                }
                
                // Start volume ramp up
                volumeHandler.post(volumeRunnable)
            } else {
                Log.e(TAG, "Failed to get audio focus")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing ringtone: ${e.message}", e)
            
            // If custom ringtone fails, try default
            if (ringtonePath != "Default" && ringtonePath.isNotEmpty()) {
                playRingtone("Default", volume)
            }
        }
    }
    
    private fun stopRingtone() {
        // Stop volume ramp up
        volumeHandler.removeCallbacks(volumeRunnable)
        
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ringtone: ${e.message}", e)
        }
        
        // Abandon audio focus
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus: ${e.message}", e)
        }
    }
    
    private fun startVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 500, 500, 500, 500, 500),
                        0
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 500, 500, 500, 500, 500), 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration: ${e.message}", e)
        }
    }
    
    private fun stopVibration() {
        try {
            vibrator.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration: ${e.message}", e)
        }
    }
} 