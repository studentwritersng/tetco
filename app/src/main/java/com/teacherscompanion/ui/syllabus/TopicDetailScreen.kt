package com.teacherscompanion.ui.syllabus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teacherscompanion.data.remote.dto.QuestionDto
import com.teacherscompanion.data.repository.TopicInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    subjectId: String,
    topicId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditNote: (String) -> Unit,
    onNavigateToTopic: (subjectId: String, topicId: String) -> Unit = { _, _ -> },
    viewModel: TopicDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(topicId) { viewModel.loadTopic(topicId) }
    val uiState by viewModel.uiState.collectAsState()
    var selectedQuestionTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.topicTitle.ifEmpty { "Topic" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (uiState.subjectName.isNotEmpty()) {
                            Text("${uiState.subjectName} · ${uiState.className}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.noteContent != null) {
                        IconButton(onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, uiState.topicTitle)
                                putExtra(android.content.Intent.EXTRA_TEXT, uiState.noteContent!!)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Lesson Note"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { onNavigateToEditNote(topicId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.topicTerm != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AssistChip(onClick = { }, label = { Text("${uiState.topicTerm} Term") })
                        if (uiState.topicWeek != null) {
                            AssistChip(onClick = { }, label = { Text("Week ${uiState.topicWeek}") })
                        }
                    }
                }

                HorizontalDivider()

                Text("LESSON NOTE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

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

                if (uiState.isGenerating) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Generating lesson note with AI...", style = MaterialTheme.typography.bodyMedium)
                            Text("This may take up to 30 seconds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (uiState.noteContent != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = uiState.noteContent!!,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No lesson note yet", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onNavigateToEditNote(topicId) }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Write Manually")
                                }
                                Button(onClick = { viewModel.showTopicPicker() }) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate with AI")
                                }
                            }
                        }
                    }
                }

                if (uiState.noteContent != null && uiState.planName != "basic") {
                    Button(
                        onClick = { viewModel.showTopicPicker() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Regenerate with AI", fontWeight = FontWeight.Medium)
                    }
                }

                if (uiState.isGeneratingGuide) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Generating teaching guide with AI...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else if (uiState.noteContent != null && uiState.teachingGuide == null && uiState.planName != "basic") {
                    OutlinedButton(
                        onClick = { viewModel.generateTeachingGuide(topicId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Teaching Guide", fontWeight = FontWeight.Medium)
                    }
                }

                if (uiState.teachingGuide != null) {
                    HorizontalDivider()
                    Text("TEACHING GUIDE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(text = uiState.teachingGuide!!, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
                    }
                }

                if (uiState.questions.isNotEmpty()) {
                    HorizontalDivider()
                    Text("QUESTIONS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    TabRow(selectedTabIndex = selectedQuestionTab) {
                        Tab(selected = selectedQuestionTab == 0, onClick = { selectedQuestionTab = 0 }, text = { Text("MCQ") })
                        Tab(selected = selectedQuestionTab == 1, onClick = { selectedQuestionTab = 1 }, text = { Text("Essay") })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val filteredQuestions = if (selectedQuestionTab == 0)
                        uiState.questions.filter { it.type == "mcq" }
                    else
                        uiState.questions.filter { it.type == "essay" }

                    filteredQuestions.forEachIndexed { index, question ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("${index + 1}. ${question.question_text}", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                if (question.type == "mcq" && question.options != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        question.options.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (question.correct_answer != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Answer: ${question.correct_answer}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (uiState.showTopicPicker) {
        var selectedId by remember { mutableStateOf(uiState.selectedTopicInfo?.topicId ?: topicId) }
        val groupedTopics = uiState.allTopics.groupBy { "${it.subjectName} · ${it.className}" }

        AlertDialog(
            onDismissRequest = { viewModel.hideTopicPicker() },
            title = {
                Text("Select Syllabus Topic", fontWeight = FontWeight.Bold)
            },
            text = {
                if (uiState.allTopics.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No syllabus topics found. Add topics first.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(400.dp)) {
                        groupedTopics.forEach { (groupLabel, topics) ->
                            item {
                                Text(
                                    groupLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(topics) { topic ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedId = topic.topicId },
                                    color = if (selectedId == topic.topicId) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (selectedId == topic.topicId) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (selectedId == topic.topicId) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                topic.topicTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (selectedId == topic.topicId) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${topic.term ?: ""} Term" + if (topic.weekNumber != null) " · Week ${topic.weekNumber}" else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selected = uiState.allTopics.find { it.topicId == selectedId }
                        if (selected != null) {
                            viewModel.selectTopic(selected)
                            viewModel.generateLessonNote { newTopicId ->
                                onNavigateToTopic(selected.subjectId, newTopicId)
                            }
                        }
                    },
                    enabled = uiState.allTopics.isNotEmpty()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideTopicPicker() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
