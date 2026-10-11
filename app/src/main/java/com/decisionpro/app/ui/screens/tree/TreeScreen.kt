package com.decisionpro.app.ui.screens.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decisionpro.app.R
import com.decisionpro.app.domain.engine.tree.Branch
import com.decisionpro.app.domain.engine.tree.Chance
import com.decisionpro.app.domain.engine.tree.Decision
import com.decisionpro.app.domain.engine.tree.Outcome
import com.decisionpro.app.domain.engine.tree.TreeAnalyzer
import com.decisionpro.app.domain.engine.tree.TreeLayout
import com.decisionpro.app.domain.engine.tree.TreeNode
import com.decisionpro.app.domain.engine.tree.branches
import com.decisionpro.app.ui.components.WarningBanner

@Composable
fun TreeScreen(
    onNavigateBack: () -> Unit,
    viewModel: TreeViewModel = hiltViewModel()
) {
    val tree by viewModel.tree.collectAsStateWithLifecycle()
    val evaluation by viewModel.evaluation.collectAsStateWithLifecycle()
    val question by viewModel.question.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val insight = remember(tree) { viewModel.breakevenInsight() }

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
                stringResource(R.string.tree_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = viewModel::undo, enabled = canUndo) {
                Icon(Icons.Default.Undo, stringResource(R.string.action_undo))
            }
            IconButton(onClick = viewModel::redo, enabled = canRedo) {
                Icon(Icons.Default.Redo, stringResource(R.string.action_redo))
            }
            IconButton(onClick = viewModel::resetToTemplate) {
                Icon(Icons.Default.Refresh, stringResource(R.string.action_reset))
            }
        }

        OutlinedTextField(
            value = question,
            onValueChange = viewModel::setQuestion,
            label = { Text("What are you deciding?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        insight?.let {
            Card(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    "💡 $it",
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        evaluation.validationError?.let {
            WarningBanner(
                stringResource(R.string.tree_error_probabilities),
                Modifier.padding(top = 8.dp)
            )
        }

        Text(
            "Best expected value: %+.0f".format(evaluation.rootValue),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        TreeCanvas(
            tree = tree,
            evaluation = evaluation,
            onNodeTap = { selectedNodeId = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
        )

        Text(
            stringResource(R.string.tree_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            textAlign = TextAlign.Center
        )

        Button(
            onClick = viewModel::saveResult,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.tree_save_result))
        }
    }

    selectedNodeId?.let { nodeId ->
        NodeEditorSheet(
            tree = tree,
            nodeId = nodeId,
            onDismiss = { selectedNodeId = null },
            onRename = { label -> viewModel.rename(nodeId, label) },
            onSetValue = { viewModel.setValue(nodeId, it) },
            onSetProbability = { parent, child, p ->
                viewModel.setProbability(parent, child, p)
            },
            onAddBranch = { parent -> viewModel.addBranch(parent) },
            onRemoveBranch = { parent, child -> viewModel.removeBranch(parent, child) },
            onConvertToChance = { viewModel.convertToChance(nodeId); selectedNodeId = null }
        )
    }
}

@Composable
fun TreeCanvas(
    tree: TreeNode,
    evaluation: TreeAnalyzer.TreeEvaluation,
    onNodeTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val layout = remember(tree, evaluation, canvasSize, isRtl) {
        if (canvasSize == IntSize.Zero) null
        else TreeLayout.layout(
            tree, canvasSize.width.toFloat(), canvasSize.height.toFloat(),
            evaluation, isRtl
        )
    }

    Canvas(
        modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(layout) {
                detectTapGestures { tap ->
                    layout?.let { l ->
                        val hitRadius = 34.dp.toPx()
                        l.nodes
                            .minByOrNull { n ->
                                val dx = n.x - tap.x; val dy = n.y - tap.y
                                dx * dx + dy * dy
                            }
                            ?.takeIf { n ->
                                val dx = n.x - tap.x; val dy = n.y - tap.y
                                dx * dx + dy * dy < hitRadius * hitRadius
                            }
                            ?.let { onNodeTap(it.node.id) }
                    }
                }
            }
    ) {
        val l = layout ?: return@Canvas

        drawRect(Color(0xFF14141F))

        l.edges.forEach { edge ->
            val color = when {
                edge.pruned -> Color.Gray.copy(alpha = 0.30f)
                edge.onBestPath -> Color(0xFF4CAF50)
                else -> Color(0xFF78909C)
            }
            val strokeWidth = when {
                edge.onBestPath -> 4.dp.toPx()
                edge.pruned -> 1.5.dp.toPx()
                else -> 2.dp.toPx()
            }
            val midX = (edge.from.x + edge.to.x) / 2f
            val path = Path().apply {
                moveTo(edge.from.x, edge.from.y)
                cubicTo(midX, edge.from.y, midX, edge.to.y, edge.to.x, edge.to.y)
            }
            drawPath(
                path, color,
                style = Stroke(
                    strokeWidth,
                    pathEffect = if (edge.pruned)
                        PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
                )
            )
            if (edge.probability != null && !edge.pruned) {
                val label = "%.0f%%".format(edge.probability * 100)
                val measured = textMeasurer.measure(
                    label,
                    TextStyle(fontSize = 10.sp, color = Color(0xFF90A4AE))
                )
                drawText(
                    measured,
                    topLeft = Offset(
                        midX - measured.size.width / 2f,
                        (edge.from.y + edge.to.y) / 2f - measured.size.height / 2f
                    )
                )
            }
        }

        l.nodes.forEach { placed ->
            val ev = placed.evaluation
            val nodeColor = when (val n = placed.node) {
                is Decision -> Color(0xFF1E88E5)
                is Chance -> Color(0xFFFFB300)
                is Outcome ->
                    if (n.value >= 0) Color(0xFF43A047) else Color(0xFFE53935)
            }
            val nodeSize = 15.dp.toPx()

            if (placed.node.id in evaluation.bestPathIds) {
                drawCircle(
                    nodeColor.copy(alpha = 0.20f),
                    nodeSize * 1.7f,
                    Offset(placed.x, placed.y)
                )
            }

            when (val n = placed.node) {
                is Decision -> drawRoundRect(
                    nodeColor,
                    topLeft = Offset(placed.x - nodeSize, placed.y - nodeSize),
                    size = Size(nodeSize * 2, nodeSize * 2),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                is Chance -> drawCircle(nodeColor, nodeSize, Offset(placed.x, placed.y))
                is Outcome -> {
                    drawRoundRect(
                        nodeColor,
                        topLeft = Offset(placed.x - nodeSize, placed.y - nodeSize * 0.7f),
                        size = Size(nodeSize * 2, nodeSize * 1.4f),
                        cornerRadius = CornerRadius(nodeSize * 0.7f)
                    )
                    val valueLabel = "%+.0f".format(n.value)
                    val measured = textMeasurer.measure(
                        valueLabel,
                        TextStyle(
                            fontSize = 11.sp, color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        measured,
                        topLeft = Offset(
                            placed.x - measured.size.width / 2f,
                            placed.y - measured.size.height / 2f
                        )
                    )
                }
            }

            if (ev != null && placed.node !is Outcome) {
                val emvLabel = "EMV %+.0f".format(ev.value)
                val measured = textMeasurer.measure(
                    emvLabel,
                    TextStyle(
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                )
                drawText(
                    measured,
                    topLeft = Offset(
                        placed.x - measured.size.width / 2f,
                        placed.y - nodeSize - measured.size.height - 8f
                    )
                )
            }

            val labelMeasured = textMeasurer.measure(
                placed.node.label.take(14),
                TextStyle(fontSize = 11.sp, color = Color(0xFFB0B3C0))
            )
            drawText(
                labelMeasured,
                topLeft = Offset(
                    placed.x - labelMeasured.size.width / 2f,
                    placed.y + nodeSize + 6f
                )
            )
        }
    }
}

private fun locate(
    root: TreeNode, id: String,
    parent: TreeNode? = null, branch: Branch? = null
): Triple<TreeNode, TreeNode?, Branch?>? {
    if (root.id == id) return Triple(root, parent, branch)
    return root.branches().firstNotNullOfOrNull { b ->
        locate(b.child, id, root, b)
    }
}

@Composable
private fun NodeEditorSheet(
    tree: TreeNode,
    nodeId: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onSetValue: (Double) -> Unit,
    onSetProbability: (parentId: String, childId: String, p: Double) -> Unit,
    onAddBranch: (parentId: String) -> Unit,
    onRemoveBranch: (parentId: String, childId: String) -> Unit,
    onConvertToChance: (id: String) -> Unit
) {
    val located = remember(tree, nodeId) { locate(tree, nodeId) } ?: return
    val (node, parent, incomingBranch) = located

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val typeLabel = when (node) {
                is Decision -> "▢ Decision — your choice"
                is Chance -> "◯ Chance — probability outcome"
                is Outcome -> "◁ Outcome — final payoff"
            }
            Text(
                typeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            var label by remember(nodeId) { mutableStateOf(node.label) }
            OutlinedTextField(
                value = label,
                onValueChange = {
                    label = it
                    onRename(it)
                },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (node is Outcome) {
                Spacer(Modifier.height(12.dp))
                var valueText by remember(nodeId) {
                    mutableStateOf("%.0f".format(node.value))
                }
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { raw ->
                        valueText = raw
                        raw.toDoubleOrNull()?.let(onSetValue)
                    },
                    label = { Text("Payoff (− cost, + gain)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onConvertToChance(nodeId) }) {
                    Text("Split into scenarios")
                }
            }

            if (parent is Chance && incomingBranch != null) {
                Spacer(Modifier.height(16.dp))
                Text("Probability", style = MaterialTheme.typography.titleSmall)
                var p by remember(nodeId) {
                    mutableDoubleStateOf(incomingBranch.probability ?: 0.5)
                }
                Text(
                    "%.0f%%".format(p * 100),
                    style = MaterialTheme.typography.headlineSmall
                )
                Slider(
                    value = p.toFloat(),
                    onValueChange = {
                        p = it.toDouble()
                        onSetProbability(parent.id, nodeId, p)
                    },
                    valueRange = 0f..1f
                )
            }

            if (node is Decision || node is Chance) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onAddBranch(nodeId) }) {
                    Text("Add branch")
                }
            }
            if (parent != null && parent.branches().size > 1) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onRemoveBranch(parent.id, nodeId) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete) + " branch")
                }
            }
        }
    }
}
