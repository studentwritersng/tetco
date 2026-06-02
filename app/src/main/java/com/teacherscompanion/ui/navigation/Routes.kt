package com.teacherscompanion.ui.navigation

sealed class Route(val route: String) {
    data object Splash : Route("splash")
    data object Login : Route("login")
    data object SignUp : Route("signup")
    data object ForgotPassword : Route("forgot_password")
    data object Home : Route("home")
    data object Schools : Route("schools")
    data object SchoolDetail : Route("schools/{schoolId}") {
        fun create(schoolId: String) = "schools/$schoolId"
    }
    data object AddEditSchool : Route("schools/add?schoolId={schoolId}") {
        fun create() = "schools/add"
        fun create(schoolId: String) = "schools/add?schoolId=$schoolId"
    }
    data object ClassDetail : Route("schools/{schoolId}/classes/{classId}") {
        fun create(schoolId: String, classId: String) = "schools/$schoolId/classes/$classId"
    }
    data object AddClass : Route("schools/{schoolId}/classes/add") {
        fun create(schoolId: String) = "schools/$schoolId/classes/add"
    }
    data object SubjectDetail : Route("schools/{schoolId}/classes/{classId}/subjects/{subjectId}") {
        fun create(schoolId: String, classId: String, subjectId: String) = "schools/$schoolId/classes/$classId/subjects/$subjectId"
    }
    data object AddEditSubject : Route("schools/{schoolId}/classes/{classId}/subjects/add?subjectId={subjectId}") {
        fun create(schoolId: String, classId: String) = "schools/$schoolId/classes/$classId/subjects/add"
        fun create(schoolId: String, classId: String, subjectId: String) = "schools/$schoolId/classes/$classId/subjects/add?subjectId=$subjectId"
    }
    data object AddEditTopic : Route("subjects/{subjectId}/topics/add?topicId={topicId}") {
        fun create(subjectId: String) = "subjects/$subjectId/topics/add"
        fun create(subjectId: String, topicId: String) = "subjects/$subjectId/topics/add?topicId=$topicId"
    }
    data object TopicDetail : Route("subjects/{subjectId}/topics/{topicId}/note") {
        fun create(subjectId: String, topicId: String) = "subjects/$subjectId/topics/$topicId/note"
    }
    data object EditLessonNote : Route("topics/{topicId}/note/edit") {
        fun create(topicId: String) = "topics/$topicId/note/edit"
    }
    data object Alarms : Route("alarms")
    data object AddEditAlarm : Route("alarms/add?alarmId={alarmId}") {
        fun create() = "alarms/add"
        fun create(alarmId: String) = "alarms/add?alarmId=$alarmId"
    }
    data object AddEditPeriodReminder : Route("alarms/periods/add?reminderId={reminderId}") {
        fun create() = "alarms/periods/add"
        fun create(reminderId: String) = "alarms/periods/add?reminderId=$reminderId"
    }
    data object GapList : Route("alarms/gaps")
    data object Profile : Route("profile")
    data object EditProfile : Route("profile/edit")
    data object ChangePassword : Route("profile/change-password")
    data object DeleteAccount : Route("profile/delete-account")
    data object Plans : Route("settings/plans")
    data object Subscription : Route("settings/subscription")
    data object Referral : Route("profile/referral")
    data object Help : Route("help")
    data object FaqCategory : Route("help/category/{categoryId}") {
        fun create(categoryId: String) = "help/category/$categoryId"
    }
    data object FaqSearch : Route("help/search")
    data object ContactSupport : Route("help/contact")
    data object QuestionGenerator : Route("questions/generate")
    data object QuestionHistory : Route("questions/history")
}
