package com.decisionpro.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownhillSkiing
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionpro.app.R
import com.decisionpro.app.ui.components.ToolCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenMatrix: () -> Unit,
    onOpenTree: () -> Unit,
    onOpenMarbleRun: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.analytics.totalCount > 0) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text("${state.analytics.totalCount} decisions") },
                            leadingIcon = {
                                Icon(Icons.Default.History, null, Modifier.size(16.dp))
                            }
                        )
                        state.analytics.mostFrequentChoice?.let { choice ->
                            AssistChip(
                                onClick = {},
                                label = { Text("Most chosen: ${choice.take(12)}") }
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolCard(
                        title = stringResource(R.string.tool_matrix_title),
                        subtitle = stringResource(R.string.tool_matrix_subtitle),
                        icon = Icons.Default.GridOn,
                        gradient = listOf(Color(0xFF7B1FA2), Color(0xFFBA68C8)),
                        onClick = onOpenMatrix,
                        modifier = Modifier.weight(1f)
                    )
                    ToolCard(
                        title = stringResource(R.string.tool_tree_title),
                        subtitle = stringResource(R.string.tool_tree_subtitle),
                        icon = Icons.AutoMirrored.Filled.CallSplit,
                        gradient = listOf(Color(0xFF1565C0), Color(0xFF42A5F5)),
                        onClick = onOpenTree,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                ToolCard(
                    title = stringResource(R.string.tool_marble_title),
                    subtitle = stringResource(R.string.tool_marble_subtitle),
                    icon = Icons.Default.DownhillSkiing,
                    gradient = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
                    onClick = onOpenMarbleRun,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    stringResource(R.string.home_recent),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.history.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        stringResource(R.string.home_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                }
            }

            items(state.history, key = { it.id }) { record ->
                val dateText = remember(record.timestamp) {
                    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                        .format(Date(record.timestamp))
                }
                ListItem(
                    headlineContent = {
                        Text(record.choice, fontWeight = FontWeight.SemiBold)
                    },
                    supportingContent = {
                        Text("${record.question.take(50)} · $dateText")
                    },
                    leadingContent = {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (record.type) {
                                "matrix" -> MaterialTheme.colorScheme.primaryContainer
                                "tree" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                        ) {
                            Text(
                                record.type.take(2).uppercase(),
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                viewModel.delete(record.id)
                                scope.launch {
                                    val result = snackbar.showSnackbar(
                                        "Deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Delete decision"
                            }
                        ) {
                            Icon(
                                Icons.Default.Close, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }
}
