package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MainApplication
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EqService : Service() {

    private var globalDynamicsProcessing: DynamicsProcessing? = null
    private val audioSessions = mutableMapOf<Int, DynamicsProcessing>()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private val audioSessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            val sessionId = intent?.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1) ?: -1
            if (sessionId < 0) return

            when (action) {
                "android.media.action.OPEN_AUDIO_EFFECT_CONTROL" -> {
                    setupDynamicsProcessingForSession(sessionId)
                }
                "android.media.action.CLOSE_AUDIO_EFFECT_CONTROL" -> {
                    audioSessions[sessionId]?.release()
                    audioSessions.remove(sessionId)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupGlobalDynamicsProcessing()
        registerAudioSessionReceiver()
        observeEqChanges()
    }

    private fun registerAudioSessionReceiver() {
        val filter = IntentFilter().apply {
            addAction("android.media.action.OPEN_AUDIO_EFFECT_CONTROL")
            addAction("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioSessionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(audioSessionReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "android.media.action.OPEN_AUDIO_EFFECT_CONTROL") {
            val sessionId = intent.getIntExtra("sessionId", -1)
            if (sessionId >= 0) {
                setupDynamicsProcessingForSession(sessionId)
            }
        } else if (action == "android.media.action.CLOSE_AUDIO_EFFECT_CONTROL") {
            val sessionId = intent.getIntExtra("sessionId", -1)
            if (sessionId >= 0) {
                audioSessions[sessionId]?.release()
                audioSessions.remove(sessionId)
            }
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, "EQ_CHANNEL_ID")
            .setContentTitle("Equalizer Active")
            .setContentText("Sound processing running in background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1, 
                notification, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK 
                else 0
            )
        } else {
            startForeground(1, notification)
        }

        return START_STICKY
    }

    private fun setupGlobalDynamicsProcessing() {
        try {
            globalDynamicsProcessing?.release()
            globalDynamicsProcessing = null

            val configBuilder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2, true, 30, false, 0, false, 0, false
            )
            
            val repository = (application as MainApplication).repository
            val freqs = repository.currentFrequencies.value
            val levels = repository.currentLevels.value

            val eq = DynamicsProcessing.Eq(true, true, 30)
            for (i in 0 until 30) {
                val band = DynamicsProcessing.EqBand(true, freqs.getOrElse(i) { 1000f }, levels.getOrElse(i) { 0f })
                eq.setBand(i, band)
            }
            configBuilder.setPreEqAllChannelsTo(eq)
            
            globalDynamicsProcessing = DynamicsProcessing(0, 0, configBuilder.build())
            globalDynamicsProcessing?.enabled = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupDynamicsProcessingForSession(sessionId: Int) {
        try {
            audioSessions[sessionId]?.release()
            
            val configBuilder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2, true, 30, false, 0, false, 0, false
            )
            
            val repository = (application as MainApplication).repository
            val freqs = repository.currentFrequencies.value
            val levels = repository.currentLevels.value
            
            val eq = DynamicsProcessing.Eq(true, true, 30)
            for (i in 0 until 30) {
                val band = DynamicsProcessing.EqBand(true, freqs.getOrElse(i) { 1000f }, levels.getOrElse(i) { 0f })
                eq.setBand(i, band)
            }
            configBuilder.setPreEqAllChannelsTo(eq)
            
            val dp = DynamicsProcessing(0, sessionId, configBuilder.build())
            dp.enabled = true
            audioSessions[sessionId] = dp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeEqChanges() {
        val repository = (application as MainApplication).repository
        serviceScope.launch {
            repository.currentLevels.collect { levels ->
                applyEqLevels(levels, repository.currentFrequencies.value)
            }
        }
        serviceScope.launch {
            repository.currentFrequencies.collect { freqs ->
                applyEqLevels(repository.currentLevels.value, freqs)
            }
        }
    }

    private fun applyEqLevels(levels: List<Float>, freqs: List<Float>) {
        if (globalDynamicsProcessing == null || globalDynamicsProcessing?.hasControl() == false) {
            setupGlobalDynamicsProcessing()
        }

        val allDp = mutableListOf<DynamicsProcessing>()
        globalDynamicsProcessing?.let { allDp.add(it) }
        allDp.addAll(audioSessions.values)

        for (dp in allDp) {
            try {
                if (!dp.hasControl()) continue
                for (channel in 0 until 2) {
                    val eq = dp.getPreEqByChannelIndex(channel)
                    if (eq != null) {
                        for (i in 0 until 30) {
                            if (i < levels.size && i < freqs.size) {
                                val band = eq.getBand(i) ?: DynamicsProcessing.EqBand(true, freqs[i], levels[i])
                                band.gain = levels[i]
                                // Handle potential issue if frequency isn't valid for the band
                                try {
                                    band.cutoffFrequency = freqs[i]
                                } catch (e: Exception) {}
                                eq.setBand(i, band)
                            }
                        }
                        dp.setPreEqByChannelIndex(channel, eq)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // If it fails, maybe it lost control or frequencies were invalid
                if (dp == globalDynamicsProcessing) {
                    setupGlobalDynamicsProcessing()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterReceiver(audioSessionReceiver)
        globalDynamicsProcessing?.release()
        audioSessions.values.forEach { it.release() }
        audioSessions.clear()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "EQ_CHANNEL_ID",
                "Equalizer Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
