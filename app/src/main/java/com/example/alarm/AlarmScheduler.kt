package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.TimetableItem
import com.example.receiver.StudyAlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules an exact alarm for a study session based on its day of the week and start time.
     * Uses setAlarmClock for high reliability and lockscreen alarm clock visibility.
     */
    fun scheduleStudyAlarm(item: TimetableItem) {
        if (!item.alarmEnabled) {
            cancelStudyAlarm(item)
            return
        }

        val triggerEpochMillis = calculateNextOccurrence(item.dayOfWeek, item.startTime)
        val requestCode = getUniqueRequestCode(item.id)

        val intent = Intent(context, StudyAlarmReceiver::class.java).apply {
            action = StudyAlarmReceiver.ACTION_STUDY_ALARM
            putExtra(StudyAlarmReceiver.EXTRA_SESSION_ID, item.id)
            putExtra(StudyAlarmReceiver.EXTRA_SUBJECT, item.subject)
            putExtra(StudyAlarmReceiver.EXTRA_TOPIC, item.topic)
            putExtra(StudyAlarmReceiver.EXTRA_TIME_RANGE, item.getFormattedTimeRange())
            putExtra(StudyAlarmReceiver.EXTRA_ROOM, item.roomOrLocation)
            putExtra(StudyAlarmReceiver.EXTRA_DAY, item.dayOfWeek)
            putExtra(StudyAlarmReceiver.EXTRA_START_TIME, item.startTime)
            putExtra(StudyAlarmReceiver.EXTRA_RINGTONE_NAME, item.ringtoneName)
            putExtra(StudyAlarmReceiver.EXTRA_RINGTONE_TYPE, item.ringtoneType)
            putExtra(StudyAlarmReceiver.EXTRA_CUSTOM_RINGTONE_URI, item.customRingtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    setExactAlarm(triggerEpochMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                }
            } else {
                setExactAlarm(triggerEpochMillis, pendingIntent)
            }
            Log.d("AlarmScheduler", "Alarm scheduled for ${item.subject} on ${item.dayOfWeek} at ${item.startTime} ($triggerEpochMillis)")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Security exception scheduling alarm: ${e.message}")
        }
    }

    private fun setExactAlarm(triggerMillis: Long, pendingIntent: PendingIntent) {
        val showIntent = Intent(context, com.example.MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    /**
     * Cancels an existing scheduled alarm for a timetable item.
     */
    fun cancelStudyAlarm(item: TimetableItem) {
        val requestCode = getUniqueRequestCode(item.id)
        val intent = Intent(context, StudyAlarmReceiver::class.java).apply {
            action = StudyAlarmReceiver.ACTION_STUDY_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Cancelled alarm for ${item.subject}")
        }
    }

    /**
     * Triggers a fast 3-second test alarm to demonstrate the loud alarm, custom ringtone, and notification.
     */
    fun scheduleTestAlarm(
        subject: String = "Physics Test Session",
        topic: String = "Quantum Mechanics derivations",
        ringtoneName: String = "Morning Chimes",
        ringtoneType: String = "chimes",
        customRingtoneUri: String? = null
    ) {
        val triggerTime = System.currentTimeMillis() + 3000L // in 3 seconds
        val requestCode = 99999

        val intent = Intent(context, StudyAlarmReceiver::class.java).apply {
            action = StudyAlarmReceiver.ACTION_STUDY_ALARM
            putExtra(StudyAlarmReceiver.EXTRA_SESSION_ID, "test_session")
            putExtra(StudyAlarmReceiver.EXTRA_SUBJECT, subject)
            putExtra(StudyAlarmReceiver.EXTRA_TOPIC, topic)
            putExtra(StudyAlarmReceiver.EXTRA_TIME_RANGE, "Right Now (Demo)")
            putExtra(StudyAlarmReceiver.EXTRA_ROOM, "Virtual Classroom")
            putExtra(StudyAlarmReceiver.EXTRA_DAY, "Today")
            putExtra(StudyAlarmReceiver.EXTRA_START_TIME, "Now")
            putExtra(StudyAlarmReceiver.EXTRA_RINGTONE_NAME, ringtoneName)
            putExtra(StudyAlarmReceiver.EXTRA_RINGTONE_TYPE, ringtoneType)
            putExtra(StudyAlarmReceiver.EXTRA_CUSTOM_RINGTONE_URI, customRingtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Failed to schedule test alarm: ${e.message}")
        }
    }

    /**
     * Checks if exact alarm permission is granted (Android 12+)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Computes the epoch timestamp for the next occurrence of dayOfWeek and startTime ("HH:mm").
     */
    fun calculateNextOccurrence(dayOfWeekStr: String, startTimeStr: String): Long {
        val targetDay = when (dayOfWeekStr.lowercase()) {
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            "sunday" -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        val parts = startTimeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        var daysDifference = targetDay - currentDay

        if (daysDifference < 0 || (daysDifference == 0 && calendar.timeInMillis <= System.currentTimeMillis())) {
            daysDifference += 7
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysDifference)
        return calendar.timeInMillis
    }

    private fun getUniqueRequestCode(id: String): Int {
        return Math.abs(id.hashCode()) % 100000
    }
}
