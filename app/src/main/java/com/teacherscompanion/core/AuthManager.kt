package com.teacherscompanion.core

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val localAuthStore: LocalAuthStore
) {
    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession: StateFlow<UserSession?> = _currentSession

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    suspend fun signInWithEmail(email: String, password: String) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        checkSuspension()
        onLoginSuccess()
    }

    private suspend fun checkSuspension() {
        try {
            val userId = getCurrentUserId() ?: return
            val profile = decodeSingleProfile(userId) ?: return
            val suspendedAt = profile["suspended_at"]?.jsonPrimitive?.contentOrNull
            if (!suspendedAt.isNullOrEmpty()) {
                val reason = profile["suspended_reason"]?.jsonPrimitive?.contentOrNull ?: "No reason provided"
                signOut()
                throw SuspendedAccountException(reason)
            }
        } catch (e: SuspendedAccountException) {
            throw e
        } catch (_: Exception) {
            // Non-fatal — continue
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, fullName: String) {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("full_name", fullName) }
        }
        onLoginSuccess()
    }

    suspend fun signInWithGoogle(idToken: String) {
        supabaseClient.auth.signInWith(IDToken) {
            provider = Google
            this.idToken = idToken
        }
        checkSuspension()
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
        supabaseClient.auth.modifyUser {
            password = newPassword
        }
    }

    private suspend fun onLoginSuccess() {
        refreshSession()
        localAuthStore.setLoggedIn()
    }

    private suspend fun decodeSingleProfile(userId: String) = try {
        supabaseClient.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingle<JsonObject>()
    } catch (_: Exception) { null }

    init {
        scope.launch {
            val locallyLoggedIn = localAuthStore.isLoggedInBlocking()
            val forceLogout = localAuthStore.shouldForceLogout()

            if (forceLogout) {
                localAuthStore.setLoggedOut()
                _isLoggedIn.value = false
            } else if (locallyLoggedIn) {
                val session = supabaseClient.auth.currentSessionOrNull()
                if (session != null) {
                    val userId = session.user?.id ?: run {
                        _currentSession.value = session
                        _isLoggedIn.value = true
                        return@launch
                    }
                    val profile = decodeSingleProfile(userId)
                    val suspended = profile?.get("suspended_at")?.jsonPrimitive?.contentOrNull
                    if (!suspended.isNullOrEmpty()) {
                        signOut()
                    } else {
                        _currentSession.value = session
                        _isLoggedIn.value = true
                    }
                }
            } else {
                val session = supabaseClient.auth.currentSessionOrNull()
                _currentSession.value = session
                _isLoggedIn.value = session != null
            }
        }
    }
}

class SuspendedAccountException(val reason: String) : Exception("Your account has been suspended: $reason")
