package com.teacherscompanion.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.teacherscompanion.MainActivity
import com.teacherscompanion.R
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TCFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var supabaseClient: SupabaseClient

    companion object {
        const val CHANNEL_ALARMS = "alarm_channel"
        const val CHANNEL_SYLLABUS = "syllabus_alerts"
        const val CHANNEL_GENERAL = "general_notifications"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = Uri.parse("android.resource://${packageName}/${R.raw.teta}")

            val alarmChannel = NotificationChannel(CHANNEL_ALARMS, "Alarms & Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(alarmSound, null)
                enableVibration(true)
                description = "Lesson period reminders and alarms"
            }
            val syllabusChannel = NotificationChannel(CHANNEL_SYLLABUS, "Syllabus Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(alarmSound, null)
                description = "Gap analysis and syllabus progress notifications"
            }
            val generalChannel = NotificationChannel(CHANNEL_GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                description = "General app notifications"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(listOf(alarmChannel, syllabusChannel, generalChannel))
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id ?: return@launch
                supabaseClient.from("profiles").update(mapOf("fcm_token" to token)) {
                    eq("id", userId)
                }
            } catch (_: Exception) { }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Teacher's Companion"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val channelId = message.data["channel"] ?: CHANNEL_GENERAL

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            message.data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmSound = Uri.parse("android.resource://${packageName}/${R.raw.teta}")

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
