package com.example.data

import android.content.Context
import android.util.Log
import com.example.alarm.AlarmScheduler
import com.example.data.local.StudyDao
import com.example.data.local.StudyDatabase
import com.example.data.model.StudyProgressLog
import com.example.data.model.TimetableItem
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class StudyRepository(
    private val context: Context,
    private val dao: StudyDao,
    private val alarmScheduler: AlarmScheduler
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null
    private var timetableListenerRegistration: ListenerRegistration? = null
    private var usersListenerRegistration: ListenerRegistration? = null

    private val _syncStatus = MutableStateFlow("Synced (Realtime Local & Cloud)")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _activeUserUid = MutableStateFlow("user_star_main_admin")
    val activeUserUid: StateFlow<String> = _activeUserUid.asStateFlow()

    init {
        initFirestore()
        coroutineScope.launch {
            seedInitialDataIfEmpty()
            startRealtimeSync()
        }
    }

    private fun initFirestore() {
        try {
            firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firebase Firestore initialized successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Firestore initialization notice (running in local-first hybrid mode): ${e.message}")
        }
    }

    // --- TIMETABLE OPERATIONS ---

    fun getAllTimetableItemsFlow(): Flow<List<TimetableItem>> = dao.getAllTimetableItemsFlow()

    fun getTimetableForDayFlow(day: String): Flow<List<TimetableItem>> = dao.getTimetableForDayFlow(day)

    suspend fun addTimetableItem(item: TimetableItem) {
        dao.insertTimetableItem(item)
        if (item.alarmEnabled) {
            alarmScheduler.scheduleStudyAlarm(item)
        }
        syncItemToFirestore(item)
    }

    suspend fun updateTimetableItem(item: TimetableItem) {
        dao.updateTimetableItem(item)
        if (item.alarmEnabled) {
            alarmScheduler.scheduleStudyAlarm(item)
        } else {
            alarmScheduler.cancelStudyAlarm(item)
        }
        syncItemToFirestore(item)
    }

    suspend fun deleteTimetableItem(item: TimetableItem) {
        alarmScheduler.cancelStudyAlarm(item)
        dao.deleteTimetableItem(item)
        deleteItemFromFirestore(item.id)
    }

    suspend fun toggleAlarm(item: TimetableItem, enabled: Boolean) {
        val updated = item.copy(alarmEnabled = enabled, updatedAt = System.currentTimeMillis())
        dao.updateTimetableItem(updated)
        if (enabled) {
            alarmScheduler.scheduleStudyAlarm(updated)
        } else {
            alarmScheduler.cancelStudyAlarm(updated)
        }
        syncItemToFirestore(updated)
    }

    private fun syncItemToFirestore(item: TimetableItem) {
        coroutineScope.launch {
            try {
                firestore?.collection("timetables")?.document(item.id)?.set(item)
                _syncStatus.value = "Live Synced (${getCurrentTimeString()})"
            } catch (e: Exception) {
                Log.w(TAG, "Firestore item sync deferred: ${e.message}")
            }
        }
    }

    private fun deleteItemFromFirestore(itemId: String) {
        coroutineScope.launch {
            try {
                firestore?.collection("timetables")?.document(itemId)?.delete()
                _syncStatus.value = "Live Synced (${getCurrentTimeString()})"
            } catch (e: Exception) {
                Log.w(TAG, "Firestore delete deferred: ${e.message}")
            }
        }
    }

    // --- USER PROFILE & ADMIN DIRECTORY OPERATIONS ---

    fun getUserProfileFlow(uid: String = _activeUserUid.value): Flow<UserProfile?> = dao.getUserProfileFlow(uid)

    fun getAllUsersFlow(): Flow<List<UserProfile>> = dao.getAllUsersFlow()

    suspend fun updateUserProfile(profile: UserProfile) {
        dao.insertUserProfile(profile)
        coroutineScope.launch {
            try {
                firestore?.collection("users")?.document(profile.uid)?.set(profile)
            } catch (e: Exception) {
                Log.w(TAG, "Firestore user profile sync deferred: ${e.message}")
            }
        }
    }

    fun setActiveUserUid(uid: String) {
        _activeUserUid.value = uid
    }

    // --- PROGRESS LOGS OPERATIONS ---

    fun getStudyLogsFlow(userId: String = _activeUserUid.value): Flow<List<StudyProgressLog>> =
        dao.getStudyLogsForUserFlow(userId)

    suspend fun logStudySession(
        subject: String,
        topic: String,
        durationMinutes: Int,
        targetMinutes: Int,
        notes: String = "",
        timetableId: String = ""
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val log = StudyProgressLog(
            id = UUID.randomUUID().toString(),
            userId = _activeUserUid.value,
            timetableItemId = timetableId,
            subject = subject,
            topic = topic,
            dateString = today,
            durationMinutes = durationMinutes,
            targetMinutes = targetMinutes,
            notes = notes,
            timestamp = System.currentTimeMillis(),
            isCompleted = true
        )
        dao.insertStudyLog(log)

        // Update user's total logged hours and streak
        val currentProfile = dao.getUserProfile(_activeUserUid.value)
        if (currentProfile != null) {
            val addedHours = durationMinutes / 60f
            val updated = currentProfile.copy(
                totalStudyHoursLogged = (currentProfile.totalStudyHoursLogged + addedHours * 10).toInt() / 10f,
                lastUpdated = System.currentTimeMillis()
            )
            dao.insertUserProfile(updated)
        }

        coroutineScope.launch {
            try {
                firestore?.collection("study_progress_logs")?.document(log.id)?.set(log)
            } catch (e: Exception) {
                Log.w(TAG, "Firestore log sync deferred: ${e.message}")
            }
        }
    }

    suspend fun deleteStudyLog(log: StudyProgressLog) {
        dao.deleteStudyLog(log)
        coroutineScope.launch {
            try {
                firestore?.collection("study_progress_logs")?.document(log.id)?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore delete log deferred: ${e.message}")
            }
        }
    }

    // --- REALTIME FIRESTORE LISTENER ---

    private fun startRealtimeSync() {
        try {
            val fs = firestore ?: return
            timetableListenerRegistration = fs.collection("timetables")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen error for timetables", error)
                        return@addSnapshotListener
                    }
                    if (snapshots != null && !snapshots.isEmpty) {
                        val items = snapshots.toObjects(TimetableItem::class.java)
                        coroutineScope.launch {
                            dao.insertTimetableItems(items)
                            // Schedule alarms for incoming active sessions
                            for (item in items) {
                                if (item.alarmEnabled) {
                                    alarmScheduler.scheduleStudyAlarm(item)
                                }
                            }
                            _syncStatus.value = "Live Synced with Admin Updates"
                        }
                    }
                }

            usersListenerRegistration = fs.collection("users")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshots != null && !snapshots.isEmpty) {
                        val users = snapshots.toObjects(UserProfile::class.java)
                        coroutineScope.launch {
                            dao.insertUserProfiles(users)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Realtime sync listener notice: ${e.message}")
        }
    }

    // --- INITIAL DATA SEEDING ---

    private suspend fun seedInitialDataIfEmpty() {
        val existingItems = dao.getAllTimetableItems()
        if (existingItems.isEmpty()) {
            val sampleItems = createDefaultTimetable()
            dao.insertTimetableItems(sampleItems)
            for (item in sampleItems) {
                if (item.alarmEnabled) {
                    alarmScheduler.scheduleStudyAlarm(item)
                }
            }
        }

        // Clean out previous placeholder users and establish Star as Main Admin
        dao.clearAllUserProfiles()

        val starAdmin = UserProfile(
            uid = "user_star_main_admin",
            name = "Star",
            email = "starkaranyadav@gmail.com",
            avatarId = 0,
            targetGoal = "Main Administrator & Master Study Coordinator",
            dailyTargetHours = 8.0f,
            weeklyTargetHours = 45.0f,
            role = "admin",
            joinedDate = "August 2026",
            totalStudyHoursLogged = 36.5f,
            currentStreakDays = 12
        )
        dao.insertUserProfile(starAdmin)
        updateUserProfile(starAdmin)

        // Seed initial study progress logs for Star
        val existingLogs = dao.getStudyLogsForUser("user_star_main_admin")
        if (existingLogs.isEmpty()) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val log1 = StudyProgressLog(
                id = "log_01",
                userId = "user_star_main_admin",
                subject = "Advanced Physics",
                topic = "Quantum Mechanics: Wave Equations & Superposition",
                dateString = today,
                durationMinutes = 90,
                targetMinutes = 90,
                notes = "Solved 6 numerical problems from Chapter 4",
                timestamp = System.currentTimeMillis() - 3600000L,
                isCompleted = true
            )
            val log2 = StudyProgressLog(
                id = "log_02",
                userId = "user_star_main_admin",
                subject = "Calculus III",
                topic = "Multivariable Integration & Green's Theorem",
                dateString = today,
                durationMinutes = 60,
                targetMinutes = 75,
                notes = "Practiced vector fields and flux surface integrals",
                timestamp = System.currentTimeMillis() - 7200000L,
                isCompleted = true
            )
            val log3 = StudyProgressLog(
                id = "log_03",
                userId = "user_star_main_admin",
                subject = "Data Structures & Algorithms",
                topic = "Graph Theory: Dijkstra & Shortest Path Trees",
                dateString = yesterday,
                durationMinutes = 120,
                targetMinutes = 120,
                notes = "Implemented priority queue Dijkstra in Kotlin",
                timestamp = System.currentTimeMillis() - 86400000L,
                isCompleted = true
            )
            dao.insertStudyLog(log1)
            dao.insertStudyLog(log2)
            dao.insertStudyLog(log3)
        }
    }

    private fun createDefaultTimetable(): List<TimetableItem> {
        return listOf(
            TimetableItem(
                id = "tt_01",
                dayOfWeek = "Monday",
                subject = "Advanced Physics",
                topic = "Quantum Mechanics & Wave Functions",
                startTime = "08:30",
                endTime = "10:00",
                durationMinutes = 90,
                roomOrLocation = "Hall B-102",
                notes = "Bring Griffiths textbook and formula notebook",
                colorHex = "#4338CA",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_02",
                dayOfWeek = "Monday",
                subject = "Calculus III",
                topic = "Vector Calculus & Stokes Theorem",
                startTime = "10:30",
                endTime = "12:00",
                durationMinutes = 90,
                roomOrLocation = "Math Annex 204",
                notes = "Review problem set #5 solutions",
                colorHex = "#3B82F6",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_03",
                dayOfWeek = "Monday",
                subject = "Computer Architecture",
                topic = "Pipelining, Hazard Resolution & Cache",
                startTime = "14:00",
                endTime = "16:00",
                durationMinutes = 120,
                roomOrLocation = "CS Lab 4",
                notes = "Complete Verilog memory model simulation",
                colorHex = "#0891B2",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_04",
                dayOfWeek = "Tuesday",
                subject = "Organic Chemistry",
                topic = "Reaction Mechanisms & Spectroscopy",
                startTime = "09:00",
                endTime = "11:00",
                durationMinutes = 120,
                roomOrLocation = "Chem Block C-12",
                notes = "NMR spectral analysis flashcards",
                colorHex = "#EC4899",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_05",
                dayOfWeek = "Tuesday",
                subject = "Data Structures",
                topic = "Dynamic Programming & Memoization",
                startTime = "13:30",
                endTime = "15:30",
                durationMinutes = 120,
                roomOrLocation = "Library Quiet Zone",
                notes = "Solve 4 LeetCode medium DP problems",
                colorHex = "#8B5CF6",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_06",
                dayOfWeek = "Wednesday",
                subject = "Advanced Physics",
                topic = "Electrodynamics & Maxwell Equations",
                startTime = "09:00",
                endTime = "11:00",
                durationMinutes = 120,
                roomOrLocation = "Hall B-102",
                notes = "Derive electromagnetic wave propagation speed in vacuum",
                colorHex = "#4338CA",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_07",
                dayOfWeek = "Wednesday",
                subject = "Linear Algebra",
                topic = "Eigenvalues, Eigenvectors & SVD",
                startTime = "14:00",
                endTime = "15:30",
                durationMinutes = 90,
                roomOrLocation = "Math Annex 204",
                notes = "Review diagonal matrices and matrix decomposition",
                colorHex = "#10B981",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_08",
                dayOfWeek = "Thursday",
                subject = "Operating Systems",
                topic = "Concurrency, Locks & Deadlocks",
                startTime = "10:00",
                endTime = "12:00",
                durationMinutes = 120,
                roomOrLocation = "CS Lab 2",
                notes = "Write mutex synchronization producer-consumer demo",
                colorHex = "#F59E0B",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_09",
                dayOfWeek = "Thursday",
                subject = "Calculus III",
                topic = "Line & Surface Integrals",
                startTime = "15:00",
                endTime = "17:00",
                durationMinutes = 120,
                roomOrLocation = "Study Room A-3",
                notes = "Mock quiz practice questions",
                colorHex = "#3B82F6",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_10",
                dayOfWeek = "Friday",
                subject = "Algorithms",
                topic = "Graph Algorithms: Shortest Paths & MST",
                startTime = "09:00",
                endTime = "11:30",
                durationMinutes = 150,
                roomOrLocation = "Auditorium 1",
                notes = "Kruskal and Prim algorithm proofs",
                colorHex = "#8B5CF6",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_11",
                dayOfWeek = "Saturday",
                subject = "Comprehensive Revision",
                topic = "Weekly Flashcards & Practice Test",
                startTime = "10:00",
                endTime = "13:00",
                durationMinutes = 180,
                roomOrLocation = "Home Desk",
                notes = "Full weekly progress review and self-test",
                colorHex = "#0891B2",
                alarmEnabled = true
            ),
            TimetableItem(
                id = "tt_12",
                dayOfWeek = "Sunday",
                subject = "Exam Strategy & Prep",
                topic = "Past Year Question Papers Analysis",
                startTime = "11:00",
                endTime = "13:00",
                durationMinutes = 120,
                roomOrLocation = "Home Desk",
                notes = "Plan upcoming week's focus areas",
                colorHex = "#10B981",
                alarmEnabled = true
            )
        )
    }

    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    companion object {
        private const val TAG = "StudyRepository"
    }
}
