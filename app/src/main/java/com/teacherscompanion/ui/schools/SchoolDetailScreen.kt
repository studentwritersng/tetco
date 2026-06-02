package com.teacherscompanion.ui.schools

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolDetailScreen(
    schoolId: String,
    onNavigateBack: () -> Unit,
    onNavigateToClassDetail: (String) -> Unit,
    viewModel: SchoolDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(schoolId) { viewModel.loadSchool(schoolId) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.school?.name ?: "School", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddClassSheet() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Class")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text("${uiState.classes.size} Classes") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.classes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No classes yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Add a class from the Nigerian standard", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    uiState.classes.forEach { classItem ->
                        ElevatedCard(
                            onClick = { onNavigateToClassDetail(classItem.id) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(classItem.class_name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                    Text(classItem.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${classItem.subject_count} subjects", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddingClass) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideAddClassSheet() }
        ) {
            AddClassSheet(
                classLevels = uiState.classLevels,
                existingClassLevelIds = uiState.classes.map { it.class_level_id }.toSet(),
                onClassSelected = { classLevelId -> viewModel.addClassToSchool(schoolId, classLevelId) },
                onDismiss = { viewModel.hideAddClassSheet() }
            )
        }
    }
}
