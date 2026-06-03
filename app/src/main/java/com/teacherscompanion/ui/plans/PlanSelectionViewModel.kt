package com.teacherscompanion.ui.plans

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.remote.dto.PlanDto
import com.teacherscompanion.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

data class PlanSelectionUiState(
    val plans: List<PlanDto> = emptyList(),
    val currentPlanName: String = "Basic",
    val isLoading: Boolean = false,
    val processingPlanId: String? = null,
    val error: String? = null,
    val couponCode: String = "",
    val couponMessage: String? = null,
    val couponError: String? = null,
    val couponDiscountPercent: Int? = null,
    val couponName: String? = null
)

@HiltViewModel
class PlanSelectionViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
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

    fun updateCouponCode(code: String) {
        _uiState.value = _uiState.value.copy(couponCode = code, couponMessage = null, couponError = null, couponDiscountPercent = null, couponName = null)
    }

    fun validateCoupon() {
        val code = _uiState.value.couponCode.trim()
        if (code.isEmpty()) {
            _uiState.value = _uiState.value.copy(couponMessage = null, couponError = null, couponDiscountPercent = null, couponName = null)
            return
        }
        viewModelScope.launch {
            try {
                val response = supabaseClient.from("coupons")
                    .select { filter { eq("code", code.uppercase()) } }
                    .decodeSingle<JsonObject>()
                val isActive = response["is_active"]?.jsonPrimitive?.boolean ?: false
                val maxUses = response["max_uses"]?.jsonPrimitive?.int ?: 0
                val currentUses = response["current_uses"]?.jsonPrimitive?.int ?: 0
                val expiresAt = response["expires_at"]?.jsonPrimitive?.contentOrNull
                val discountPercent = response["discount_percent"]?.jsonPrimitive?.int ?: 0
                val name = response["name"]?.jsonPrimitive?.contentOrNull ?: code.uppercase()

                if (!isActive) {
                    _uiState.value = _uiState.value.copy(couponError = "This coupon is no longer active", couponDiscountPercent = null, couponName = null)
                    return@launch
                }
                if (maxUses > 0 && currentUses >= maxUses) {
                    _uiState.value = _uiState.value.copy(couponError = "This coupon has reached its usage limit", couponDiscountPercent = null, couponName = null)
                    return@launch
                }
                if (!expiresAt.isNullOrEmpty()) {
                    val expiryDate = java.time.Instant.parse(expiresAt)
                    if (java.time.Instant.now().isAfter(expiryDate)) {
                        _uiState.value = _uiState.value.copy(couponError = "This coupon has expired", couponDiscountPercent = null, couponName = null)
                        return@launch
                    }
                }
                _uiState.value = _uiState.value.copy(
                    couponMessage = "$discountPercent% discount applied",
                    couponDiscountPercent = discountPercent,
                    couponName = name,
                    couponError = null
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(couponError = "Invalid coupon code", couponDiscountPercent = null, couponName = null)
            }
        }
    }

    fun initiatePayment(planId: String, onOpenUrl: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processingPlanId = planId)
            try {
                val bodyBuilder = buildJsonObject {
                    put("plan_id", planId)
                    val couponCode = _uiState.value.couponCode.trim()
                    if (couponCode.isNotEmpty()) {
                        put("coupon_code", couponCode.uppercase())
                    }
                }
                val httpResponse = supabaseClient.functions.invoke("create-payment-session", body = bodyBuilder)
                val responseString = httpResponse.bodyAsText()
                val responseJson = json.parseToJsonElement(responseString).jsonObject
                val authUrl = responseJson["authorization_url"]?.jsonPrimitive?.content
                    ?: responseJson["data"]?.jsonObject?.let { it["authorization_url"]?.jsonPrimitive?.content }
                    ?: throw Exception("No payment URL received")
                onOpenUrl(authUrl)
                _uiState.value = _uiState.value.copy(processingPlanId = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(processingPlanId = null, error = e.message)
            }
        }
    }
}
