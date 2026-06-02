package com.teacherscompanion.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.browser.customtabs.CustomTabsIntent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teacherscompanion.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileLiveState(
    val fullName: String = "Teacher",
    val planName: String = "Basic Plan",
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileLiveViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileLiveState())
    val state: StateFlow<ProfileLiveState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val profile = profileRepository.getProfile()
                _state.value = ProfileLiveState(
                    fullName = profile.full_name ?: "Teacher",
                    planName = profile.plan_name,
                    isLoading = false
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToReferral: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToContactSupport: () -> Unit = {},
    onNavigateToDeleteAccount: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: ProfileLiveViewModel = hiltViewModel()
) {
    val profileState by viewModel.state.collectAsState()
    val context = LocalContext.current

    fun openUrl(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profile", fontWeight = FontWeight.Bold) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        if (profileState.isLoading) {
                            Text("Loading...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        } else {
                            Text(profileState.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(profileState.planName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            }

            Text("Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            ListItem(
                headlineContent = { Text("Edit Profile") },
                leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToEditProfile() }
            )
            ListItem(
                headlineContent = { Text("Change Password") },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToChangePassword() }
            )
            ListItem(
                headlineContent = { Text("Refer a Friend") },
                leadingContent = { Icon(Icons.Default.CardGiftcard, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToReferral() }
            )
            ListItem(
                headlineContent = { Text("Manage Subscription") },
                leadingContent = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToSubscription() }
            )

            Text("Support", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            ListItem(
                headlineContent = { Text("Help & FAQ") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToHelp() }
            )
            ListItem(
                headlineContent = { Text("Contact Support") },
                leadingContent = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToContactSupport() }
            )

            Text("App Info", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            ListItem(
                headlineContent = { Text("Privacy Policy") },
                leadingContent = { Icon(Icons.Default.Policy, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable {
                    openUrl("https://teacherscompanion.app/privacy")
                }
            )
            ListItem(
                headlineContent = { Text("Terms of Service") },
                leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable {
                    openUrl("https://teacherscompanion.app/terms")
                }
            )
            ListItem(
                headlineContent = { Text("App Version: 1.0.0") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
            TextButton(onClick = onNavigateToDeleteAccount, modifier = Modifier.fillMaxWidth()) {
                Text("Delete Account", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
        }
    }
}
