package com.teacherscompanion.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class LocalAuthStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_LAST_ONLINE_MS = longPreferencesKey("last_online_ms")
        private val KEY_OFFLINE_SINCE_MS = longPreferencesKey("offline_since_ms")
        val MAX_OFFLINE_DURATION_MS = 7L * 24 * 60 * 60 * 1000
    }

    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }
    val lastOnlineMs: Flow<Long> = context.authDataStore.data.map { it[KEY_LAST_ONLINE_MS] ?: 0L }
    val offlineSinceMs: Flow<Long> = context.authDataStore.data.map { it[KEY_OFFLINE_SINCE_MS] ?: 0L }

    suspend fun setLoggedIn() {
        val now = System.currentTimeMillis()
        context.authDataStore.edit {
            it[KEY_IS_LOGGED_IN] = true
            it[KEY_LAST_ONLINE_MS] = now
            it.remove(KEY_OFFLINE_SINCE_MS)
        }
    }

    suspend fun setLoggedOut() {
        context.authDataStore.edit {
            it[KEY_IS_LOGGED_IN] = false
            it.remove(KEY_LAST_ONLINE_MS)
            it.remove(KEY_OFFLINE_SINCE_MS)
        }
    }

    suspend fun markOnline() {
        context.authDataStore.edit {
            it[KEY_LAST_ONLINE_MS] = System.currentTimeMillis()
            it.remove(KEY_OFFLINE_SINCE_MS)
        }
    }

    suspend fun markOffline() {
        context.authDataStore.edit {
            val existing = it[KEY_OFFLINE_SINCE_MS]
            if (existing == null || existing == 0L) {
                it[KEY_OFFLINE_SINCE_MS] = System.currentTimeMillis()
            }
        }
    }

    suspend fun isLoggedInBlocking(): Boolean = context.authDataStore.data.first()[KEY_IS_LOGGED_IN] ?: false

    suspend fun shouldForceLogout(): Boolean {
        val offlineSince = context.authDataStore.data.first()[KEY_OFFLINE_SINCE_MS] ?: 0L
        if (offlineSince == 0L) return false
        return System.currentTimeMillis() - offlineSince > MAX_OFFLINE_DURATION_MS
    }
}
