package com.teacherscompanion.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.core.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.invoke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeleteAccountUiState(
    val isDeleting: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false
)

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun deleteAccount(password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            try {
                supabaseClient.functions.invoke("delete-account") {
                    setBody(mapOf("password" to password))
                }
                authManager.signOut()
                _uiState.value = DeleteAccountUiState(isDeleted = true)
                onSuccess()
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to delete account"
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = if (msg.contains("INVALID_PASSWORD", ignoreCase = true)) "Incorrect password. Please try again." else msg
                )
            }
        }
    }
}
