package com.teacherscompanion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.teacherscompanion.core.AuthManager
import com.teacherscompanion.ui.alarms.AddEditAlarmScreen
import com.teacherscompanion.ui.alarms.AddEditPeriodReminderScreen
import com.teacherscompanion.ui.alarms.AlarmsScreen
import com.teacherscompanion.ui.alarms.GapListScreen
import com.teacherscompanion.ui.auth.ForgotPasswordScreen
import com.teacherscompanion.ui.auth.LoginScreen
import com.teacherscompanion.ui.auth.SignUpScreen
import com.teacherscompanion.ui.auth.SplashScreen
import com.teacherscompanion.ui.home.HomeScreen
import com.teacherscompanion.ui.plans.PlanSelectionScreen
import com.teacherscompanion.ui.plans.SubscriptionScreen
import com.teacherscompanion.ui.profile.ChangePasswordScreen
import com.teacherscompanion.ui.questions.QuestionGeneratorScreen
import com.teacherscompanion.ui.questions.QuestionHistoryScreen
import com.teacherscompanion.ui.profile.ContactSupportScreen
import com.teacherscompanion.ui.profile.DeleteAccountScreen
import com.teacherscompanion.ui.profile.EditProfileScreen
import com.teacherscompanion.ui.profile.FaqCategoryScreen
import com.teacherscompanion.ui.profile.FaqSearchScreen
import com.teacherscompanion.ui.profile.HelpScreen
import com.teacherscompanion.ui.profile.ProfileScreen
import com.teacherscompanion.ui.referral.ReferralScreen
import com.teacherscompanion.ui.schools.AddEditSchoolScreen
import com.teacherscompanion.ui.schools.AddEditSubjectScreen
import com.teacherscompanion.ui.schools.ClassDetailScreen
import com.teacherscompanion.ui.schools.SchoolDetailScreen
import com.teacherscompanion.ui.schools.SchoolListScreen
import com.teacherscompanion.ui.syllabus.AddEditTopicScreen
import com.teacherscompanion.ui.syllabus.EditLessonNoteScreen
import com.teacherscompanion.ui.syllabus.SubjectDetailScreen
import com.teacherscompanion.ui.syllabus.TopicDetailScreen

