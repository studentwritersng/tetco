package com.teacherscompanion.ui.syllabus

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLessonNoteScreen(
    topicId: String,
    onNavigateBack: () -> Unit,
    viewModel: EditLessonNoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var content by remember { mutableStateOf("") }
    var saveStatus by remember { mutableStateOf("Saved") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(topicId) {
        viewModel.loadNote(topicId)
    }

    LaunchedEffect(uiState.noteContent) {
        if (uiState.noteContent != null && content.isEmpty()) {
            content = uiState.noteContent!!
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Edit Note", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(saveStatus, style = MaterialTheme.typography.bodySmall, color = if (saveStatus == "Saved") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { }) { Icon(Icons.Default.FormatBold, contentDescription = "Bold") }
                    IconButton(onClick = { }) { Icon(Icons.Default.FormatItalic, contentDescription = "Italic") }
                    IconButton(onClick = { }) { Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List") }
                    IconButton(onClick = { }) { Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List") }
                }
            }
        }
    ) { paddingValues ->
        OutlinedTextField(
            value = content,
            onValueChange = { newContent ->
                content = newContent
                saveStatus = "Saving..."
                scope.launch {
                    delay(1500)
                    viewModel.saveNote(topicId, content) { success ->
                        saveStatus = if (success) "Saved" else "Error saving"
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            placeholder = { Text("Start writing your lesson note...") },
            maxLines = Int.MAX_VALUE
        )
    }
}
