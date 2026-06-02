package com.teacherscompanion.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.teacherscompanion.data.local.dao.*
import com.teacherscompanion.data.local.entity.*

@Database(
    entities = [
        AlarmEntity::class, FaqItemCache::class,
        PendingSyncEntity::class,
        SchoolEntity::class, SchoolClassEntity::class, SubjectEntity::class,
        SyllabusTopicEntity::class, LessonNoteEntity::class, QuestionEntity::class,
        ClassLevelEntity::class, PlanEntity::class, ProfileEntity::class, ReferralHistoryEntity::class,
        QuestionHistoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun faqDao(): FaqDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun schoolDao(): SchoolDao
    abstract fun schoolClassDao(): SchoolClassDao
    abstract fun subjectDao(): SubjectDao
    abstract fun syllabusTopicDao(): SyllabusTopicDao
    abstract fun lessonNoteDao(): LessonNoteDao
    abstract fun questionDao(): QuestionDao
    abstract fun classLevelDao(): ClassLevelDao
    abstract fun planDao(): PlanDao
    abstract fun profileDao(): ProfileDao
    abstract fun referralHistoryDao(): ReferralHistoryDao
    abstract fun questionHistoryDao(): QuestionHistoryDao
}
