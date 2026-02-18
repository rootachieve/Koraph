package com.rootachieve.koraph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rootachieve.koraph.graphvisualizer.ForceLayoutConfig
import com.rootachieve.koraph.graphvisualizer.GraphInteractionConfig
import com.rootachieve.koraph.graphvisualizer.GraphLabelConfig
import com.rootachieve.koraph.graphvisualizer.GraphVisualizer
import com.rootachieve.koraph.graphvisualizer.GraphVisualizerOptions
import com.rootachieve.koraph.graphvisualizer.NodeStyleInput
import com.rootachieve.koraph.graphvisualizer.rememberGraphVisualizerState

private object SampleScreenDimensions {
    val containerPadding = 16.dp
    val headerSpacing = 8.dp
    val selectionRowSpacing = 10.dp
    val controlsSpacing = 8.dp
    val graphSpacing = 12.dp
    val chipSpacing = 8.dp
    val graphCornerRadius = 20.dp
    val graphBorderWidth = 1.dp
    val graphContentPadding = 10.dp
}

private object SampleInteractionDefaults {
    const val minScale: Float = 0.4f
    const val maxScale: Float = 6f
    const val tapSelectionPadding: Float = 12f
}

private object SampleLabelDefaults {
    const val compactWidthDp: Float = 96f
    const val expandedWidthDp: Float = 128f
    const val compactFontSizeSp: Float = 12f
    const val expandedFontSizeSp: Float = 14f
    const val compactVerticalPaddingDp: Float = 6f
    const val expandedVerticalPaddingDp: Float = 9f
}

private object SampleLayoutDefaults {
    const val iterations: Int = 520
    const val nodeRepulsion: Float = 1450f
    const val repulsionExponent: Float = 1.2f
    const val edgeTension: Float = 0.018f
    const val centerTension: Float = 0.04f
    const val baseEdgeLength: Float = 96f
    const val edgeDistanceScale: Float = 1f
    const val damping: Float = 0.9f
    const val convergenceThreshold: Float = 0.14f
    const val collisionPadding: Float = 14f
    const val collisionStrength: Float = 0.85f
    const val maxVelocity: Float = 11f
}

private const val defaultWeightFallback: Float = 1f

