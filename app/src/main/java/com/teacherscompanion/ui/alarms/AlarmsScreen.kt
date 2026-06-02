package com.teacherscompanion.ui.alarms

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    onNavigateToAddAlarm: () -> Unit,
    onNavigateToAddPeriodReminder: () -> Unit,
    onNavigateToGapList: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Alarms & Reminders", fontWeight = FontWeight.Bold) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Wake-Up Alarms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(onClick = onNavigateToAddAlarm, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Add Wake-Up Alarm", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }

            Text("Period Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(onClick = onNavigateToAddPeriodReminder, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Add Period Reminder", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }

            Text("Syllabus Gap Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(onClick = onNavigateToGapList, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Review Uncovered Topics", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
