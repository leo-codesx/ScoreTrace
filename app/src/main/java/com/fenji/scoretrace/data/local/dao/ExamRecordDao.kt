package com.fenji.scoretrace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenji.scoretrace.data.local.entity.ExamRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamRecordDao {

    @Query("SELECT * FROM exam_records WHERE (:userId IS NULL OR userId = :userId) ORDER BY examDate DESC")
    fun observeAll(userId: String?): Flow<List<ExamRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ExamRecord): Long

    /** 删除某次考试的排名行（按账号隔离），返回删除条数（重录/编辑时先清旧行，避免同名累积）。 */
    @Query("DELETE FROM exam_records WHERE examName = :examName AND (:userId IS NULL OR userId = :userId)")
    suspend fun deleteByExamName(examName: String, userId: String?): Int

    @Query("DELETE FROM exam_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM exam_records WHERE userId IS NULL")
    suspend fun countUnclaimed(): Int

    @Query("UPDATE exam_records SET userId = :userId WHERE userId IS NULL")
    suspend fun claimUnclaimed(userId: String): Int
}
