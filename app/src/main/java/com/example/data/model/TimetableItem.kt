package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "timetable_items")
data class TimetableItem(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var dayOfWeek: String = "Monday", // Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday
    var subject: String = "",
    var topic: String = "",
    var startTime: String = "09:00", // "HH:mm" in 24-hr format
    var endTime: String = "10:30",   // "HH:mm" in 24-hr format
    var durationMinutes: Int = 90,
    var roomOrLocation: String = "Study Desk",
    var notes: String = "",
    var colorHex: String = "#6750A4",
    var alarmEnabled: Boolean = true,
    var ringtoneName: String = "Morning Chimes",
    var ringtoneType: String = "chimes", // "chimes", "energy", "bell", "zen", "digital", "system", "custom"
    var customRingtoneUri: String? = null,
    var alarmRequestCode: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var createdBy: String = "starkaranyadav@gmail.com"
) {
    // Helper to calculate minutes between start and end time
    fun calculateDuration(): Int {
        return try {
            val startParts = startTime.split(":").map { it.toInt() }
            val endParts = endTime.split(":").map { it.toInt() }
            val startMin = startParts[0] * 60 + startParts[1]
            val endMin = endParts[0] * 60 + endParts[1]
            if (endMin >= startMin) endMin - startMin else (24 * 60 - startMin) + endMin
        } catch (e: Exception) {
            durationMinutes
        }
    }

    // Formats start and end time for friendly display (e.g. 9:00 AM - 10:30 AM)
    fun getFormattedTimeRange(): String {
        return "${formatTime12Hour(startTime)} - ${formatTime12Hour(endTime)}"
    }

    companion object {
        fun formatTime12Hour(time24: String): String {
            return try {
                val parts = time24.split(":")
                val hour = parts[0].toInt()
                val min = parts[1].toInt()
                val amPm = if (hour >= 12) "PM" else "AM"
                val hour12 = if (hour % 12 == 0) 12 else hour % 12
                String.format("%02d:%02d %s", hour12, min, amPm)
            } catch (e: Exception) {
                time24
            }
        }

        val DAYS_OF_WEEK = listOf(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        )

        val DEFAULT_COLORS = listOf(
            "#6750A4", // Vibrant Purple
            "#FFB4AB", // Coral Accent
            "#7D5260", // Deep Mauve
            "#00687A", // Cyan Teal
            "#386A20", // Forest Olive
            "#F59E0B", // Warm Amber
            "#984061", // Berry
            "#625B71"  // Soft Slate
        )

        val RINGTONES = listOf(
            RingtoneOption("Morning Chimes", "chimes", "🎶 Pleasant melodic harp chimes"),
            RingtoneOption("High Energy Wakeup", "energy", "⚡ Upbeat study alarm tones"),
            RingtoneOption("Focus Bell", "bell", "🔔 Clear harmonic study bell"),
            RingtoneOption("Zen Sanctuary", "zen", "🧘 Deep calming meditation waves"),
            RingtoneOption("Digital Alarm Pulse", "digital", "⏰ Classic electronic clock beep"),
            RingtoneOption("System Default Alarm", "system", "📱 Android default device alarm")
        )
    }
}

data class RingtoneOption(
    val name: String,
    val type: String,
    val description: String
)
