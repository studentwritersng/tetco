package com.teacherscompanion.ui.plans

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teacherscompanion.data.remote.dto.PlanDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSelectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    viewModel: PlanSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadPlans() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Your Plan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Prices in Nigerian Naira (NGN). Billed monthly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                uiState.plans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        isCurrentPlan = plan.name.equals(uiState.currentPlanName, ignoreCase = true),
                        onSubscribe = {
                            viewModel.initiatePayment(plan.id) { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                        isProcessing = uiState.processingPlanId == plan.id
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: PlanDto,
    isCurrentPlan: Boolean,
    onSubscribe: () -> Unit,
    isProcessing: Boolean
) {
    val isBasic = plan.is_free
    val cardColors = if (plan.name == "Premium") {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(plan.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (isBasic) {
                        Text("Free forever", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("₦${plan.price_ngn}/month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isCurrentPlan) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text("Current Plan", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            val lessonLimit = plan.lesson_note_limit
            val mcqLimit = plan.mcq_limit
            val essayLimit = plan.essay_limit
            val guideLimit = plan.teaching_guide_limit

            FeatureRow("Schools (up to 10)", true)
            FeatureRow("Classes & Subjects", true)
            FeatureRow("Syllabus Topics", true)
            FeatureRow("Lesson Notes (manual)", true)
            FeatureRow("Alarms & Reminders", true)
            FeatureRow("AI Lesson Notes", lessonLimit != null && lessonLimit > 0, if (lessonLimit != null && lessonLimit > 0) "${lessonLimit}/mo" else null)
            FeatureRow("AI MCQ Generation", mcqLimit != null && mcqLimit > 0, if (mcqLimit != null && mcqLimit > 0) "${mcqLimit}/mo" else null)
            FeatureRow("AI Essay Generation", essayLimit != null && essayLimit > 0, if (essayLimit != null && essayLimit > 0) "${essayLimit}/mo" else null)
            FeatureRow("AI Teaching Guide", guideLimit != null && guideLimit > 0, if (guideLimit != null && guideLimit > 0) "${guideLimit}/mo" else null)
            FeatureRow("Referral Rewards", true)

            Spacer(modifier = Modifier.height(16.dp))

            if (isCurrentPlan) {
                OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = false) {
                    Text("Current Plan")
                }
            } else if (isBasic) {
                OutlinedButton(onClick = onSubscribe, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Switch to Free")
                }
            } else {
                Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = !isProcessing) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Subscribe", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureRow(label: String, included: Boolean, detail: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (included) "✓" else "✗",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (included) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (included) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
