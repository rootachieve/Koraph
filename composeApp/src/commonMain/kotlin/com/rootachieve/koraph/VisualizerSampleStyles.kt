package com.rootachieve.koraph

import androidx.compose.ui.graphics.Color
import com.rootachieve.koraph.graphvisualizer.EdgeStyle
import com.rootachieve.koraph.graphvisualizer.EdgeStyleInput
import com.rootachieve.koraph.graphvisualizer.NodeShape
import com.rootachieve.koraph.graphvisualizer.NodeStyle
import com.rootachieve.koraph.graphvisualizer.NodeStyleInput
import com.rootachieve.koraph.graphvisualizer.SelectionState

private val disconnectedNodeGray = Color(0xFFE5E7EB)
private val edgeNoneSelectedGray = Color(0xFFD1D5DB)
private val edgeUnselectedGray = Color(0xFFE5E7EB)
private val edgeRelatedGreen = Color(0xFFA7F3D0)
private val edgeCriticalRed = Color(0xFFFCA5A5)
private val edgeDataFlowBlue = Color(0xFF93C5FD)

private object SampleStyleDefaults {
    const val strokeLightenRatio: Float = 0.35f
    const val selectedEdgeBaseWidth: Float = 3.2f
    const val noneSelectedEdgeBaseWidth: Float = 1.7f
    const val otherSelectedEdgeBaseWidth: Float = 1.4f
    const val defaultEdgeWeight: Float = 1f
    const val minNodeDegreeForScale: Int = 0
    const val maxNodeDegreeForScale: Int = 8
    const val nodeRadiusGrowthPerDegree: Float = 0.09f
    const val maxNodeRadius: Float = 36f
    const val minimumWeightedEdgeWidth: Float = 0.9f
    const val weightNormalizationEpsilon: Float = 0.0001f
    const val sameWeightMultiplier: Float = 3f
    const val weightScalingRange: Float = 2f
}

internal fun visualizerSampleNodeStyle(
    input: NodeStyleInput<SampleNode>,
    selectedNode: SampleNode?,
    graph: VisualizerSampleGraphData,
    scaleNodeSizeByDegree: Boolean,
): NodeStyle {
    val group = graph.groupByNode[input.key] ?: SampleGroup.Support
    val groupColor = group.baseColor
    val nodeColor = when {
        selectedNode == null -> groupColor
        input.key == selectedNode -> groupColor
        graph.neighborsByNode[selectedNode]?.contains(input.key) == true -> groupColor
        else -> disconnectedNodeGray
    }
    val borderColor = lightenColor(nodeColor, SampleStyleDefaults.strokeLightenRatio)
    val degree = graph.neighborsByNode[input.key]?.size ?: SampleStyleDefaults.minNodeDegreeForScale
    val radius = if (scaleNodeSizeByDegree) {
        radiusByDegree(
            baseRadius = input.nodeInfo.size,
            degree = degree,
        )
    } else {
        input.nodeInfo.size
    }

    return NodeStyle(
        shape = NodeShape.Circle,
        fillColor = nodeColor,
        strokeColor = borderColor,
        radius = radius,
        labelColor = Color(0xFF1F2937),
    )
}

internal fun visualizerSampleEdgeStyle(
    input: EdgeStyleInput<SampleNode>,
    edgeTypes: Map<SampleEdgeKey, SampleEdgeType>,
    edgeWeights: Map<SampleEdgeKey, Float>,
    emphasizeEdgeWeight: Boolean,
    selectedEdgeWeightRange: EdgeWeightRange?,
): EdgeStyle {
    val edgeKey = sampleEdgeKey(input.from, input.to)
    val color = when (input.selectionState) {
        SelectionState.NoneSelected -> edgeNoneSelectedGray
        SelectionState.OtherSelected -> edgeUnselectedGray
        SelectionState.Selected -> {
            when (edgeTypes[edgeKey] ?: SampleEdgeType.Related) {
                SampleEdgeType.Related -> edgeRelatedGreen
                SampleEdgeType.Critical -> edgeCriticalRed
                SampleEdgeType.DataFlow -> edgeDataFlowBlue
            }
        }
    }

    val baseWidth = when (input.selectionState) {
        SelectionState.Selected -> SampleStyleDefaults.selectedEdgeBaseWidth
        SelectionState.NoneSelected -> SampleStyleDefaults.noneSelectedEdgeBaseWidth
        SelectionState.OtherSelected -> SampleStyleDefaults.otherSelectedEdgeBaseWidth
    }

    val weightedWidth = if (emphasizeEdgeWeight && input.selectionState == SelectionState.Selected) {
        selectedEdgeWidthByWeight(
            baseWidth = baseWidth,
            weight = edgeWeights[edgeKey] ?: SampleStyleDefaults.defaultEdgeWeight,
            range = selectedEdgeWeightRange,
        )
    } else {
        baseWidth
    }

    return EdgeStyle(
        color = color,
        width = weightedWidth,
        dashed = false,
    )
}

private fun lightenColor(color: Color, amount: Float): Color {
    val ratio = amount.coerceIn(0f, 1f)
    return Color(
        red = color.red + (1f - color.red) * ratio,
        green = color.green + (1f - color.green) * ratio,
        blue = color.blue + (1f - color.blue) * ratio,
        alpha = color.alpha,
    )
}

private fun radiusByDegree(baseRadius: Float, degree: Int): Float {
    val growth = 1f + (
        degree.coerceAtMost(SampleStyleDefaults.maxNodeDegreeForScale) *
            SampleStyleDefaults.nodeRadiusGrowthPerDegree
        )
    return (baseRadius * growth).coerceIn(baseRadius, SampleStyleDefaults.maxNodeRadius)
}

private fun selectedEdgeWidthByWeight(
    baseWidth: Float,
    weight: Float,
    range: EdgeWeightRange?,
): Float {
    val multiplier = when {
        range == null -> 1f
        (range.max - range.min) <= SampleStyleDefaults.weightNormalizationEpsilon -> {
            SampleStyleDefaults.sameWeightMultiplier
        }
        else -> {
            val normalized = ((weight - range.min) / (range.max - range.min))
                .coerceIn(0f, 1f)
            1f + (normalized * SampleStyleDefaults.weightScalingRange)
        }
    }
    return (baseWidth * multiplier).coerceAtLeast(SampleStyleDefaults.minimumWeightedEdgeWidth)
}
