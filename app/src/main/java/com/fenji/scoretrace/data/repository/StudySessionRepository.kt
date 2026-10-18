package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.local.dao.StudySessionDao
import com.fenji.scoretrace.data.local.entity.StudySession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

interface StudySessionRepository {
    /** 当前账号的全部专注记录，按开始时间倒序 */
    fun observeSessions(): Flow<List<StudySession>>

    suspend fun addSession(startedAt: Long, durationSeconds: Int, type: String)

    suspend fun clearAll()
}

/** 专注计时仓库：读写按「当前登录账号」隔离。 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DefaultStudySessionRepository @Inject constructor(
    private val studySessionDao: StudySessionDao,
    private val userPreferences: UserPreferences,
) : StudySessionRepository {

    override fun observeSessions(): Flow<List<StudySession>> =
        userPreferences.authUserId.flatMapLatest { uid -> studySessionDao.observeAll(uid) }

    override suspend fun addSession(startedAt: Long, durationSeconds: Int, type: String) {
        studySessionDao.upsert(
            StudySession(
                startedAt = startedAt,
                durationSeconds = durationSeconds,
                type = type,
                userId = userPreferences.authUserId.first(),
            )
        )
    }

    override suspend fun clearAll() = studySessionDao.clearAll()
}
