package com.teacherscompanion

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.teacherscompanion.R
import com.teacherscompanion.core.SyncManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class TeacherCompanionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        val entryPoint = EntryPointAccessors.fromApplication(this, SyncManagerEntryPoint::class.java)
        entryPoint.syncManager().start()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = Uri.parse("android.resource://${packageName}/${R.raw.teta}")

            val alarmChannel = NotificationChannel(
                "alarm_channel",
                "Alarms & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(alarmSound, null)
                enableVibration(true)
                description = "Lesson period reminders and alarms"
            }
            val syllabusChannel = NotificationChannel(
                "syllabus_alerts",
                "Syllabus Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(alarmSound, null)
                description = "Gap analysis and syllabus progress notifications"
            }
            val generalChannel = NotificationChannel(
                "general_notifications",
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                description = "General app notifications"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(listOf(alarmChannel, syllabusChannel, generalChannel))
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncManagerEntryPoint {
        fun syncManager(): SyncManager
    }
}
