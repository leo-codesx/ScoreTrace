package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.local.dao.ExamRecordDao
import com.fenji.scoretrace.data.local.entity.ExamRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface ExamRecordRepository {
    fun observeAll(): Flow<List<ExamRecord>>

    suspend fun save(
        examName: String,
        examDate: Date,
        classRank: Int?,
        gradeRank: Int?,
    )

    suspend fun clearAll()
}

/** 考试级记录仓库：读写按「当前登录账号」隔离。 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DefaultExamRecordRepository @Inject constructor(
    private val examRecordDao: ExamRecordDao,
    private val userPreferences: UserPreferences,
) : ExamRecordRepository {

    override fun observeAll(): Flow<List<ExamRecord>> =
        userPreferences.authUserId.flatMapLatest { uid -> examRecordDao.observeAll(uid) }

    override suspend fun save(
        examName: String,
        examDate: Date,
        classRank: Int?,
        gradeRank: Int?,
    ) {
        val uid = userPreferences.authUserId.first()
        // 覆盖式：先清同名旧行，避免同一考试重复保存累积多条排名行
        examRecordDao.deleteByExamName(examName, uid)
        examRecordDao.upsert(
            ExamRecord(
                examName = examName,
                examDate = examDate,
                classRank = classRank,
                gradeRank = gradeRank,
                userId = uid,
            )
        )
    }

    override suspend fun clearAll() = examRecordDao.clearAll()
}
