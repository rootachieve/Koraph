package com.rootachieve.koraph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.rootachieve.koraph.graphvisualizer.GraphVisualizer
import com.rootachieve.koraph.graphvisualizer.NodeStyleInput
import com.rootachieve.koraph.graphvisualizer.rememberGraphVisualizerState

private object SampleScreenDimensions {
    val containerPadding = 16.dp
    val headerSpacing = 8.dp
    val actionRowSpacing = 10.dp
    val panelSpacing = 12.dp
    val chipSpacing = 8.dp
    val controlPanelMaxHeight = 320.dp
    val graphCornerRadius = 20.dp
    val graphBorderWidth = 1.dp
    val graphContentPadding = 10.dp
}

private const val defaultWeightFallback: Float = 1f

@Composable
internal fun VisualizerSampleScreen() {
    val state = rememberGraphVisualizerState()
    val graph = remember { buildVisualizerSampleGraphData() }

    var selectedNode by remember { mutableStateOf<SampleNode?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var controls by remember { mutableStateOf(buildVisualizerControlState()) }

    val visualizerOptions = remember(controls) {
        controls.toGraphVisualizerOptions()
    }
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
        Spacer(modifier = Modifier.height(SampleScreenDimensions.actionRowSpacing))

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
                    val preservedStyles = controls
                    controls = buildVisualizerControlState(
                        preset = controls.preset,
                        directed = controls.directed,
                    ).copy(
                        scaleNodeSizeByDegree = preservedStyles.scaleNodeSizeByDegree,
                        emphasizeEdgeWeight = preservedStyles.emphasizeEdgeWeight,
                    )
                },
                label = { Text("Reset Inputs") },
            )
        }

        Spacer(modifier = Modifier.height(SampleScreenDimensions.chipSpacing))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = {
                    showControls = !showControls
                },
                label = {
                    Text(
                        if (showControls) {
                            "Hide Controls"
                        } else {
                            "Show Controls"
                        },
                    )
                },
            )
            Spacer(modifier = Modifier.width(SampleScreenDimensions.chipSpacing))
            AssistChip(
                onClick = {
                    controls = buildVisualizerControlState(
                        preset = VisualizerPreset.Sample,
                        directed = controls.directed,
                    ).copy(
                        scaleNodeSizeByDegree = controls.scaleNodeSizeByDegree,
                        emphasizeEdgeWeight = controls.emphasizeEdgeWeight,
                    )
                },
                label = { Text("Load Sample Preset") },
            )
        }

        if (showControls) {
            Spacer(modifier = Modifier.height(SampleScreenDimensions.panelSpacing))
            VisualizerControlPanel(
                controls = controls,
                onControlsChange = { updated ->
                    controls = updated
                },
                onApplyPreset = { preset ->
                    val preservedStyles = controls
                    controls = buildVisualizerControlState(
                        preset = preset,
                        directed = controls.directed,
                    ).copy(
                        scaleNodeSizeByDegree = preservedStyles.scaleNodeSizeByDegree,
                        emphasizeEdgeWeight = preservedStyles.emphasizeEdgeWeight,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = SampleScreenDimensions.controlPanelMaxHeight),
            )
        }

        Spacer(modifier = Modifier.height(SampleScreenDimensions.panelSpacing))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
                options = visualizerOptions,
                nodeStyle = { input: NodeStyleInput<SampleNode> ->
                    visualizerSampleNodeStyle(
                        input = input,
                        selectedNode = selectedNode,
                        graph = graph,
                        scaleNodeSizeByDegree = controls.scaleNodeSizeByDegree,
                    )
                },
                edgeStyle = { input ->
                    visualizerSampleEdgeStyle(
                        input = input,
                        edgeTypes = graph.edgeTypeByKey,
                        edgeWeights = graph.edgeWeightByKey,
                        emphasizeEdgeWeight = controls.emphasizeEdgeWeight,
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
