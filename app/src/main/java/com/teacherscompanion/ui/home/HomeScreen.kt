package com.teacherscompanion.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSchools: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToQuestionGenerator: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    // Color definitions based on ui_template.png
    val backgroundColor = Color(0xFFF8F9FD)
    val primaryColor = Color(0xFF3B82F6) // Accent indigo-blue
    val textMainColor = Color(0xFF1E293B)
    val textMutedColor = Color(0xFF64748B)

    Scaffold(
        modifier = Modifier.background(backgroundColor),
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Top Avatar and Custom Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Robot/Teacher Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2E8F0))
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Profile",
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Action buttons styled as circular soft boxes
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderActionIcon(Icons.Default.LightMode, onClick = {})
                    HeaderActionIcon(Icons.Default.HelpOutline, onClick = {})
                    HeaderActionIcon(Icons.Default.Person, onClick = onNavigateToProfile)
                    HeaderActionIcon(Icons.Default.Notifications, onClick = {}, badge = true)
                }
            }

            // 2. Greeting Header
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Good morning, ",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textMainColor,
                        fontSize = 26.sp
                    )
                    Text(
                        text = "👋",
                        fontSize = 24.sp
                    )
                }
                Text(
                    text = "${uiState.teacherName}  •  ${uiState.planName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textMutedColor,
                    fontWeight = FontWeight.Medium
                )
            }

            // 3. AI GENERATOR HUB (styled side-by-side cards from template)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AI GENERATOR HUB",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubGeneratorCard(
                        title = "Lesson Notes",
                        desc = "Generate notes",
                        icon = Icons.Default.Assignment,
                        iconBg = Color(0xFFEEF2FF),
                        iconColor = Color(0xFF4F46E5),
                        onClick = onNavigateToSchools // Takes them to school/syllabus to pick a topic for notes
                    )

                    HubGeneratorCard(
                        title = "MCQ Gen",
                        desc = "Generate MCQs",
                        icon = Icons.Default.Quiz,
                        iconBg = Color(0xFFECFDF5),
                        iconColor = Color(0xFF10B981),
                        onClick = onNavigateToQuestionGenerator
                    )

                    HubGeneratorCard(
                        title = "Essay Paper",
                        desc = "Generate essays",
                        icon = Icons.Default.Edit,
                        iconBg = Color(0xFFFFF1F2),
                        iconColor = Color(0xFFF43F5E),
                        onClick = onNavigateToQuestionGenerator
                    )
                }
            }

            // 4. Today's Schedule Card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textMainColor
                    )
                    Text(
                        text = "VIEW ALL",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.clickable { onNavigateToAlarms() }
                    )
                }

                if (uiState.nextAlarmTime != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(70.dp)
                            ) {
                                Text(
                                    text = uiState.nextAlarmTime!!.substringBefore(" "),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textMainColor
                                )
                                Text(
                                    text = uiState.nextAlarmTime!!.substringAfter(" "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textMutedColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Thin vertical separator line from template
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(Color(0xFFE2E8F0))
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.nextAlarmLabel ?: "Upcoming Alarm",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = textMainColor
                                )
                                Text(
                                    text = "Class Session reminder",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textMutedColor
                                )
                            }

                            IconButton(
                                onClick = onNavigateToAlarms,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No active alarms today.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textMutedColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onNavigateToAlarms,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add Alarm")
                            }
                        }
                    }
                }
            }

            // 5. Syllabus Progress Tracker Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LESSON NOTES COMPLETED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Syllabus Progress Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textMainColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${uiState.syllabusPercent}% of syllabus topics completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = textMutedColor
                        )
                    }

                    // Circular Progress gauge exactly like template (0%)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                        CircularProgressIndicator(
                            progress = uiState.syllabusPercent / 100f,
                            modifier = Modifier.fillMaxSize(),
                            color = primaryColor,
                            strokeWidth = 6.dp,
                            trackColor = Color(0xFFF1F5F9)
                        )
                        Text(
                            text = "${uiState.syllabusPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = textMainColor
                        )
                    }
                }
            }

            // 6. Pending Lesson Notes Tracker success box
            val allGenerated = uiState.uncoveredNotesCount == 0
            val successBoxColor = if (allGenerated) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
            val successBorderColor = if (allGenerated) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
            val successTextColor = if (allGenerated) Color(0xFF065F46) else Color(0xFF92400E)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = successBoxColor),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, successBorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (allGenerated) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (allGenerated) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (allGenerated) "All lesson notes generated." else "${uiState.uncoveredNotesCount} lesson notes still need to be generated.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = successTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun HeaderActionIcon(
    icon: ImageVector,
    onClick: () -> Unit,
    badge: Boolean = false
) {
    Box(contentAlignment = Alignment.TopEnd) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
        if (badge) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(8.dp)
                    .background(Color(0xFFEF4444), CircleShape)
            )
        }
    }
}

@Composable
fun HubGeneratorCard(
    title: String,
    desc: String,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}


