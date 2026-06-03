package com.teacherscompanion.ui.referral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.remote.dto.ReferralHistoryDto
import com.teacherscompanion.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscountTier(
    val referralNumber: Int,
    val incrementalPercent: Int,
    val cumulativePercent: Int
)

data class ReferralUiState(
    val referralCode: String = "",
    val totalReferred: Int = 0,
    val qualified: Int = 0,
    val pending: Int = 0,
    val history: List<ReferralHistoryDto> = emptyList(),
    val discountPercent: Int = 0,
    val discountTiers: List<DiscountTier> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferralUiState())
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val profile = profileRepository.getProfile()
                val history = profileRepository.getReferralHistory()
                val qualified = history.count { it.qualified }
                val tiers = computeDiscountTiers(qualified)
                val discountPercent = computeDiscount(qualified)
                _uiState.value = ReferralUiState(
                    referralCode = profile.referral_code,
                    totalReferred = history.size,
                    qualified = qualified,
                    pending = history.size - qualified,
                    history = history,
                    discountPercent = discountPercent,
                    discountTiers = tiers,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun computeDiscountTiers(qualifiedCount: Int): List<DiscountTier> {
        val tiers = mutableListOf<DiscountTier>()
        var cumulative = 0
        for (i in 1..10) {
            val incremental = 11 - i
            cumulative += incremental
            tiers.add(DiscountTier(
                referralNumber = i,
                incrementalPercent = incremental,
                cumulativePercent = cumulative
            ))
        }
        return tiers
    }

    private fun computeDiscount(qualifiedCount: Int): Int {
        var discount = 0
        for (i in 1..minOf(qualifiedCount, 10)) {
            discount += (11 - i)
        }
        return minOf(discount, 55)
    }
}
