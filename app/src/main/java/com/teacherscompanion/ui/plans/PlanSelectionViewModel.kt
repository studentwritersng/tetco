package com.teacherscompanion.ui.plans

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.remote.dto.PlanDto
import com.teacherscompanion.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.invoke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanSelectionUiState(
    val plans: List<PlanDto> = emptyList(),
    val currentPlanName: String = "Basic",
    val isLoading: Boolean = false,
    val processingPlanId: String? = null,
    val error: String? = null
)

@HiltViewModel
class PlanSelectionViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanSelectionUiState())
    val uiState: StateFlow<PlanSelectionUiState> = _uiState.asStateFlow()

    init {
        loadPlans()
    }

    fun loadPlans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val plans = profileRepository.getPlans()
                val profile = profileRepository.getProfile()
                _uiState.value = PlanSelectionUiState(plans = plans, currentPlanName = profile.plan_name, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun initiatePayment(planId: String, onOpenUrl: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processingPlanId = planId)
            try {
                val response = supabaseClient.functions.invoke("create-payment-session") {
                    setBody(mapOf("plan_id" to planId))
                }
                val map = response as? Map<*, *>
                val authUrl = map?.get("authorization_url") as? String
                    ?: map?.get("data")?.let { (it as? Map<*, *>)?.get("authorization_url") as? String }
                    ?: throw Exception("No payment URL received")
                onOpenUrl(authUrl)
                _uiState.value = _uiState.value.copy(processingPlanId = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(processingPlanId = null, error = e.message)
            }
        }
    }
}
