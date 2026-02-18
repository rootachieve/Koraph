package com.rootachieve.koraph.graphvisualizer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Stable
data class GraphVisualizerOptions(
    val directed: Boolean = true,
    val showArrows: Boolean = true,
    val enablePanZoom: Boolean = true,
    val enableTapSelection: Boolean = true,
    val fitToViewport: Boolean = true,
    val viewportPadding: Float = 48f,
    val clearSelectionOnInit: Boolean = true,
    val selectionColors: GraphSelectionColors = GraphSelectionColors(),
    val animationFlags: Int = GraphAnimationFlags.COLOR_TRANSITION or
        GraphAnimationFlags.LABEL_VISIBILITY_FADE or
        GraphAnimationFlags.LAYOUT_TRANSITION,
    val colorAnimationDurationMillis: Int = 220,
    val labelFadeAnimationDurationMillis: Int = 220,
    val layoutAnimationDurationMillis: Int = 280,
    val labelFadeZoomThreshold: Float = 0.75f,
    val initialRenderAnimationDurationMillis: Int = 900,
    val layout: ForceLayoutConfig = ForceLayoutConfig(),
    val interaction: GraphInteractionConfig = GraphInteractionConfig(),
    val label: GraphLabelConfig = GraphLabelConfig(),
) {
    companion object {
        fun default(): GraphVisualizerOptions = GraphVisualizerOptions()

        fun performance(
            directed: Boolean = true,
        ): GraphVisualizerOptions {
            return GraphVisualizerOptions(
                directed = directed,
                showArrows = directed,
                animationFlags = GraphAnimationFlags.NONE,
                layout = ForceLayoutConfig(
                    iterations = 180,
                    nodeRepulsion = 900f,
                    edgeTension = 0.018f,
                    centerTension = 0.03f,
                    damping = 0.92f,
                    convergenceThreshold = 0.9f,
                    baseEdgeLength = 78f,
                    collisionPadding = 6f,
                    collisionStrength = 0.5f,
                    maxVelocity = 16f,
                ),
            )
        }

        fun presentation(
            directed: Boolean = true,
        ): GraphVisualizerOptions {
            return GraphVisualizerOptions(
                directed = directed,
                showArrows = directed,
                animationFlags = GraphAnimationFlags.COLOR_TRANSITION or
                    GraphAnimationFlags.LABEL_VISIBILITY_FADE or
                    GraphAnimationFlags.LAYOUT_TRANSITION or
                    GraphAnimationFlags.INITIAL_RENDER,
                colorAnimationDurationMillis = 260,
                labelFadeAnimationDurationMillis = 260,
                layoutAnimationDurationMillis = 300,
                labelFadeZoomThreshold = 0.85f,
                initialRenderAnimationDurationMillis = 1000,
                layout = ForceLayoutConfig(
                    iterations = 320,
                    centerTension = 0.03f,
                    baseEdgeLength = 88f,
                    collisionPadding = 10f,
                    collisionStrength = 0.75f,
                    maxVelocity = 12f,
                ),
            )
        }
    }
}

@Stable
data class GraphInteractionConfig(
    val minScale: Float = 0.35f,
    val maxScale: Float = 4.5f,
    val tapSelectionPadding: Float = 8f,
    val clearSelectionOnBackgroundTap: Boolean = true,
) {
    val resolvedMinScale: Float
        get() = minScale.coerceAtLeast(0.01f)

    val resolvedMaxScale: Float
        get() = maxScale.coerceAtLeast(resolvedMinScale)

    val resolvedTapSelectionPadding: Float
        get() = tapSelectionPadding.coerceAtLeast(0f)
}

@Stable
data class GraphLabelConfig(
    val widthDp: Float = 96f,
    val fontSizeSp: Float = 12f,
    val verticalPaddingDp: Float = 6f,
) {
    val resolvedWidthDp: Float
        get() = widthDp.coerceAtLeast(24f)

    val resolvedFontSizeSp: Float
        get() = fontSizeSp.coerceAtLeast(8f)

    val resolvedVerticalPaddingDp: Float
        get() = verticalPaddingDp.coerceAtLeast(0f)
}

