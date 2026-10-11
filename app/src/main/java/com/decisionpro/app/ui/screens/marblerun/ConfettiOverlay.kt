package com.decisionpro.app.ui.screens.marblerun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decisionpro.app.R

private class ConfettiPiece(
    var x: Float, var y: Float,
    val vx: Float, val vy: Float,
    val color: Long, var rotation: Float, val size: Float
)

@Composable
fun ConfettiOverlay(winner: String) {
    val confetti = remember {
        List(60) {
            ConfettiPiece(
                x = Math.random().toFloat(),
                y = -Math.random().toFloat(),
                vx = (Math.random() - 0.5).toFloat() * 0.3f,
                vy = 0.25f + Math.random().toFloat() * 0.4f,
                color = MarblePalette.colors.random(),
                rotation = Math.random().toFloat() * 360f,
                size = 6f + Math.random().toFloat() * 8f
            )
        }
    }
    var animate by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        var frames = 0
        while (animate && frames < 240) {
            withFrameNanos {
                confetti.forEach { c ->
                    c.y += c.vy * 0.016f
                    c.x += c.vx * 0.016f
                    c.rotation += 4f
                    if (c.y > 1.1f) { c.y = -0.1f; c.x = Math.random().toFloat() }
                }
            }
            frames++
        }
        animate = false
    }

    Box(Modifier.fillMaxSize()) {
        if (animate) {
            Canvas(Modifier.fillMaxSize()) {
                confetti.forEach { c ->
                    rotate(c.rotation) {
                        drawRect(
                            color = Color(c.color),
                            topLeft = Offset(c.x * size.width, c.y * size.height),
                            size = Size(c.size, c.size * 0.6f)
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            shadowElevation = 8.dp,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆", fontSize = 48.sp)
                Text(
                    winner,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.marble_run_winner),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
