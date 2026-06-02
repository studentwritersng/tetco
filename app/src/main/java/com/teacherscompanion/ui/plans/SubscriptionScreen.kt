package com.teacherscompanion.ui.plans

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teacherscompanion.ui.components.AiUsageWidget
import com.teacherscompanion.ui.components.AiUsageData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(uiState.planName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (uiState.isFreePlan) {
                        Text("Free plan — no renewal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (uiState.renewalDate != null) {
                        Text("Renews ${uiState.renewalDate}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            AiUsageWidget(
                usage = AiUsageData(
                    lessonNotesUsed = uiState.lessonNotesUsed,
                    lessonNotesLimit = uiState.lessonNotesLimit,
                    questionsUsed = uiState.questionsUsed,
                    questionsLimit = uiState.questionsLimit,
                    teachingGuidesUsed = uiState.teachingGuidesUsed,
                    teachingGuidesLimit = uiState.teachingGuidesLimit,
                    month = uiState.month,
                    planName = uiState.planName
                ),
                onUpgradeClick = onNavigateToPlans
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!uiState.isFreePlan) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onNavigateToPlans,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Change Plan")
                    }
                    OutlinedButton(
                        onClick = { viewModel.cancelSubscription() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel")
                    }
                }
            } else {
                Button(
                    onClick = onNavigateToPlans,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upgrade Plan", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
