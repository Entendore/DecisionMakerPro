package com.decisionpro.app.ui.screens.marblerun

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionpro.app.R
import com.decisionpro.app.domain.engine.marblerun.BoardFactory
import com.decisionpro.app.domain.engine.marblerun.BoardPreset
import com.decisionpro.app.domain.engine.marblerun.MarblePalette
import com.decisionpro.app.ui.screens.marblerun.MarbleRunViewModel.Phase

@Composable
fun MarbleRunScreen(
    onNavigateBack: () -> Unit,
    viewModel: MarbleRunViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val frame by viewModel.renderFrame.collectAsStateWithLifecycle()

    // Battery safety: physics follows the app lifecycle
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pause()
                Lifecycle.Event.ON_RESUME -> viewModel.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var boardSize by remember { mutableStateOf(IntSize.Zero) }

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
                stringResource(R.string.marble_run_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (state.seed != 0L) {
                Text("Seed: ${state.seed}", style = MaterialTheme.typography.labelSmall)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BoardPreset.values()) { preset ->
                FilterChip(
                    selected = state.preset == preset,
                    onClick = { viewModel.setPreset(preset) },
                    label = { Text("${preset.emoji} ${preset.displayName}") }
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.marble_run_best_of),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = state.bestOfThree, onCheckedChange = {
                viewModel.toggleBestOfThree()
            })
        }
        if (state.bestOfThree && state.roundWins.isNotEmpty()) {
            Text(
                "Round ${state.round}/3 · " +
                    state.roundWins.entries.joinToString(" · ") { "${it.key}: ${it.value}" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        frame?.let { f ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                f.marbles.forEach { marble ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(MarblePalette.forIndex(marble.id)),
                            modifier = Modifier.size(14.dp)
                        ) {}
                        Text(
                            state.options.getOrNull(marble.id)?.take(8) ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        MarbleBoard(
            state = state,
            frame = frame,
            onSize = { boardSize = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(Modifier.height(12.dp))

        when (state.phase) {
            Phase.SETUP -> OptionsEditor(viewModel, state) {
                viewModel.startRace(
                    boardSize.width.toFloat(), boardSize.height.toFloat()
                )
            }
            Phase.COUNTDOWN, Phase.RACING -> Unit
            Phase.FINISHED -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.startRace(
                                boardSize.width.toFloat(), boardSize.height.toFloat()
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.marble_race_again))
                    }
                    OutlinedButton(
                        onClick = viewModel::reset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Edit options")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MarbleBoard(
    state: MarbleRunViewModel.UiState,
    frame: PhysicsOrchestrator.RenderFrame?,
    onSize: (IntSize) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val width = size.width.toFloat()
    val height = size.height.toFloat()

    // Determinism payoff: board = pure function of (preset, size, seed).
    // Render and simulation reconstruct the IDENTICAL board from the same
    // seed — no board state to drift or persist.
    val board = remember(state.preset, state.seed, width, height, state.options.size) {
        if (state.seed == 0L || width <= 0f || height <= 0f) null
        else BoardFactory.build(
            state.preset, width, height, state.options.size, state.seed
        )
    }

    Box(modifier.clip(RoundedCornerShape(16.dp))) {
        Canvas(Modifier.fillMaxSize().onSizeChanged {
            size = it
            onSize(it)
        }) {
            drawRect(Color(0xFF14141F))

            val b = board ?: return@Canvas

            drawLine(
                color = Color(0xFF4CAF50).copy(alpha = 0.5f),
                start = Offset(0f, b.finishLineY),
                end = Offset(size.width, b.finishLineY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 12f))
            )

            b.walls.forEach { wall ->
                drawLine(
                    Color(0xFF546E7A),
                    Offset(wall.x1, wall.y1),
                    Offset(wall.x2, wall.y2),
                    strokeWidth = 8f
                )
            }

            b.pegs.forEach { peg ->
                drawCircle(Color(0xFF90A4AE), peg.radius, Offset(peg.x, peg.y))
                drawCircle(
                    Color(0xFFB0BEC5), peg.radius * 0.45f,
                    Offset(peg.x - peg.radius * 0.25f, peg.y - peg.radius * 0.25f)
                )
            }

            frame?.marbles?.forEach { m ->
                val color = Color(MarblePalette.forIndex(m.id))
                m.trail.forEachIndexed { i, point ->
                    val alpha = (i.toFloat() / m.trail.size) * 0.25f
                    drawCircle(
                        color.copy(alpha = alpha),
                        radius = 18f * (0.4f + 0.5f * i / m.trail.size),
                        center = Offset(point.x, point.y)
                    )
                }
                drawCircle(color, 18f, Offset(m.x, m.y))
                drawCircle(
                    Color.White.copy(alpha = 0.45f), 18f * 0.35f,
                    Offset(m.x - 5.4f, m.y - 6.3f)
                )
                if (m.finished) {
                    drawCircle(Color.White, 10f, Offset(m.x, m.y))
                }
            }
        }

        when (state.phase) {
            Phase.COUNTDOWN -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "${state.countdown}",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
            Phase.SETUP -> {
                if (state.seed == 0L) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.marble_board_hint),
                            color = Color(0xFF90A4AE),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
            Phase.FINISHED -> state.winner?.let { ConfettiOverlay(it) }
            Phase.RACING -> Unit
        }
    }
}

@Composable
private fun OptionsEditor(
    viewModel: MarbleRunViewModel,
    state: MarbleRunViewModel.UiState,
    onStart: () -> Unit
) {
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        state.options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = option,
                    onValueChange = { new ->
                        viewModel.updateOptions(
                            state.options.mapIndexed { i, o -> if (i == index) new else o }
                        )
                    },
                    label = { Text("Option ${index + 1}") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (state.options.size > 2) {
                    IconButton(onClick = {
                        viewModel.updateOptions(
                            state.options.filterIndexed { i, _ -> i != index }
                        )
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            stringResource(R.string.cd_delete_decision)
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                viewModel.updateOptions(
                    state.options + "Option ${state.options.size + 1}"
                )
            },
            enabled = state.options.size < 8
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.marble_add_option))
        }

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Button(
            onClick = onStart,
            enabled = state.options.size >= 2,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.marble_run_start))
        }
    }
}
