package com.decisionpro.app.ui/screens.matrix

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.decisionpro.app.domain.engine.matrix.MatrixResult
import com.decisionpro.app.ui.components.ConfidenceMeter
import com.decisionpro.app.ui.components.InputDialog

private val GutterWidth = 96.dp
private val CellWidth = 88.dp

@Composable
fun MatrixScreen(
    onNavigateBack: () -> Unit,
    viewModel: MatrixViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var scoreEdit by remember { mutableStateOf<Pair<String, String>?>(null) }
    var weightEdit by remember { mutableStateOf<String?>(null) }
    var showAddCriterion by remember { mutableStateOf(false) }
    var showAddOption by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
            Text(
                stringResource(R.string.matrix_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::setTitle,
            label = { Text("What are you deciding?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    Spacer(Modifier.width(GutterWidth))
                    state.criteria.forEach { criterion ->
                        Column(
                            Modifier.width(CellWidth),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                criterion.name,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                            AssistChip(
                                onClick = { weightEdit = criterion.id },
                                label = { Text("×%.0f".format(criterion.weight)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Tune, null, Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }
            }

            items(state.options.size) { index ->
                val option = state.options[index]
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        option.name,
                        Modifier.width(GutterWidth),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                    state.criteria.forEach { criterion ->
                        val score = option.scores[criterion.id]
                        ScoreCell(
                            score = score,
                            optionName = option.name,
                            criterionName = criterion.name,
                            onTap = { scoreEdit = option.id to criterion.id },
                            modifier = Modifier
                                .width(CellWidth)
                                .padding(2.dp)
                                .height(44.dp)
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showAddCriterion = true }) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.matrix_add_criterion))
                    }
                    OutlinedButton(onClick = { showAddOption = true }) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.matrix_add_option))
                    }
                }
            }

            if (state.results.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    ConfidenceMeter(confidence = state.confidence)
                }
                items(state.results.size) { index ->
                    ResultCard(result = state.results[index], rank = index + 1)
                }
            }
        }

        state.validationError?.let { error ->
            Text(
                "⚠️ $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Button(
            onClick = viewModel::analyze,
            enabled = state.isReady && !state.isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            if (state.isAnalyzing) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.results.isEmpty())
                        stringResource(R.string.matrix_analyze) else "Re-analyze"
                )
            }
        }
    }

    scoreEdit?.let { (optionId, criterionId) ->
        val option = state.options.firstOrNull { it.id == optionId } ?: return@let
        val criterion = state.criteria.firstOrNull { it.id == criterionId } ?: return@let
        ScoreEditorDialog(
            title = "${option.name} — ${criterion.name}",
            current = option.scores[criterionId] ?: 5f,
            onDismiss = { scoreEdit = null },
            onConfirm = { viewModel.setScore(optionId, criterionId, it); scoreEdit = null }
        )
    }
    weightEdit?.let { criterionId ->
        val criterion = state.criteria.firstOrNull { it.id == criterionId } ?: return@let
        ScoreEditorDialog(
            title = "Weight — ${criterion.name}",
            current = criterion.weight,
            onDismiss = { weightEdit = null },
            onConfirm = { viewModel.setWeight(criterionId, it); weightEdit = null }
        )
    }
    if (showAddCriterion) {
        InputDialog(
            title = stringResource(R.string.matrix_add_criterion),
            label = "Name",
            onDismiss = { showAddCriterion = false },
            onConfirm = { viewModel.addCriterion(it); showAddCriterion = false }
        )
    }
    if (showAddOption) {
        InputDialog(
            title = stringResource(R.string.matrix_add_option),
            label = "Name",
            onDismiss = { showAddOption = false },
            onConfirm = { viewModel.addOption(it); showAddOption = false }
        )
    }
}

@Composable
private fun ScoreCell(
    score: Float?,
    optionName: String,
    criterionName: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(10.dp),
        color = when {
            score == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            score >= 8f -> Color(0xFF2E7D32).copy(alpha = 0.85f)
            score >= 6f -> Color(0xFF9E9D24).copy(alpha = 0.85f)
            score >= 4f -> Color(0xFFFF8F00).copy(alpha = 0.85f)
            else -> Color(0xFFC62828).copy(alpha = 0.85f)
        },
        modifier = modifier.semantics {
            contentDescription = if (score != null)
                "$optionName scored ${score.toInt()} of 10 for $criterionName. Tap to change."
            else
                "$optionName not yet rated for $criterionName. Tap to rate."
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                score?.let { "%.0f".format(it) }
                    ?: stringResource(R.string.matrix_empty_score),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ScoreEditorDialog(
    title: String,
    current: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var value by remember { mutableFloatStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    "%.0f / 10".format(value),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..10f,
                    steps = 19
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ResultCard(result: MatrixResult, rank: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#$rank",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (rank == 1) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(result.optionName, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "%.0f%%".format(result.normalizedScore),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { result.normalizedScore / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
