package com.fenji.scoretrace.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenji.scoretrace.data.local.entity.ScoreRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreRecordDao {

    /** [userId] 为 null 表示不按账号过滤（显示全部，用于未登录或迁移前的窗口期）。 */
    @Query("SELECT * FROM score_records WHERE (:userId IS NULL OR userId = :userId) ORDER BY examDate DESC")
    fun observeAll(userId: String?): Flow<List<ScoreRecord>>

    /** subjectId 为空表示不筛选（全部科目） */
    @Query(
        "SELECT * FROM score_records WHERE (:userId IS NULL OR userId = :userId) " +
            "AND (:subjectId IS NULL OR subjectId = :subjectId) ORDER BY examDate DESC"
    )
    fun observeBySubject(userId: String?, subjectId: Long?): Flow<List<ScoreRecord>>

    @Query("SELECT * FROM score_records WHERE (:userId IS NULL OR userId = :userId) ORDER BY examDate DESC LIMIT :limit")
    fun observeRecent(userId: String?, limit: Int): Flow<List<ScoreRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ScoreRecord): Long

    @Delete
    suspend fun delete(record: ScoreRecord)

    /** 删除某次考试名下的全部单科记录（按账号隔离），返回删除条数。 */
    @Query("DELETE FROM score_records WHERE examName = :examName AND (:userId IS NULL OR userId = :userId)")
    suspend fun deleteByExamName(examName: String, userId: String?): Int

    @Query("DELETE FROM score_records")
    suspend fun clearAll()

    /** 尚未与账号绑定的记录数（用于首次登录时的迁移提示）。 */
    @Query("SELECT COUNT(*) FROM score_records WHERE userId IS NULL")
    suspend fun countUnclaimed(): Int

    /** 把未认领的记录绑定到 [userId]，返回受影响行数。 */
    @Query("UPDATE score_records SET userId = :userId WHERE userId IS NULL")
    suspend fun claimUnclaimed(userId: String): Int
}
