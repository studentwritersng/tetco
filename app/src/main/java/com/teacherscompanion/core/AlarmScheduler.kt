package com.teacherscompanion.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.teacherscompanion.data.local.entity.AlarmEntity

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: AlarmEntity) {
        if (!alarm.isActive) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("label", alarm.label ?: "Alarm")
            putExtra("type", alarm.type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val days = alarm.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
        for (day in days) {
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_WEEK, day + 1)
                set(java.util.Calendar.HOUR_OF_DAY, alarm.timeHour)
                set(java.util.Calendar.MINUTE, alarm.timeMinute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (before(java.util.Calendar.getInstance())) {
                    add(java.util.Calendar.WEEK_OF_YEAR, 1)
                }
            }

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                pendingIntent
            )
        }
    }

    fun cancelAlarm(alarm: AlarmEntity) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAllAlarms() {
    }
}
