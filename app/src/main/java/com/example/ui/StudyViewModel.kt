package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.StudyRepository
import com.example.data.model.StudyProgressLog
import com.example.data.model.TimetableItem
import com.example.data.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class StudyViewModel(
    private val repository: StudyRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    companion object {
        const val ADMIN_SECURITY_PIN = "9044"

        fun getTodayDayOfWeek(): String {
            val calendar = Calendar.getInstance()
            return when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                Calendar.SUNDAY -> "Sunday"
                else -> "Monday"
            }
        }
    }

    // Tab Navigation: 0 = Timetable, 1 = Progress Tracker, 2 = Admin Panel, 3 = Profile & Alarms
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Day Selection for Timetable
    private val _selectedDay = MutableStateFlow(getTodayDayOfWeek())
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    // Role switcher ("user" or "admin")
    private val _currentRole = MutableStateFlow("user")
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    // Admin Security PIN Lock state
    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _showPinDialog = MutableStateFlow(false)
    val showPinDialog: StateFlow<Boolean> = _showPinDialog.asStateFlow()

    // Active User Profile
    val userProfile: StateFlow<UserProfile?> = repository.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All Timetable Items
    val allTimetableItems: StateFlow<List<TimetableItem>> = repository.getAllTimetableItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Timetable Items for Selected Day
    val dayTimetableItems: StateFlow<List<TimetableItem>> = combine(
        allTimetableItems,
        _selectedDay
    ) { items, day ->
        items.filter { it.dayOfWeek.equals(day, ignoreCase = true) }
            .sortedBy { it.startTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Users for Admin Panel
    val allUsers: StateFlow<List<UserProfile>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study Progress Logs for Current User
    val studyLogs: StateFlow<List<StudyProgressLog>> = repository.getStudyLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sync Status
    val syncStatus: StateFlow<String> = repository.syncStatus

    // Active Study Stopwatch Session
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _activeTimerSubject = MutableStateFlow("Physics")
    val activeTimerSubject: StateFlow<String> = _activeTimerSubject.asStateFlow()

    private val _activeTimerTopic = MutableStateFlow("Derivations")
    val activeTimerTopic: StateFlow<String> = _activeTimerTopic.asStateFlow()

    private var timerJob: Job? = null

    // UI Message / Toast banner state
    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    fun setSelectedTab(tab: Int) {
        if (tab == 2 && !_isAdminUnlocked.value) {
            // Require PIN unlock to view admin panel
            _showPinDialog.value = true
            return
        }
        _selectedTab.value = tab
    }

    fun setSelectedDay(day: String) {
        _selectedDay.value = day
    }

    fun promptAdminPin() {
        if (_isAdminUnlocked.value) {
            _currentRole.value = "admin"
            _selectedTab.value = 2
        } else {
            _showPinDialog.value = true
        }
    }

    fun dismissPinDialog() {
        _showPinDialog.value = false
    }

    fun verifyAndUnlockAdmin(pin: String): Boolean {
        return if (pin == ADMIN_SECURITY_PIN) {
            _isAdminUnlocked.value = true
            _currentRole.value = "admin"
            _showPinDialog.value = false
            _selectedTab.value = 2
            showBanner("🔓 Admin Access Unlocked! (PIN: 9044 Verified)")
            true
        } else {
            showBanner("❌ Incorrect PIN! Try again.")
            false
        }
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
        _currentRole.value = "user"
        if (_selectedTab.value == 2) {
            _selectedTab.value = 0
        }
        showBanner("🔒 Admin Panel Locked.")
    }

    fun toggleRole() {
        if (_currentRole.value == "user") {
            if (_isAdminUnlocked.value) {
                _currentRole.value = "admin"
                showBanner("Switched to ADMIN Mode")
            } else {
                _showPinDialog.value = true
            }
        } else {
            _currentRole.value = "user"
            showBanner("Switched to STUDENT Mode")
        }
    }

    // --- TIMETABLE ACTIONS (ADMIN & USER) ---

    fun addTimetableItem(
        dayOfWeek: String,
        subject: String,
        topic: String,
        startTime: String,
        endTime: String,
        room: String,
        notes: String,
        colorHex: String,
        alarmEnabled: Boolean,
        ringtoneName: String = "Morning Chimes",
        ringtoneType: String = "chimes",
        customRingtoneUri: String? = null
    ) {
        viewModelScope.launch {
            val item = TimetableItem(
                dayOfWeek = dayOfWeek,
                subject = subject.trim(),
                topic = topic.trim(),
                startTime = startTime,
                endTime = endTime,
                roomOrLocation = room.trim().ifEmpty { "Study Desk" },
                notes = notes.trim(),
                colorHex = colorHex,
                alarmEnabled = alarmEnabled,
                ringtoneName = ringtoneName,
                ringtoneType = ringtoneType,
                customRingtoneUri = customRingtoneUri,
                durationMinutes = calculateMinutes(startTime, endTime)
            )
            repository.addTimetableItem(item)
            showBanner("Added '$subject' to $dayOfWeek's Timetable!")
        }
    }

    fun updateTimetableItem(item: TimetableItem) {
        viewModelScope.launch {
            repository.updateTimetableItem(item)
            showBanner("Updated timetable slot for ${item.subject}!")
        }
    }

    fun deleteTimetableItem(item: TimetableItem) {
        viewModelScope.launch {
            repository.deleteTimetableItem(item)
            showBanner("Removed ${item.subject} session.")
        }
    }

    fun toggleAlarm(item: TimetableItem, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(item, enabled)
            val msg = if (enabled) "🔔 Alarm enabled for ${item.subject} (${item.startTime})" else "🔕 Alarm disabled for ${item.subject}"
            showBanner(msg)
        }
    }

    // --- PROGRESS LOGGING & STOPWATCH ---

    fun startStopwatch(subject: String, topic: String) {
        _activeTimerSubject.value = subject
        _activeTimerTopic.value = topic
        _timerSeconds.value = 0
        _isTimerRunning.value = true

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                delay(1000L)
                _timerSeconds.value += 1
            }
        }
        showBanner("⏱️ Study Timer started for $subject!")
    }

    fun pauseOrResumeStopwatch() {
        if (_isTimerRunning.value) {
            _isTimerRunning.value = false
            timerJob?.cancel()
        } else {
            _isTimerRunning.value = true
            timerJob = viewModelScope.launch {
                while (_isTimerRunning.value) {
                    delay(1000L)
                    _timerSeconds.value += 1
                }
            }
        }
    }

    fun finishAndLogStopwatch(notes: String = "") {
        val totalSeconds = _timerSeconds.value
        val minutes = (totalSeconds / 60).coerceAtLeast(1)
        _isTimerRunning.value = false
        timerJob?.cancel()

        viewModelScope.launch {
            repository.logStudySession(
                subject = _activeTimerSubject.value,
                topic = _activeTimerTopic.value,
                durationMinutes = minutes,
                targetMinutes = minutes,
                notes = notes
            )
            _timerSeconds.value = 0
            showBanner("🎉 Logged $minutes mins of ${_activeTimerSubject.value} study time!")
        }
    }

    fun quickLogSession(subject: String, topic: String, durationMinutes: Int, notes: String = "") {
        viewModelScope.launch {
            repository.logStudySession(
                subject = subject,
                topic = topic,
                durationMinutes = durationMinutes,
                targetMinutes = durationMinutes,
                notes = notes
            )
            showBanner("✅ Added $durationMinutes min study log for $subject!")
        }
    }

    fun deleteStudyLog(log: StudyProgressLog) {
        viewModelScope.launch {
            repository.deleteStudyLog(log)
            showBanner("Deleted study record.")
        }
    }

    // --- USER PROFILE & SETTINGS ---

    fun updateProfile(name: String, email: String, goal: String, dailyTarget: Float, weeklyTarget: Float, avatarId: Int) {
        val current = userProfile.value ?: UserProfile()
        val updated = current.copy(
            name = name.trim(),
            email = email.trim(),
            targetGoal = goal.trim(),
            dailyTargetHours = dailyTarget,
            weeklyTargetHours = weeklyTarget,
            avatarId = avatarId,
            lastUpdated = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updateUserProfile(updated)
            showBanner("Profile details saved successfully!")
        }
    }

    // --- ALARM TESTING ---

    fun triggerTestAlarm() {
        alarmScheduler.scheduleTestAlarm("Physics Live Demo", "Calculus and Electromagnetics review")
        showBanner("🚨 Test Alarm scheduled! Ringing in 3 seconds...")
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    private fun showBanner(message: String) {
        _bannerMessage.value = message
    }

    private fun calculateMinutes(start: String, end: String): Int {
        return try {
            val sParts = start.split(":").map { it.toInt() }
            val eParts = end.split(":").map { it.toInt() }
            val sMin = sParts[0] * 60 + sParts[1]
            val eMin = eParts[0] * 60 + eParts[1]
            if (eMin >= sMin) eMin - sMin else (24 * 60 - sMin) + eMin
        } catch (e: Exception) {
            60
        }
    }
}

class StudyViewModelFactory(
    private val repository: StudyRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            return StudyViewModel(repository, alarmScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
