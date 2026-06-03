package com.teacherscompanion.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.local.dao.FaqDao
import com.teacherscompanion.data.local.entity.FaqItemCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HelpUiState(
    val faqItems: List<FaqItemCache> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val faqDao: FaqDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HelpUiState())
    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = HelpUiState(isLoading = true)
            try {
                val items = faqDao.getAll()
                _uiState.value = HelpUiState(faqItems = items, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = HelpUiState(isLoading = false)
            }
        }
    }
}