@Composable
internal fun VisualizerSampleScreen() {
    val state = rememberGraphVisualizerState()
    val graph = remember { buildVisualizerSampleGraphData() }

    var selectedNode by remember { mutableStateOf<SampleNode?>(null) }
    var scaleNodeSizeByDegree by remember { mutableStateOf(false) }
    var emphasizeEdgeWeight by remember { mutableStateOf(false) }
    var largeLabelMode by remember { mutableStateOf(false) }

    val selectedEdgeWeightRange = remember(
        selectedNode,
        graph.neighborsByNode,
        graph.edgeWeightByKey,
    ) {
        val node = selectedNode ?: return@remember null
        val neighborWeights = graph.neighborsByNode[node]
            ?.mapNotNull { neighbor ->
                graph.edgeWeightByKey[sampleEdgeKey(node, neighbor)]
            }
            .orEmpty()

        if (neighborWeights.isEmpty()) {
            null
        } else {
            EdgeWeightRange(
                min = neighborWeights.minOrNull() ?: defaultWeightFallback,
                max = neighborWeights.maxOrNull() ?: defaultWeightFallback,
            )
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .safeContentPadding()
            .fillMaxSize()
            .padding(SampleScreenDimensions.containerPadding),
    ) {
        Text(
            text = "Visualizer Sample",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(SampleScreenDimensions.headerSpacing))
        Text(
            text = "Selected node: ${selectedNode?.let { graph.nodeInfo[it]?.name } ?: "None"}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(SampleScreenDimensions.selectionRowSpacing))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = {
                    state.resetView()
                    selectedNode = null
                },
                label = { Text("Reset View") },
            )
            Spacer(modifier = Modifier.width(SampleScreenDimensions.chipSpacing))
            AssistChip(
                onClick = {
                    scaleNodeSizeByDegree = !scaleNodeSizeByDegree
                },
                label = {
                    Text(
                        if (scaleNodeSizeByDegree) {
                            "Scale Nodes: ON"
                        } else {
                            "Scale Nodes: OFF"
                        },
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(SampleScreenDimensions.controlsSpacing))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = {
                    emphasizeEdgeWeight = !emphasizeEdgeWeight
                },
                label = {
                    Text(
                        if (emphasizeEdgeWeight) {
                            "Weighted Edges: ON"
                        } else {
                            "Weighted Edges: OFF"
                        },
                    )
                },
            )
            Spacer(modifier = Modifier.width(SampleScreenDimensions.chipSpacing))
            AssistChip(
                onClick = {
                    largeLabelMode = !largeLabelMode
                },
                label = {
                    Text(
                        if (largeLabelMode) {
                            "Large Labels: ON"
                        } else {
                            "Large Labels: OFF"
                        },
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(SampleScreenDimensions.graphSpacing))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(SampleScreenDimensions.graphCornerRadius),
                )
                .border(
                    width = SampleScreenDimensions.graphBorderWidth,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(SampleScreenDimensions.graphCornerRadius),
                )
                .padding(SampleScreenDimensions.graphContentPadding),
        ) {
            GraphVisualizer(
                adjacency = graph.adjacency,
                nodeInfo = graph.nodeInfo,
                state = state,
                options = GraphVisualizerOptions.presentation(directed = false).copy(
                    fitToViewport = true,
                    interaction = GraphInteractionConfig(
                        minScale = SampleInteractionDefaults.minScale,
                        maxScale = SampleInteractionDefaults.maxScale,
                        tapSelectionPadding = SampleInteractionDefaults.tapSelectionPadding,
                    ),
                    label = GraphLabelConfig(
                        widthDp = if (largeLabelMode) {
                            SampleLabelDefaults.expandedWidthDp
                        } else {
                            SampleLabelDefaults.compactWidthDp
                        },
                        fontSizeSp = if (largeLabelMode) {
                            SampleLabelDefaults.expandedFontSizeSp
                        } else {
                            SampleLabelDefaults.compactFontSizeSp
                        },
                        verticalPaddingDp = if (largeLabelMode) {
                            SampleLabelDefaults.expandedVerticalPaddingDp
                        } else {
                            SampleLabelDefaults.compactVerticalPaddingDp
                        },
                    ),
                    layout = ForceLayoutConfig(
                        iterations = SampleLayoutDefaults.iterations,
                        nodeRepulsion = SampleLayoutDefaults.nodeRepulsion,
                        repulsionExponent = SampleLayoutDefaults.repulsionExponent,
                        edgeTension = SampleLayoutDefaults.edgeTension,
                        centerTension = SampleLayoutDefaults.centerTension,
                        baseEdgeLength = SampleLayoutDefaults.baseEdgeLength,
                        edgeDistanceScale = SampleLayoutDefaults.edgeDistanceScale,
                        damping = SampleLayoutDefaults.damping,
                        convergenceThreshold = SampleLayoutDefaults.convergenceThreshold,
                        collisionPadding = SampleLayoutDefaults.collisionPadding,
                        collisionStrength = SampleLayoutDefaults.collisionStrength,
                        maxVelocity = SampleLayoutDefaults.maxVelocity,
                    ),
                ),
                nodeStyle = { input: NodeStyleInput<SampleNode> ->
                    visualizerSampleNodeStyle(
                        input = input,
                        selectedNode = selectedNode,
                        graph = graph,
                        scaleNodeSizeByDegree = scaleNodeSizeByDegree,
                    )
                },
                edgeStyle = { input ->
                    visualizerSampleEdgeStyle(
                        input = input,
                        edgeTypes = graph.edgeTypeByKey,
                        edgeWeights = graph.edgeWeightByKey,
                        emphasizeEdgeWeight = emphasizeEdgeWeight,
                        selectedEdgeWeightRange = selectedEdgeWeightRange,
                    )
                },
                onSelectionChange = { selected ->
                    selectedNode = selected
                },
            )
        }
    }
}
