package com.example.data.model

object FirestoreSchemaInfo {

    const val FIRESTORE_SCHEMA_JSON = """
{
  "users": {
    "USER_UID_1": {
      "uid": "user_alex_01",
      "name": "Alex Rivera",
      "email": "alex.rivera@student.edu",
      "avatarId": 1,
      "targetGoal": "Score 99% in Engineering Entrance Exam",
      "dailyTargetHours": 5.0,
      "weeklyTargetHours": 30.0,
      "role": "user",
      "joinedDate": "August 2026",
      "totalStudyHoursLogged": 14.5,
      "currentStreakDays": 4,
      "lastUpdated": 1787140000000
    }
  },
  "timetables": {
    "SESSION_DOC_ID_1": {
      "id": "item_001",
      "dayOfWeek": "Monday",
      "subject": "Advanced Physics",
      "topic": "Quantum Mechanics: Wave Equations & Tunneling",
      "startTime": "09:00",
      "endTime": "10:30",
      "durationMinutes": 90,
      "roomOrLocation": "Study Room B-102",
      "notes": "Review Griffiths Chapter 2 & problem set 4",
      "colorHex": "#4338CA",
      "alarmEnabled": true,
      "alarmRequestCode": 1001,
      "createdAt": 1787140000000,
      "updatedAt": 1787140000000,
      "createdBy": "admin@studyapp.io"
    }
  },
  "study_progress_logs": {
    "LOG_DOC_ID_1": {
      "id": "log_001",
      "userId": "user_alex_01",
      "timetableItemId": "item_001",
      "subject": "Advanced Physics",
      "topic": "Quantum Mechanics",
      "dateString": "2026-08-19",
      "durationMinutes": 90,
      "targetMinutes": 90,
      "notes": "Completed wave equations derivation notes",
      "timestamp": 1787140000000,
      "isCompleted": true
    }
  }
}
"""

    const val ARCHITECTURE_OVERVIEW = """
1. Cloud Backend (Firebase Firestore / Realtime DB):
   - Collection 'timetables': Admin manages study schedule items with real-time snapshot listeners for all students.
   - Collection 'users': Holds student profiles, study goals, target study hours, and roles ('admin' vs 'user').
   - Collection 'study_progress_logs': Records completed study sessions, actual study minutes vs target minutes.

2. On-Device Persistence & Offline Cache (Room + Jetpack Compose):
   - Room Database: Caches timetables and logs locally for instant offline loading and alarm rescheduling.
   - Live synchronization via Firestore Snapshot Listeners with automatic fallback.

3. Alarms & Notifications System:
   - AlarmManager exact alarms with `setAlarmClock()` / `setExactAndAllowWhileIdle()`
   - BroadcastReceiver (`StudyAlarmReceiver`) catches triggered intents even when app is terminated.
   - High-priority NotificationChannel with loud alarm audio, vibration, and full-screen intent actions.
   - `BootCompletedReceiver` automatically reschedules alarms across device reboots.
"""
}
