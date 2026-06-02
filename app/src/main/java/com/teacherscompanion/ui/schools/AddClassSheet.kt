package com.teacherscompanion.ui.schools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teacherscompanion.data.remote.dto.ClassLevelDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassSheet(
    classLevels: List<ClassLevelDto>,
    existingClassLevelIds: Set<Int>,
    onClassSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val groupedLevels = classLevels.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Add Class",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            groupedLevels.forEach { (category, levels) ->
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(levels, key = { it.id }) { level ->
                    val alreadyAdded = level.id in existingClassLevelIds
                    ListItem(
                        headlineContent = {
                            Text(
                                level.name,
                                color = if (alreadyAdded) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = if (alreadyAdded) { { Text("Added", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } else null
                    )
                    if (!alreadyAdded) {
                        HorizontalDivider()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}
