package com.teacherscompanion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AiUsageData(
    val lessonNotesUsed: Int = 0,
    val lessonNotesLimit: Int? = null,
    val questionsUsed: Int = 0,
    val questionsLimit: Int? = null,
    val teachingGuidesUsed: Int = 0,
    val teachingGuidesLimit: Int? = null,
    val month: String = "",
    val planName: String = "Basic"
)

@Composable
fun AiUsageWidget(
    usage: AiUsageData,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = usage.planName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (usage.planName != "Premium") {
                    FilledTonalButton(onClick = onUpgradeClick, contentPadding = ButtonDefaults.ContentPadding) {
                        Text("Upgrade", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (usage.planName == "Basic") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No AI features on your current plan. Upgrade to access AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                if (usage.month.isNotEmpty()) {
                    Text(
                        text = "AI Usage — ${usage.month}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                UsageRow("Lesson Notes", usage.lessonNotesUsed, usage.lessonNotesLimit)
                UsageRow("Questions", usage.questionsUsed, usage.questionsLimit)
                UsageRow("Teaching Guides", usage.teachingGuidesUsed, usage.teachingGuidesLimit)
            }
        }
    }
}

@Composable
fun UsageRow(label: String, used: Int, limit: Int?) {
    val displayLimit = limit ?: 999999
    val progress = if (displayLimit > 0) used.toFloat() / displayLimit else 0f
    val cappedProgress = progress.coerceIn(0f, 1f)
    val color = when {
        progress >= 1f -> MaterialTheme.colorScheme.error
        progress >= 0.8f -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }
    val limitText = if (limit == null || limit >= 999999) "Unlimited" else limit.toString()

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        LinearProgressIndicator(
            progress = cappedProgress,
            modifier = Modifier.width(80.dp).height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("$used/$limitText", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}
