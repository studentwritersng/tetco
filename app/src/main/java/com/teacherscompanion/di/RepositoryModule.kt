package com.teacherscompanion.di

import com.teacherscompanion.core.SyncManager
import com.teacherscompanion.data.local.dao.*
import com.teacherscompanion.data.repository.ProfileRepository
import com.teacherscompanion.data.repository.SchoolRepository
import com.teacherscompanion.data.repository.SyllabusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSchoolRepository(
        supabaseClient: SupabaseClient,
        schoolDao: SchoolDao,
        schoolClassDao: SchoolClassDao,
        subjectDao: SubjectDao,
        classLevelDao: ClassLevelDao,
        profileDao: ProfileDao,
        syllabusTopicDao: SyllabusTopicDao,
        syncManager: SyncManager
    ): SchoolRepository {
        return SchoolRepository(
            supabaseClient, schoolDao, schoolClassDao, subjectDao,
            classLevelDao, profileDao, syllabusTopicDao, syncManager
        )
    }

    @Provides
    @Singleton
    fun provideSyllabusRepository(
        supabaseClient: SupabaseClient,
        syncManager: SyncManager,
        syllabusTopicDao: SyllabusTopicDao,
        lessonNoteDao: LessonNoteDao,
        questionDao: QuestionDao,
        subjectDao: SubjectDao,
        schoolClassDao: SchoolClassDao,
        classLevelDao: ClassLevelDao,
        questionHistoryDao: QuestionHistoryDao
    ): SyllabusRepository {
        return SyllabusRepository(
            supabaseClient, syncManager,
            syllabusTopicDao, lessonNoteDao, questionDao,
            questionHistoryDao, subjectDao, schoolClassDao,
            classLevelDao
        )
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        supabaseClient: SupabaseClient,
        profileDao: ProfileDao,
        planDao: PlanDao,
        referralHistoryDao: ReferralHistoryDao,
        syncManager: SyncManager
    ): ProfileRepository {
        return ProfileRepository(supabaseClient, profileDao, planDao, referralHistoryDao, syncManager)
    }
}