@Composable
fun AppNavigation(
    authManager: AuthManager
) {
    val isLoggedIn by authManager.isLoggedIn.collectAsState()
    val navController = rememberNavController()

    val startDestination = if (isLoggedIn) Route.Home.route else Route.Splash.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.Splash.route) {
            SplashScreen(
                onNavigateToLogin = { navController.navigate(Route.Login.route) },
                onNavigateToHome = { navController.navigate(Route.Home.route) { popUpTo(Route.Splash.route) { inclusive = true } } },
                authManager = authManager
            )
        }

        composable(Route.Login.route) {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Route.SignUp.route) },
                onNavigateToForgotPassword = { navController.navigate(Route.ForgotPassword.route) },
                onLoginSuccess = { navController.navigate(Route.Home.route) { popUpTo(Route.Login.route) { inclusive = true } } },
                authManager = authManager
            )
        }

        composable(Route.SignUp.route) {
            SignUpScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignUpSuccess = { navController.navigate(Route.Home.route) { popUpTo(Route.SignUp.route) { inclusive = true } } },
                authManager = authManager
            )
        }

        composable(Route.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                authManager = authManager
            )
        }

        composable(Route.Home.route) {
            HomeScreen(
                onNavigateToSchools = { navController.navigate(Route.Schools.route) },
                onNavigateToAlarms = { navController.navigate(Route.Alarms.route) },
                onNavigateToProfile = { navController.navigate(Route.Profile.route) },
                onNavigateToQuestionGenerator = { navController.navigate(Route.QuestionGenerator.route) }
            )
        }

        composable(Route.Schools.route) {
            SchoolListScreen(
                onNavigateToAddSchool = { navController.navigate(Route.AddEditSchool.create()) },
                onNavigateToSchoolDetail = { schoolId -> navController.navigate(Route.SchoolDetail.create(schoolId)) }
            )
        }

        composable(
            route = Route.SchoolDetail.route,
            arguments = listOf(navArgument("schoolId") { type = NavType.StringType })
        ) { backStackEntry ->
            val schoolId = backStackEntry.arguments?.getString("schoolId") ?: return@composable
            SchoolDetailScreen(
                schoolId = schoolId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClassDetail = { classId -> navController.navigate(Route.ClassDetail.create(schoolId, classId)) }
            )
        }

        composable(
            route = Route.AddEditSchool.route,
            arguments = listOf(navArgument("schoolId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val schoolId = backStackEntry.arguments?.getString("schoolId")
            AddEditSchoolScreen(
                schoolId = schoolId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.ClassDetail.route,
            arguments = listOf(
                navArgument("schoolId") { type = NavType.StringType },
                navArgument("classId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val schoolId = backStackEntry.arguments?.getString("schoolId") ?: return@composable
            val classId = backStackEntry.arguments?.getString("classId") ?: return@composable
            ClassDetailScreen(
                schoolId = schoolId,
                classId = classId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubjectDetail = { subjectId -> navController.navigate(Route.SubjectDetail.create(schoolId, classId, subjectId)) },
                onNavigateToAddSubject = { navController.navigate(Route.AddEditSubject.create(schoolId, classId)) }
            )
        }

        composable(
            route = Route.AddEditSubject.route,
            arguments = listOf(
                navArgument("schoolId") { type = NavType.StringType },
                navArgument("classId") { type = NavType.StringType },
                navArgument("subjectId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: return@composable
            val subjectId = backStackEntry.arguments?.getString("subjectId")
            AddEditSubjectScreen(
                classId = classId,
                subjectId = subjectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.SubjectDetail.route,
            arguments = listOf(
                navArgument("schoolId") { type = NavType.StringType },
                navArgument("classId") { type = NavType.StringType },
                navArgument("subjectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: return@composable
            SubjectDetailScreen(
                subjectId = subjectId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddTopic = { navController.navigate(Route.AddEditTopic.create(subjectId)) },
                onNavigateToTopicDetail = { topicId -> navController.navigate(Route.TopicDetail.create(subjectId, topicId)) }
            )
        }

        composable(
            route = Route.AddEditTopic.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("topicId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: return@composable
            val topicId = backStackEntry.arguments?.getString("topicId")
            AddEditTopicScreen(
                subjectId = subjectId,
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.TopicDetail.route,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("topicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: return@composable
            val topicId = backStackEntry.arguments?.getString("topicId") ?: return@composable
            TopicDetailScreen(
                subjectId = subjectId,
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditNote = { navController.navigate(Route.EditLessonNote.create(topicId)) },
                onNavigateToTopic = { sid, tid -> navController.navigate(Route.TopicDetail.create(sid, tid)) { popUpTo(navController.currentDestination?.route ?: Route.Home.route) } }
            )
        }

        composable(
            route = Route.EditLessonNote.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: return@composable
            EditLessonNoteScreen(
                topicId = topicId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.Alarms.route) {
            AlarmsScreen(
                onNavigateToAddAlarm = { navController.navigate(Route.AddEditAlarm.create()) },
                onNavigateToAddPeriodReminder = { navController.navigate(Route.AddEditPeriodReminder.create()) },
                onNavigateToGapList = { navController.navigate(Route.GapList.route) }
            )
        }

        composable(
            route = Route.AddEditAlarm.route,
            arguments = listOf(navArgument("alarmId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getString("alarmId")
            AddEditAlarmScreen(
                alarmId = alarmId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.AddEditPeriodReminder.route,
            arguments = listOf(navArgument("reminderId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString("reminderId")
            AddEditPeriodReminderScreen(
                reminderId = reminderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.GapList.route) {
            GapListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTopicDetail = { subjectId, topicId -> navController.navigate(Route.TopicDetail.create(subjectId, topicId)) }
            )
        }

        composable(Route.Profile.route) {
            ProfileScreen(
                onNavigateToEditProfile = { navController.navigate(Route.EditProfile.route) },
                onNavigateToChangePassword = { navController.navigate(Route.ChangePassword.route) },
                onNavigateToReferral = { navController.navigate(Route.Referral.route) },
                onNavigateToPlans = { navController.navigate(Route.Plans.route) },
                onNavigateToHelp = { navController.navigate(Route.Help.route) },
                onNavigateToSubscription = { navController.navigate(Route.Subscription.route) },
                onNavigateToContactSupport = { navController.navigate(Route.ContactSupport.route) },
                onNavigateToDeleteAccount = { navController.navigate(Route.DeleteAccount.route) },
                onLogout = {
                    navController.navigate(Route.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Route.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                authManager = authManager
            )
        }

        composable(Route.DeleteAccount.route) {
            DeleteAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.Plans.route) {
            PlanSelectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubscription = { navController.navigate(Route.Subscription.route) }
            )
        }

        composable(Route.Subscription.route) {
            SubscriptionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlans = { navController.navigate(Route.Plans.route) }
            )
        }

        composable(Route.Referral.route) {
            ReferralScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.Help.route) {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSearch = { navController.navigate(Route.FaqSearch.route) },
                onNavigateToCategory = { categoryId -> navController.navigate(Route.FaqCategory.create(categoryId)) }
            )
        }

        composable(
            route = Route.FaqCategory.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: return@composable
            FaqCategoryScreen(
                categoryId = categoryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.FaqSearch.route) {
            FaqSearchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.ContactSupport.route) {
            ContactSupportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.QuestionGenerator.route) {
            QuestionGeneratorScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Route.QuestionHistory.route) }
            )
        }

        composable(Route.QuestionHistory.route) {
            QuestionHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
