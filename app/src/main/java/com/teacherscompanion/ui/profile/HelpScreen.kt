package com.teacherscompanion.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teacherscompanion.data.local.entity.FaqItemCache

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    viewModel: HelpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & FAQ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val categories = uiState.faqItems.map { it.categoryName to it.categoryId }.distinct()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height((categories.size / 2 + categories.size % 2) * 80.dp)
            ) {
                items(categories) { (name, id) ->
                    OutlinedCard(onClick = { onNavigateToCategory(id) }, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Text("Popular Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val popular = uiState.faqItems.sortedBy { it.displayOrder }.take(4)
            popular.forEach { item ->
                ListItem(
                    headlineContent = { Text(item.question, style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.fillMaxWidth().clickable { }
                )
                HorizontalDivider()
            }
        }
    }
}
