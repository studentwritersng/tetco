package com.teacherscompanion.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SubscriptionUiState(
    val planName: String = "Basic",
    val isFreePlan: Boolean = true,
    val renewalDate: String? = null,
    val lessonNotesUsed: Int = 0,
    val lessonNotesLimit: Int? = null,
    val questionsUsed: Int = 0,
    val questionsLimit: Int? = null,
    val teachingGuidesUsed: Int = 0,
    val teachingGuidesLimit: Int? = null,
    val month: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val profile = profileRepository.getProfile()
                val subscription = profileRepository.getSubscription()
                val usage = profileRepository.getAiUsage()
                val plans = profileRepository.getPlans()
                val currentPlan = plans.find { it.name.equals(profile.plan_name, ignoreCase = true) }

                val renewalDate = subscription?.current_period_end?.let { dateStr ->
                    try {
                        val date = java.time.OffsetDateTime.parse(dateStr).toLocalDate()
                        date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                    } catch (_: Exception) { dateStr }
                }

                val month = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))

                _uiState.value = SubscriptionUiState(
                    planName = profile.plan_name,
                    isFreePlan = profile.plan == "basic",
                    renewalDate = renewalDate,
                    lessonNotesUsed = usage?.lesson_notes_generated ?: 0,
                    lessonNotesLimit = currentPlan?.lesson_note_limit,
                    questionsUsed = usage?.questions_generated ?: 0,
                    questionsLimit = currentPlan?.mcq_limit?.let { it + (currentPlan.essay_limit ?: 0) },
                    teachingGuidesUsed = usage?.teaching_guides_generated ?: 0,
                    teachingGuidesLimit = currentPlan?.teaching_guide_limit,
                    month = month,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            try {
                val subscription = profileRepository.getSubscription()
                if (subscription != null) {
                    val cancelBody = buildJsonObject {
                        put("action", "cancel")
                        put("subscription_id", subscription.id)
                    }
                    supabaseClient.functions.invoke("cancel-subscription", body = cancelBody)
                }
                loadData()
            } catch (_: Exception) { }
        }
    }
}
