package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StudyProgressLog
import com.example.data.model.TimetableItem
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    // Timetable Items
    @Query("SELECT * FROM timetable_items ORDER BY startTime ASC")
    fun getAllTimetableItemsFlow(): Flow<List<TimetableItem>>

    @Query("SELECT * FROM timetable_items ORDER BY startTime ASC")
    suspend fun getAllTimetableItems(): List<TimetableItem>

    @Query("SELECT * FROM timetable_items WHERE dayOfWeek = :day ORDER BY startTime ASC")
    fun getTimetableForDayFlow(day: String): Flow<List<TimetableItem>>

    @Query("SELECT * FROM timetable_items WHERE id = :id LIMIT 1")
    suspend fun getTimetableItemById(id: String): TimetableItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableItem(item: TimetableItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableItems(items: List<TimetableItem>)

    @Update
    suspend fun updateTimetableItem(item: TimetableItem)

    @Delete
    suspend fun deleteTimetableItem(item: TimetableItem)

    @Query("DELETE FROM timetable_items WHERE id = :id")
    suspend fun deleteTimetableItemById(id: String)

    @Query("DELETE FROM timetable_items")
    suspend fun clearAllTimetableItems()

    // User Profile
    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    fun getUserProfileFlow(uid: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles")
    fun getAllUsersFlow(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getUserProfile(uid: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfiles(profiles: List<UserProfile>)

    @Delete
    suspend fun deleteUserProfile(userProfile: UserProfile)

    @Query("DELETE FROM user_profiles")
    suspend fun clearAllUserProfiles()

    // Progress Logs
    @Query("SELECT * FROM study_progress_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getStudyLogsForUserFlow(userId: String): Flow<List<StudyProgressLog>>

    @Query("SELECT * FROM study_progress_logs WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getStudyLogsForUser(userId: String): List<StudyProgressLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLog(log: StudyProgressLog)

    @Delete
    suspend fun deleteStudyLog(log: StudyProgressLog)

    @Query("DELETE FROM study_progress_logs WHERE id = :id")
    suspend fun deleteStudyLogById(id: String)
}
