package com.teacherscompanion.ui.alarms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    alarmId: String?,
    onNavigateBack: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.hour,
        initialMinute = uiState.minute,
        is24Hour = false
    )
    var showTimePicker by remember { mutableStateOf(false) }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = remember { mutableStateListOf(true, true, true, true, true, false, false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == null) "Add Alarm" else "Edit Alarm", fontWeight = FontWeight.Bold) },
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
                value = uiState.label,
                onValueChange = viewModel::updateLabel,
                label = { Text("Label (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                val amPm = if (timePickerState.hour >= 12) "PM" else "AM"
                val displayHour = if (timePickerState.hour == 0) 12 else if (timePickerState.hour > 12) timePickerState.hour - 12 else timePickerState.hour
                Text("Time: ${String.format("%02d", displayHour)}:${String.format("%02d", timePickerState.minute)} $amPm", fontWeight = FontWeight.Medium)
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.updateTime(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("OK") }
                    },
                    text = { TimePicker(state = timePickerState) }
                )
            }

            Text("Repeat Days", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEachIndexed { index, day ->
                    FilterChip(
                        selected = selectedDays[index],
                        onClick = { selectedDays[index] = !selectedDays[index] },
                        label = { Text(day, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vibrate", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = uiState.vibrate, onCheckedChange = viewModel::updateVibrate)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val dayIndices = selectedDays.mapIndexedNotNull { index, selected -> if (selected) index + 1 else null }
                    viewModel.saveAlarm(dayIndices) { onNavigateBack() }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isSaving
            ) {
                Text(if (alarmId == null) "Add Alarm" else "Save Changes", fontWeight = FontWeight.Medium)
            }
        }
    }
}
