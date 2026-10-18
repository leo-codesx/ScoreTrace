package com.fenji.scoretrace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenji.scoretrace.data.local.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Query("SELECT * FROM study_sessions WHERE (:userId IS NULL OR userId = :userId) ORDER BY startedAt DESC")
    fun observeAll(userId: String?): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: StudySession): Long

    @Query("DELETE FROM study_sessions")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM study_sessions WHERE userId IS NULL")
    suspend fun countUnclaimed(): Int

    @Query("UPDATE study_sessions SET userId = :userId WHERE userId IS NULL")
    suspend fun claimUnclaimed(userId: String): Int
}