object GraphAnimationFlags {
    const val NONE: Int = 0
    const val COLOR_TRANSITION: Int = 1
    const val LABEL_VISIBILITY_FADE: Int = 1 shl 1
    const val INITIAL_RENDER: Int = 1 shl 2
    const val LAYOUT_TRANSITION: Int = 1 shl 3
}

fun Int.hasAnimationFlag(flag: Int): Boolean = (this and flag) == flag

fun graphAnimationFlagsOf(vararg flags: Int): Int {
    var merged = GraphAnimationFlags.NONE
    for (flag in flags) {
        merged = merged or flag
    }
    return merged
}

@Stable
data class ForceLayoutConfig(
    val iterations: Int = 300,
    val nodeRepulsion: Float = 1200f,
    val repulsionExponent: Float = 2f,
    val edgeTension: Float = 0.02f,
    val degreeAwareEdgeTension: Boolean = true,
    val centerTension: Float = 0.02f,
    val baseEdgeLength: Float = 84f,
    val edgeDistanceScale: Float = 1f,
    val damping: Float = 0.9f,
    val convergenceThreshold: Float = 0.5f,
    val randomSeed: Int = 42,
    val collisionPadding: Float = 8f,
    val collisionStrength: Float = 0.65f,
    val maxVelocity: Float = 14f,
)

sealed interface NodeShape {
    data object Circle : NodeShape
    data object RoundedRect : NodeShape
    data object Diamond : NodeShape
    data object Hexagon : NodeShape

    /**
     * Return a closed [Path] in canvas coordinates.
     * The path will be used for both fill and stroke drawing.
     */
    data class Custom(
        val pathBuilder: (center: Offset, radius: Float) -> Path,
    ) : NodeShape
}

@Stable
data class NodeInfo(
    val name: String,
    val style: NodeShape = NodeShape.Circle,
    val size: Float = 18f,
    val color: Color = Color(0xFF2563EB),
    val selectedColor: Color = Color(0xFFF59E0B),
    val strokeColor: Color = Color(0xFF1E3A8A),
    val selectedStrokeColor: Color = Color(0xFF7C2D12),
    val labelColor: Color = Color(0xFF111827),
)

@Stable
data class GraphStateColor(
    val nodeColor: Color? = null,
    val borderColor: Color? = null,
    val edgeColor: Color? = null,
)

@Stable
data class GraphSelectionColors(
    val selected: GraphStateColor = GraphStateColor(),
    val otherSelected: GraphStateColor = GraphStateColor(),
    val noneSelected: GraphStateColor = GraphStateColor(),
)

enum class SelectionState {
    Selected,
    OtherSelected,
    NoneSelected,
}

@Stable
data class NodeStyle(
    val shape: NodeShape,
    val fillColor: Color,
    val strokeColor: Color,
    val radius: Float,
    val labelColor: Color,
)

@Stable
data class EdgeStyle(
    val color: Color,
    val width: Float,
    val dashed: Boolean = false,
)

@Stable
data class NodeStyleInput<K>(
    val nodeId: Int,
    val key: K,
    val nodeInfo: NodeInfo,
    val isSelected: Boolean,
    val selectionState: SelectionState,
)

@Stable
data class EdgeStyleInput<K>(
    val fromId: Int,
    val toId: Int,
    val from: K,
    val to: K,
    val isSelected: Boolean,
    val selectionState: SelectionState,
)

