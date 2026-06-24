package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AudioSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, EqService::class.java)
        serviceIntent.action = intent?.action
        serviceIntent.putExtra("sessionId", intent?.getIntExtra("android.media.extra.AUDIO_SESSION", -1) ?: -1)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context?.startForegroundService(serviceIntent)
        } else {
            context?.startService(serviceIntent)
        }
    }
}
