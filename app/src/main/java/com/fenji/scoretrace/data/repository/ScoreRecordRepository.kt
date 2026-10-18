package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.local.dao.ScoreRecordDao
import com.fenji.scoretrace.data.local.entity.ScoreRecord
import com.fenji.scoretrace.util.AppLogger
import com.fenji.scoretrace.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface ScoreRecordRepository {
    /** subjectId 为空表示全部科目 */
    fun observeRecords(subjectId: Long?): Flow<List<ScoreRecord>>

    fun observeRecent(limit: Int = Constants.HOME_RECENT_SCORE_LIMIT): Flow<List<ScoreRecord>>

    /** 全部成绩记录（导出用） */
    fun observeAll(): Flow<List<ScoreRecord>>

    suspend fun addRecord(
        subjectId: Long,
        score: Double,
        fullScore: Double,
        examName: String,
        examDate: Date,
        primarySubject: String? = null,
        secondarySubject: String? = null,
    )

    suspend fun deleteRecord(record: ScoreRecord)

    /**
     * 删除某次考试名下的全部单科记录，返回删除条数。
     * [examName] 为空时不做任何删除（防止空值误删）。
     */
    suspend fun deleteExamByName(examName: String): Int

    suspend fun clearAll()
}

/**
 * 成绩仓库：所有读写都按「当前登录账号」隔离。
 * 未登录（[UserPreferences.authUserId] 为 null）时不过滤，可看到全部本机数据。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DefaultScoreRecordRepository @Inject constructor(
    private val scoreRecordDao: ScoreRecordDao,
    private val userPreferences: UserPreferences,
) : ScoreRecordRepository {

    override fun observeRecords(subjectId: Long?): Flow<List<ScoreRecord>> =
        userPreferences.authUserId.flatMapLatest { uid -> scoreRecordDao.observeBySubject(uid, subjectId) }

    override fun observeRecent(limit: Int): Flow<List<ScoreRecord>> =
        userPreferences.authUserId.flatMapLatest { uid -> scoreRecordDao.observeRecent(uid, limit) }

    override fun observeAll(): Flow<List<ScoreRecord>> =
        userPreferences.authUserId.flatMapLatest { uid -> scoreRecordDao.observeAll(uid) }

    override suspend fun addRecord(
        subjectId: Long,
        score: Double,
        fullScore: Double,
        examName: String,
        examDate: Date,
        primarySubject: String?,
        secondarySubject: String?,
    ) {
        scoreRecordDao.upsert(
            ScoreRecord(
                subjectId = subjectId,
                score = score,
                fullScore = fullScore,
                examName = examName,
                examDate = examDate,
                primarySubject = primarySubject,
                secondarySubject = secondarySubject,
                userId = userPreferences.authUserId.first(),
            )
        )
        AppLogger.i("ScoreInsert", "exam=$examName subjectId=$subjectId score=$score/$fullScore")
    }

    override suspend fun deleteRecord(record: ScoreRecord) {
        scoreRecordDao.delete(record)
        AppLogger.i("ScoreDelete", "record=${record.examName} subjectId=${record.subjectId}")
    }

    override suspend fun deleteExamByName(examName: String): Int {
        if (examName.isBlank()) {
            AppLogger.w("ScoreDelete", "examName 为空，已跳过删除")
            return 0
        }
        val deleted = scoreRecordDao.deleteByExamName(examName, userPreferences.authUserId.first())
        AppLogger.i("ScoreDelete", "exam=$examName deletedRows=$deleted")
        return deleted
    }

    override suspend fun clearAll() = scoreRecordDao.clearAll()
}
