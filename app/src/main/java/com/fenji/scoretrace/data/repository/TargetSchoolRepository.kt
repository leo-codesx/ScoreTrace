package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.data.local.dao.TargetSchoolDao
import com.fenji.scoretrace.data.local.entity.TargetSchool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

interface TargetSchoolRepository {
    fun observeLatest(): Flow<TargetSchool?>

    /** [id] 传 0 表示新增；传已有 id 则覆盖那条记录 */
    suspend fun save(
        schoolName: String,
        majorName: String,
        targetScore: Int,
        currentScore: Int,
        year: Int,
        id: Long = 0L,
    ): Long

    suspend fun delete(entity: TargetSchool)

    suspend fun clearAll()
}

/** 目标院校仓库：读写按「当前登录账号」隔离。 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DefaultTargetSchoolRepository @Inject constructor(
    private val targetSchoolDao: TargetSchoolDao,
    private val userPreferences: UserPreferences,
) : TargetSchoolRepository {

    override fun observeLatest(): Flow<TargetSchool?> =
        userPreferences.authUserId.flatMapLatest { uid -> targetSchoolDao.observeLatest(uid) }

    override suspend fun save(
        schoolName: String,
        majorName: String,
        targetScore: Int,
        currentScore: Int,
        year: Int,
        id: Long,
    ): Long = targetSchoolDao.upsert(
        TargetSchool(
            id = id,
            schoolName = schoolName,
            majorName = majorName,
            targetScore = targetScore,
            currentScore = currentScore,
            year = year,
            userId = userPreferences.authUserId.first(),
        )
    )

    override suspend fun delete(entity: TargetSchool) = targetSchoolDao.delete(entity)

    override suspend fun clearAll() = targetSchoolDao.clearAll()
}
