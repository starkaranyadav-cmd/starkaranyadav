package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    var uid: String = "user_star_main_admin",
    var name: String = "Star",
    var email: String = "starkaranyadav@gmail.com",
    var avatarId: Int = 0, // index for default avatars
    var targetGoal: String = "Main Administrator & Master Study Coordinator",
    var dailyTargetHours: Float = 8.0f,
    var weeklyTargetHours: Float = 45.0f,
    var role: String = "admin", // "admin" or "user"
    var joinedDate: String = "August 2026",
    var totalStudyHoursLogged: Float = 36.5f,
    var currentStreakDays: Int = 12,
    var lastUpdated: Long = System.currentTimeMillis()
)
