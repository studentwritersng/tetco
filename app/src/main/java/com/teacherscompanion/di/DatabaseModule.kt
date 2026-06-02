package com.teacherscompanion.di

import android.content.Context
import androidx.room.Room
import com.teacherscompanion.core.SupabaseClientProvider
import com.teacherscompanion.data.local.dao.*
import com.teacherscompanion.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "teachers_companion_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()
    @Provides fun provideFaqDao(db: AppDatabase): FaqDao = db.faqDao()
    @Provides fun providePendingSyncDao(db: AppDatabase): PendingSyncDao = db.pendingSyncDao()
    @Provides fun provideSchoolDao(db: AppDatabase): SchoolDao = db.schoolDao()
    @Provides fun provideSchoolClassDao(db: AppDatabase): SchoolClassDao = db.schoolClassDao()
    @Provides fun provideSubjectDao(db: AppDatabase): SubjectDao = db.subjectDao()
    @Provides fun provideSyllabusTopicDao(db: AppDatabase): SyllabusTopicDao = db.syllabusTopicDao()
    @Provides fun provideLessonNoteDao(db: AppDatabase): LessonNoteDao = db.lessonNoteDao()
    @Provides fun provideQuestionDao(db: AppDatabase): QuestionDao = db.questionDao()
    @Provides fun provideClassLevelDao(db: AppDatabase): ClassLevelDao = db.classLevelDao()
    @Provides fun providePlanDao(db: AppDatabase): PlanDao = db.planDao()
    @Provides fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()
    @Provides fun provideReferralHistoryDao(db: AppDatabase): ReferralHistoryDao = db.referralHistoryDao()
    @Provides fun provideQuestionHistoryDao(db: AppDatabase): QuestionHistoryDao = db.questionHistoryDao()

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClientProvider.client
}
