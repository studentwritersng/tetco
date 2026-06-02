package com.teacherscompanion.ui.syllabus

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTopicScreen(
    subjectId: String,
    topicId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditTopicViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val terms = listOf("First", "Second", "Third")
    var termExpanded by remember { mutableStateOf(false) }
    var weekExpanded by remember { mutableStateOf(false) }
    val weeks = (1..14).toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (topicId == null) "Add Topic" else "Edit Topic", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Topic Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } }
            )

            ExposedDropdownMenuBox(
                expanded = termExpanded,
                onExpandedChange = { termExpanded = !termExpanded }
            ) {
                OutlinedTextField(
                    value = if (uiState.term.isNotEmpty()) "${uiState.term} Term" else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Term") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = termExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = termExpanded,
                    onDismissRequest = { termExpanded = false }
                ) {
                    terms.forEach { term ->
                        DropdownMenuItem(
                            text = { Text("$term Term") },
                            onClick = { viewModel.updateTerm(term); termExpanded = false }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = weekExpanded,
                onExpandedChange = { weekExpanded = !weekExpanded }
            ) {
                OutlinedTextField(
                    value = if (uiState.weekNumber > 0) "Week ${uiState.weekNumber}" else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Week Number") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = weekExpanded,
                    onDismissRequest = { weekExpanded = false }
                ) {
                    weeks.forEach { week ->
                        DropdownMenuItem(
                            text = { Text("Week $week") },
                            onClick = { viewModel.updateWeek(week); weekExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.saveTopic(subjectId) { onNavigateBack() } },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isSaving && uiState.title.isNotBlank() && uiState.term.isNotEmpty() && uiState.weekNumber > 0
            ) {
                Text(if (topicId == null) "Add Topic" else "Save Changes", fontWeight = FontWeight.Medium)
            }
        }
    }
}
