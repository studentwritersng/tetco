package com.teacherscompanion.ui.questions

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionGeneratorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: QuestionGeneratorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val typeLabel = if (uiState.questionType == "mcq") "MCQ" else "Theory"
    val countRange = if (uiState.questionType == "mcq") 10..50 else 3..10

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Question Generator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("1. Select Class", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            var classExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = classExpanded, onExpandedChange = { classExpanded = it }) {
                OutlinedTextField(
                    value = uiState.selectedClassLabel.ifEmpty { "Select a class" },
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) }
                )
                ExposedDropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                    uiState.classes.forEach { classItem ->
                        DropdownMenuItem(
                            text = { Text(classItem.label) },
                            onClick = {
                                viewModel.selectClass(classItem.id)
                                classExpanded = false
                            }
                        )
                    }
                }
            }

            if (uiState.subjects.isNotEmpty()) {
                Text("2. Select Subject", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                var subjectExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = subjectExpanded, onExpandedChange = { subjectExpanded = it }) {
                    OutlinedTextField(
                        value = uiState.selectedSubjectName.ifEmpty { "Select a subject" },
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) }
                    )
                    ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                        uiState.subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject.name) },
                                onClick = {
                                    viewModel.selectSubject(subject.id)
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.selectedSubjectId.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("3. Question Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.questionType == "mcq",
                        onClick = { viewModel.setQuestionType("mcq") },
                        label = { Text("MCQ (10-50)") }
                    )
                    FilterChip(
                        selected = uiState.questionType == "essay",
                        onClick = { viewModel.setQuestionType("essay") },
                        label = { Text("Theory (3-10)") }
                    )
                }

                Text("4. Number of Questions: ${uiState.questionCount}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = uiState.questionCount.toFloat(),
                    onValueChange = { viewModel.setQuestionCount(it.toInt()) },
                    valueRange = countRange.first.toFloat()..countRange.last.toFloat(),
                    steps = countRange.last - countRange.first - 1,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${countRange.first}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${countRange.last}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text("5. Week Range", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = uiState.weekStart.toString(),
                        onValueChange = { it.toIntOrNull()?.let { w -> if (w in uiState.minWeek..uiState.maxWeek) viewModel.setWeekStart(w) } },
                        label = { Text("Start Week") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.weekEnd.toString(),
                        onValueChange = { it.toIntOrNull()?.let { w -> if (w in uiState.minWeek..uiState.maxWeek) viewModel.setWeekEnd(w) } },
                        label = { Text("End Week") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("6. Difficulty", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = uiState.difficulty == "easy",
                        onClick = { viewModel.setDifficulty("easy") },
                        label = { Text("Easy") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.difficulty == "medium",
                        onClick = { viewModel.setDifficulty("medium") },
                        label = { Text("Medium") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.difficulty == "hard",
                        onClick = { viewModel.setDifficulty("hard") },
                        label = { Text("Hard") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.difficulty == "30-30-40",
                        onClick = { viewModel.setDifficulty("30-30-40") },
                        label = { Text("Mixed") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.questionType == "mcq") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Format Distribution", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("• 50% Direct questions", style = MaterialTheme.typography.bodySmall)
                            Text("• 50% Mixed: fill gaps, odd one out, negative, best answer, matching, sequence, situation, multi-statement", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(uiState.error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                Button(
                    onClick = { viewModel.generate() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !uiState.isGenerating && uiState.selectedSubjectId.isNotEmpty()
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Generating $typeLabel Questions...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate $typeLabel Questions", fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (uiState.formattedText != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RESULT", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    FilledTonalButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "${uiState.selectedSubjectName} ${typeLabel} Questions")
                            putExtra(Intent.EXTRA_TEXT, uiState.formattedText!!)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Questions"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share / Export")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = uiState.formattedText!!,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
