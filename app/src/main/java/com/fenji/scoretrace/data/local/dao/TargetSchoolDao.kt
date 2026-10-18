package com.fenji.scoretrace.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenji.scoretrace.data.local.entity.TargetSchool
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetSchoolDao {

    @Query("SELECT * FROM target_schools WHERE (:userId IS NULL OR userId = :userId) ORDER BY id ASC")
    fun observeAll(userId: String?): Flow<List<TargetSchool>>

    /** 首页只关心最新设定的一条目标 */
    @Query("SELECT * FROM target_schools WHERE (:userId IS NULL OR userId = :userId) ORDER BY id DESC LIMIT 1")
    fun observeLatest(userId: String?): Flow<TargetSchool?>

    /** 编辑面板打开时用于预填，一次性读取 */
    @Query("SELECT * FROM target_schools WHERE (:userId IS NULL OR userId = :userId) ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(userId: String?): TargetSchool?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TargetSchool): Long

    @Delete
    suspend fun delete(entity: TargetSchool)

    @Query("DELETE FROM target_schools")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM target_schools WHERE userId IS NULL")
    suspend fun countUnclaimed(): Int

    @Query("UPDATE target_schools SET userId = :userId WHERE userId IS NULL")
    suspend fun claimUnclaimed(userId: String): Int
}
