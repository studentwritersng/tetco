package com.teacherscompanion.ui.syllabus

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teacherscompanion.data.remote.dto.SyllabusTopicDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddTopic: () -> Unit,
    onNavigateToTopicDetail: (String) -> Unit,
    viewModel: SubjectDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(subjectId) { viewModel.loadTopics(subjectId) }
    val uiState by viewModel.uiState.collectAsState()
    var selectedTerm by remember { mutableIntStateOf(0) }
    val terms = listOf("All", "First Term", "Second Term", "Third Term")

    val filteredTopics = when (selectedTerm) {
        0 -> uiState.topics
        else -> uiState.topics.filter { it.term == terms[selectedTerm] }
    }

    var reorderedTopics by remember(filteredTopics) { mutableStateOf(filteredTopics) }
    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    var reorderJob by remember { mutableStateOf<Job?>(null) }

    val coveredCount = uiState.topics.count { it.has_lesson_note }
    val totalCount = uiState.topics.size
    val canReorder = selectedTerm != 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Syllabus", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddTopic) {
                Icon(Icons.Default.Add, contentDescription = "Add Topic")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
        ) {
            if (totalCount > 0) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$coveredCount of $totalCount topics have lesson notes", style = MaterialTheme.typography.bodyMedium)
                        Text("${if (totalCount > 0) (coveredCount * 100 / totalCount) else 0}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = if (totalCount > 0) coveredCount.toFloat() / totalCount else 0f,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTerm,
                modifier = Modifier.fillMaxWidth()
            ) {
                terms.forEachIndexed { index, term ->
                    Tab(
                        selected = selectedTerm == index,
                        onClick = { selectedTerm = index },
                        text = { Text(term, fontWeight = if (selectedTerm == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (reorderedTopics.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (uiState.topics.isEmpty()) "No topics yet. Start building your syllabus." else "No topics for this term yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    val grouped = reorderedTopics.groupBy { it.week_number }
                    grouped.forEach { (week, topics) ->
                        if (week != null) {
                            item {
                                Text("Week $week", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                            }
                        }
                        itemsIndexed(topics, key = { _, topic -> topic.id }) { index, topic ->
                            val globalIndex = reorderedTopics.indexOf(topic)
                            TopicRow(
                                topic = topic,
                                onClick = { onNavigateToTopicDetail(topic.id) },
                                showDragHandle = canReorder,
                                isDragging = dragIndex == globalIndex,
                                onDragStart = {
                                    dragIndex = globalIndex
                                    dragOffsetY = 0f
                                },
                                onDrag = { offset ->
                                    dragOffsetY += offset.y
                                    val from = dragIndex
                                    if (from >= 0) {
                                        val swapThreshold = 80f
                                        val newItems = reorderedTopics.toMutableList()
                                        val to = when {
                                            dragOffsetY > swapThreshold && from < newItems.lastIndex -> from + 1
                                            dragOffsetY < -swapThreshold && from > 0 -> from - 1
                                            else -> from
                                        }
                                        if (from != to) {
                                            val moved = newItems.removeAt(from)
                                            newItems.add(to, moved)
                                            reorderedTopics = newItems
                                            dragIndex = to
                                            dragOffsetY = 0f
                                            reorderJob?.cancel()
                                            reorderJob = scope.launch {
                                                delay(400)
                                                val orders = newItems.mapIndexed { i, t -> t.id to i }.toMap()
                                                viewModel.reorderTopics(orders)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    dragIndex = -1
                                    dragOffsetY = 0f
                                    reorderJob?.cancel()
                                    val orders = reorderedTopics.mapIndexed { i, t -> t.id to i }.toMap()
                                    viewModel.reorderTopics(orders)
                                },
                                onDragCancel = {
                                    dragIndex = -1
                                    dragOffsetY = 0f
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopicRow(
    topic: SyllabusTopicDto,
    onClick: () -> Unit,
    showDragHandle: Boolean = false,
    isDragging: Boolean = false,
    onDragStart: () -> Unit = {},
    onDrag: (offset: Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {}
) {
    val elevation = if (isDragging) 4.dp else 0.dp
    val containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

    Surface(
        tonalElevation = elevation,
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (showDragHandle) {
                        Modifier.pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStart() },
                                onDrag = { _, offset -> onDrag(offset) },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragCancel() }
                            )
                        }
                    } else Modifier
                )
                .padding(vertical = 4.dp)
        ) {
            if (topic.week_number != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "Wk ${topic.week_number}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(topic.title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
            }

            if (topic.has_lesson_note) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Has lesson note", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.Warning, contentDescription = "No lesson note", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }

            if (showDragHandle) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
