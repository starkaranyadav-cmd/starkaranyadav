package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "study_progress_logs")
data class StudyProgressLog(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var userId: String = "user_default_01",
    var timetableItemId: String = "",
    var subject: String = "",
    var topic: String = "",
    var dateString: String = "", // e.g. "2026-08-19"
    var durationMinutes: Int = 45,
    var targetMinutes: Int = 60,
    var notes: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var isCompleted: Boolean = true
) {
    val durationHours: Float
        get() = durationMinutes / 60f
}
