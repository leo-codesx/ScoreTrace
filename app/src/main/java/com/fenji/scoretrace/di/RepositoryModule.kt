package com.fenji.scoretrace.di

import com.fenji.scoretrace.data.repository.DefaultConversationRepository
import com.fenji.scoretrace.data.repository.DefaultAuthRepository
import com.fenji.scoretrace.data.repository.DefaultExamRecordRepository
import com.fenji.scoretrace.data.repository.DefaultGlmVisionRepository
import com.fenji.scoretrace.data.repository.DefaultLocalDataMigrationRepository
import com.fenji.scoretrace.data.repository.DefaultMajorRepository
import com.fenji.scoretrace.data.repository.DefaultMusicRepository
import com.fenji.scoretrace.data.repository.DefaultNetworkRepository
import com.fenji.scoretrace.data.repository.DefaultNotificationRepository
import com.fenji.scoretrace.data.repository.DefaultSchoolRepository
import com.fenji.scoretrace.data.repository.DefaultSchoolAdmissionRepository
import com.fenji.scoretrace.data.repository.DefaultScoreRecordRepository
import com.fenji.scoretrace.data.repository.DefaultProvinceScoreRepository
import com.fenji.scoretrace.data.repository.DefaultScoreSectionRepository
import com.fenji.scoretrace.data.repository.DefaultStudyTaskRepository
import com.fenji.scoretrace.data.repository.DefaultSubjectRepository
import com.fenji.scoretrace.data.repository.DefaultTargetSchoolRepository
import com.fenji.scoretrace.data.repository.ConversationRepository
import com.fenji.scoretrace.data.repository.AuthRepository
import com.fenji.scoretrace.data.repository.ExamRecordRepository
import com.fenji.scoretrace.data.repository.GlmVisionRepository
import com.fenji.scoretrace.data.repository.LocalDataMigrationRepository
import com.fenji.scoretrace.data.repository.MajorRepository
import com.fenji.scoretrace.data.repository.MusicRepository
import com.fenji.scoretrace.data.repository.NetworkRepository
import com.fenji.scoretrace.data.repository.NotificationRepository
import com.fenji.scoretrace.data.repository.SchoolRepository
import com.fenji.scoretrace.data.repository.SchoolAdmissionRepository
import com.fenji.scoretrace.data.repository.ScoreRecordRepository
import com.fenji.scoretrace.data.repository.ProvinceScoreRepository
import com.fenji.scoretrace.data.repository.ScoreSectionRepository
import com.fenji.scoretrace.data.repository.StudyTaskRepository
import com.fenji.scoretrace.data.repository.SubjectRepository
import com.fenji.scoretrace.data.repository.TargetSchoolRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSubjectRepository(impl: DefaultSubjectRepository): SubjectRepository

    @Binds
    abstract fun bindStudyTaskRepository(impl: DefaultStudyTaskRepository): StudyTaskRepository

    @Binds
    abstract fun bindScoreRecordRepository(impl: DefaultScoreRecordRepository): ScoreRecordRepository

    @Binds
    abstract fun bindTargetSchoolRepository(impl: DefaultTargetSchoolRepository): TargetSchoolRepository

    @Binds
    abstract fun bindNetworkRepository(impl: DefaultNetworkRepository): NetworkRepository

    @Binds
    abstract fun bindSchoolRepository(impl: DefaultSchoolRepository): SchoolRepository

    @Binds
    abstract fun bindMajorRepository(impl: DefaultMajorRepository): MajorRepository

    @Binds
    abstract fun bindExamRecordRepository(impl: DefaultExamRecordRepository): ExamRecordRepository

    @Binds
    abstract fun bindNotificationRepository(impl: DefaultNotificationRepository): NotificationRepository

    @Binds
    abstract fun bindConversationRepository(impl: DefaultConversationRepository): ConversationRepository

    @Binds
    abstract fun bindGlmVisionRepository(impl: DefaultGlmVisionRepository): GlmVisionRepository

    @Binds
    abstract fun bindProvinceScoreRepository(impl: DefaultProvinceScoreRepository): ProvinceScoreRepository

    @Binds
    abstract fun bindSchoolAdmissionRepository(impl: DefaultSchoolAdmissionRepository): SchoolAdmissionRepository

    @Binds
    abstract fun bindMusicRepository(impl: DefaultMusicRepository): MusicRepository

    @Binds
    abstract fun bindScoreSectionRepository(impl: DefaultScoreSectionRepository): ScoreSectionRepository

    @Binds
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    abstract fun bindLocalDataMigrationRepository(
        impl: DefaultLocalDataMigrationRepository,
    ): LocalDataMigrationRepository
}
