package com.example.aioapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AIOApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Pomodoro Timer"
        val descriptionText = "Notifications for Pomodoro timer"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(POMODORO_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val POMODORO_CHANNEL_ID = "pomodoro_channel"
    }
}