@Stable
data class GraphVisualizerState(
    private val scaleState: MutableState<Float>,
    private val offsetState: MutableState<Offset>,
    private val selectedNodeIdState: MutableState<Int?>,
) {
    val scale: Float
        get() = scaleState.value

    val offset: Offset
        get() = offsetState.value

    val selectedNodeId: Int?
        get() = selectedNodeIdState.value

    fun updateScale(value: Float) {
        scaleState.value = value
    }

    fun updateOffset(value: Offset) {
        offsetState.value = value
    }

    fun updateSelectedNodeId(value: Int?) {
        selectedNodeIdState.value = value
    }

    fun resetView(
        scale: Float = 1f,
        offset: Offset = Offset.Zero,
        clearSelection: Boolean = true,
    ) {
        updateScale(scale.coerceAtLeast(0.01f))
        updateOffset(offset)
        if (clearSelection) {
            updateSelectedNodeId(null)
        }
    }

    fun applyTransform(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        minScale: Float = 0.35f,
        maxScale: Float = 4.5f,
    ) {
        val previousScale = scale.coerceAtLeast(0.01f)
        val nextScale = (previousScale * zoom).coerceIn(minScale, maxScale)
        val scaleFactor = nextScale / previousScale

        val shiftedOffset = offset - centroid
        val scaledShiftedOffset = Offset(
            x = shiftedOffset.x * scaleFactor,
            y = shiftedOffset.y * scaleFactor,
        )

        updateOffset(centroid + scaledShiftedOffset + pan)
        updateScale(nextScale)
    }
}

@Composable
fun rememberGraphVisualizerState(
    initialScale: Float = 1f,
    initialOffset: Offset = Offset.Zero,
    initialSelectedNodeId: Int? = null,
): GraphVisualizerState {
    val scaleState = remember { mutableStateOf(initialScale.coerceAtLeast(0.01f)) }
    val offsetState = remember { mutableStateOf(initialOffset) }
    val selectedNodeIdState = remember { mutableStateOf(initialSelectedNodeId) }

    return remember {
        GraphVisualizerState(
            scaleState = scaleState,
            offsetState = offsetState,
            selectedNodeIdState = selectedNodeIdState,
        )
    }
}

fun defaultNodeInfo(name: String): NodeInfo {
    return NodeInfo(name = name)
}

fun nodeStyleFromInfo(nodeInfo: NodeInfo, isSelected: Boolean): NodeStyle {
    return NodeStyle(
        shape = nodeInfo.style,
        fillColor = if (isSelected) nodeInfo.selectedColor else nodeInfo.color,
        strokeColor = if (isSelected) nodeInfo.selectedStrokeColor else nodeInfo.strokeColor,
        radius = nodeInfo.size,
        labelColor = nodeInfo.labelColor,
    )
}

fun <K> defaultNodeStyle(
    input: NodeStyleInput<K>,
    selectionColors: GraphSelectionColors,
): NodeStyle {
    val stateColor = when (input.selectionState) {
        SelectionState.Selected -> selectionColors.selected
        SelectionState.OtherSelected -> selectionColors.otherSelected
        SelectionState.NoneSelected -> selectionColors.noneSelected
    }

    val defaultNodeColor = when (input.selectionState) {
        SelectionState.Selected -> input.nodeInfo.selectedColor
        SelectionState.OtherSelected -> input.nodeInfo.color
        SelectionState.NoneSelected -> input.nodeInfo.color
    }
    val defaultBorderColor = when (input.selectionState) {
        SelectionState.Selected -> input.nodeInfo.selectedStrokeColor
        SelectionState.OtherSelected -> input.nodeInfo.strokeColor
        SelectionState.NoneSelected -> input.nodeInfo.strokeColor
    }

    return NodeStyle(
        shape = input.nodeInfo.style,
        fillColor = stateColor.nodeColor ?: defaultNodeColor,
        strokeColor = stateColor.borderColor ?: defaultBorderColor,
        radius = input.nodeInfo.size,
        labelColor = input.nodeInfo.labelColor,
    )
}

fun defaultEdgeStyle(isSelected: Boolean): EdgeStyle {
    return EdgeStyle(
        color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF64748B),
        width = if (isSelected) 3.5f else 2f,
        dashed = false,
    )
}

fun <K> defaultEdgeStyle(
    input: EdgeStyleInput<K>,
    selectionColors: GraphSelectionColors,
): EdgeStyle {
    val stateColor = when (input.selectionState) {
        SelectionState.Selected -> selectionColors.selected
        SelectionState.OtherSelected -> selectionColors.otherSelected
        SelectionState.NoneSelected -> selectionColors.noneSelected
    }
    val fallback = defaultEdgeStyle(isSelected = input.isSelected)

    return fallback.copy(
        color = stateColor.edgeColor ?: fallback.color,
    )
}
