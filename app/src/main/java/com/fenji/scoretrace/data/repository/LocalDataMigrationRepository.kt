package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.local.dao.ExamRecordDao
import com.fenji.scoretrace.data.local.dao.ScoreRecordDao
import com.fenji.scoretrace.data.local.dao.StudySessionDao
import com.fenji.scoretrace.data.local.dao.TargetSchoolDao
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** 尚未与账号绑定的本机历史数据统计。 */
data class UnclaimedLocalData(
    val scoreRecords: Int = 0,
    val targetSchools: Int = 0,
    val studySessions: Int = 0,
    val examRecords: Int = 0,
) {
    val total: Int get() = scoreRecords + targetSchools + studySessions + examRecords
}

/**
 * 本机历史数据 → 账号的绑定（迁移）。
 *
 * 强制登录版本上线后，老用户的成绩 / 目标 / 计时等数据仍是「未认领」（userId 为 null）；
 * 首次登录时询问是否把它们绑定到当前账号，避免换账号后串数据。
 */
interface LocalDataMigrationRepository {
    suspend fun countUnclaimed(): UnclaimedLocalData

    /** 把未认领的数据绑定到当前登录账号，返回受影响行数（未登录返回 0）。 */
    suspend fun claimToCurrentUser(): Int
}

@Singleton
class DefaultLocalDataMigrationRepository @Inject constructor(
    private val scoreRecordDao: ScoreRecordDao,
    private val targetSchoolDao: TargetSchoolDao,
    private val studySessionDao: StudySessionDao,
    private val examRecordDao: ExamRecordDao,
    private val userPreferences: UserPreferences,
) : LocalDataMigrationRepository {

    override suspend fun countUnclaimed(): UnclaimedLocalData = UnclaimedLocalData(
        scoreRecords = scoreRecordDao.countUnclaimed(),
        targetSchools = targetSchoolDao.countUnclaimed(),
        studySessions = studySessionDao.countUnclaimed(),
        examRecords = examRecordDao.countUnclaimed(),
    )

    override suspend fun claimToCurrentUser(): Int {
        val uid = userPreferences.authUserId.first() ?: return 0
        return scoreRecordDao.claimUnclaimed(uid) +
            targetSchoolDao.claimUnclaimed(uid) +
            studySessionDao.claimUnclaimed(uid) +
            examRecordDao.claimUnclaimed(uid)
    }
}
