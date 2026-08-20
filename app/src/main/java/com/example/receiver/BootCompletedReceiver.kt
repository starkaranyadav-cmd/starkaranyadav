package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.AlarmScheduler
import com.example.data.local.StudyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            Log.d("BootCompletedReceiver", "Received $action. Rescheduling all active study alarms...")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = StudyDatabase.getDatabase(context)
                    val items = db.studyDao().getAllTimetableItems()
                    val scheduler = AlarmScheduler(context)
                    for (item in items) {
                        if (item.alarmEnabled) {
                            scheduler.scheduleStudyAlarm(item)
                        }
                    }
                    Log.d("BootCompletedReceiver", "Successfully rescheduled ${items.count { it.alarmEnabled }} alarms.")
                } catch (e: Exception) {
                    Log.e("BootCompletedReceiver", "Error rescheduling alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
