package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmSoundManager
import com.example.data.local.StudyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_ALARM) {
            AlarmSoundManager.stopAll()
            val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: ""
            if (sessionId.isNotEmpty()) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationId = Math.abs(sessionId.hashCode()).coerceAtLeast(1)
                notificationManager.cancel(notificationId)
            }
            return
        }

        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: ""
        val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "Study Session"
        val topic = intent.getStringExtra(EXTRA_TOPIC) ?: "Time to focus on your studies!"
        val timeRange = intent.getStringExtra(EXTRA_TIME_RANGE) ?: ""
        val room = intent.getStringExtra(EXTRA_ROOM) ?: "Study Area"
        val day = intent.getStringExtra(EXTRA_DAY) ?: ""
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
        val ringtoneName = intent.getStringExtra(EXTRA_RINGTONE_NAME) ?: "Morning Chimes"
        val ringtoneType = intent.getStringExtra(EXTRA_RINGTONE_TYPE) ?: "chimes"
        val customRingtoneUri = intent.getStringExtra(EXTRA_CUSTOM_RINGTONE_URI)

        // 1. Play Alarm Vibration
        triggerVibration(context)

        // 2. Play Alarm Musical Tone / Ringtone
        AlarmSoundManager.playAlarmSound(context, ringtoneType, customRingtoneUri)

        // 3. Show Push Notification
        showStudyNotification(context, sessionId, subject, topic, timeRange, room, ringtoneName)

        // 4. Reschedule for next week if it's a recurring session
        if (sessionId.isNotEmpty() && sessionId != "test_session" && day.isNotEmpty() && startTime.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = StudyDatabase.getDatabase(context)
                val item = db.studyDao().getTimetableItemById(sessionId)
                if (item != null && item.alarmEnabled) {
                    val scheduler = AlarmScheduler(context)
                    scheduler.scheduleStudyAlarm(item)
                }
            }
        }
    }

    private fun triggerVibration(context: Context) {
        try {
            val pattern = longArrayOf(0, 500, 200, 500, 200, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showStudyNotification(
        context: Context,
        sessionId: String,
        subject: String,
        topic: String,
        timeRange: String,
        room: String,
        ringtoneName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_HIGHLIGHT_SESSION_ID", sessionId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopAlarmIntent = Intent(context, StudyAlarmReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.hashCode() + 1000,
            stopAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val contentText = if (topic.isNotBlank()) "$topic • $timeRange ($room)" else "$timeRange • $room"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🔔 Study Alarm: $subject")
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("📖 Session: $subject\n📝 Topic: $topic\n⏰ Scheduled: $timeRange\n📍 Location: $room\n🎶 Tune: $ringtoneName\n\nStay focused and achieve your daily study goals!")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .addAction(
                android.R.drawable.ic_media_play,
                "Start Studying",
                pendingIntent
            )
            .addAction(
                android.R.drawable.ic_delete,
                "Stop Alarm",
                stopPendingIntent
            )
            .build()

        val notificationId = Math.abs(sessionId.hashCode()).coerceAtLeast(1)
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "study_timetable_alarms"
        const val ACTION_STUDY_ALARM = "com.example.ACTION_STUDY_ALARM"
        const val ACTION_STOP_ALARM = "com.example.ACTION_STOP_ALARM"

        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_SUBJECT = "EXTRA_SUBJECT"
        const val EXTRA_TOPIC = "EXTRA_TOPIC"
        const val EXTRA_TIME_RANGE = "EXTRA_TIME_RANGE"
        const val EXTRA_ROOM = "EXTRA_ROOM"
        const val EXTRA_DAY = "EXTRA_DAY"
        const val EXTRA_START_TIME = "EXTRA_START_TIME"
        const val EXTRA_RINGTONE_NAME = "EXTRA_RINGTONE_NAME"
        const val EXTRA_RINGTONE_TYPE = "EXTRA_RINGTONE_TYPE"
        const val EXTRA_CUSTOM_RINGTONE_URI = "EXTRA_CUSTOM_RINGTONE_URI"
    }
}
