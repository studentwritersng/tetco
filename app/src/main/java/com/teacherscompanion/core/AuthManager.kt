package com.teacherscompanion.core

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.data.session.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val localAuthStore: LocalAuthStore
) {
    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    suspend fun signInWithEmail(email: String, password: String) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        onLoginSuccess()
    }

    suspend fun signUpWithEmail(email: String, password: String, fullName: String) {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = mapOf("full_name" to fullName)
        }
        onLoginSuccess()
    }

    suspend fun signInWithGoogle(idToken: String) {
        supabaseClient.auth.signInWith(Google) {
            this.idToken = idToken
        }
        onLoginSuccess()
    }

    suspend fun refreshSession() {
        val session = supabaseClient.auth.currentSessionOrNull()
        _currentSession.value = session
        if (session != null) _isLoggedIn.value = true
    }

    suspend fun signOut() {
        try { supabaseClient.auth.signOut() } catch (_: Exception) { }
        _currentSession.value = null
        _isLoggedIn.value = false
        localAuthStore.setLoggedOut()
    }

    fun getCurrentUserId(): String? = supabaseClient.auth.currentSessionOrNull()?.user?.id
        ?: _currentSession.value?.user?.id

    suspend fun resetPassword(email: String) {
        supabaseClient.auth.resetPasswordForEmail(email)
    }

    suspend fun updatePassword(newPassword: String) {
        supabaseClient.auth.updateUser {
            this.password = newPassword
        }
    }

    private suspend fun onLoginSuccess() {
        refreshSession()
        localAuthStore.setLoggedIn()
    }

    init {
        scope.launch {
            val locallyLoggedIn = localAuthStore.isLoggedInBlocking()
            val forceLogout = localAuthStore.shouldForceLogout()

            if (forceLogout) {
                localAuthStore.setLoggedOut()
                _isLoggedIn.value = false
            } else if (locallyLoggedIn) {
                _isLoggedIn.value = true
                val session = supabaseClient.auth.currentSessionOrNull()
                _currentSession.value = session
            } else {
                val session = supabaseClient.auth.currentSessionOrNull()
                _currentSession.value = session
                _isLoggedIn.value = session != null
            }
        }
    }
}
