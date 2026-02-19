package com.rootachieve.koraph.graphvisualizer

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StyleAndInteractionTest {

    @Test
    fun resolveNodeStyles_usesNodeInfoNameShapeSizeAndColor() {
        // given
        val model = buildGraphModel(
            adjacency = mapOf(
                "gateway" to listOf("search"),
            ),
            nodeInfo = mapOf(
                "gateway" to NodeInfo(
                    name = "Gateway",
                    style = NodeShape.Custom { center, radius ->
                        Path().apply {
                            moveTo(center.x, center.y - radius)
                            lineTo(center.x + radius, center.y)
                            lineTo(center.x, center.y + radius)
                            lineTo(center.x - radius, center.y)
                            close()
                        }
                    },
                    size = 24f,
                    color = Color(0xFF2563EB),
                    selectedColor = Color(0xFF93C5FD),
                ),
                "search" to NodeInfo(
                    name = "Search",
                    style = NodeShape.Hexagon,
                    size = 20f,
                    color = Color(0xFF10B981),
                ),
            ),
        )

        // when
        val nodeStyles = resolveNodeStyles(
            graphModel = model,
            selectedNodeId = 0,
            styleProvider = { input ->
                defaultNodeStyle(
                    input = input,
                    selectionColors = GraphSelectionColors(
                        selected = GraphStateColor(nodeColor = Color(0xFFFF0000)),
                        otherSelected = GraphStateColor(nodeColor = Color(0xFF00FF00)),
                        noneSelected = GraphStateColor(nodeColor = Color(0xFF0000FF)),
                    ),
                )
            },
        )
        val labels = resolveNodeLabels(model)

        // then
        assertEquals("Gateway", labels[0])
        assertTrue(nodeStyles[0].shape is NodeShape.Custom)
        assertEquals(24f, nodeStyles[0].radius)
        assertEquals(Color(0xFFFF0000), nodeStyles[0].fillColor)
        assertEquals(NodeShape.Hexagon, nodeStyles[1].shape)
        assertEquals(20f, nodeStyles[1].radius)
        assertEquals(Color(0xFF00FF00), nodeStyles[1].fillColor)
    }

    @Test
    fun resolveEdgeStyles_marksEdgesConnectedToSelectedNode() {
        // given
        val renderableEdges = listOf(
            RenderableEdge(fromId = 0, toId = 1, from = "a", to = "b", drawArrow = true),
            RenderableEdge(fromId = 2, toId = 3, from = "c", to = "d", drawArrow = true),
        )

        // when
        val edgeStyles = resolveEdgeStyles(
            renderableEdges = renderableEdges,
            selectedNodeId = 1,
            styleProvider = { input ->
                defaultEdgeStyle(
                    input = input,
                    selectionColors = GraphSelectionColors(
                        selected = GraphStateColor(edgeColor = Color.Yellow),
                        otherSelected = GraphStateColor(edgeColor = Color.Gray),
                        noneSelected = GraphStateColor(edgeColor = Color.Black),
                    ),
                )
            },
        )

        // then
        assertEquals(Color.Yellow, edgeStyles[0].color)
        assertEquals(Color.Gray, edgeStyles[1].color)
    }

    @Test
    fun resolveNodeLabels_canHideNodesByLabelProvider() {
        // given
        val model = buildGraphModel(
            adjacency = mapOf(
                "selected" to listOf("linked"),
                "other" to emptyList(),
            ),
            nodeInfo = mapOf(
                "selected" to NodeInfo(name = "Selected"),
                "linked" to NodeInfo(name = "Linked"),
                "other" to NodeInfo(name = "Other"),
            ),
        )

        // when
        val labels = resolveNodeLabels(
            graphModel = model,
            selectedNodeId = 0,
            labelProvider = { input ->
                when (input.key) {
                    "selected",
                    "linked",
                    -> input.nodeInfo.name
                    else -> null
                }
            },
        )

        // then
        assertEquals("Selected", labels[0])
        assertEquals("Linked", labels[1])
        assertEquals("", labels[2])
    }

    @Test
    fun defaultStyles_supportThreeSelectionStates() {
        // given
        val nodeInfo = NodeInfo(name = "N", color = Color(0xFF123456), strokeColor = Color(0xFF654321))
        val colors = GraphSelectionColors(
            selected = GraphStateColor(
                nodeColor = Color(0xFFAAAAAA),
                borderColor = Color(0xFF111111),
                edgeColor = Color(0xFF222222),
            ),
            otherSelected = GraphStateColor(
                nodeColor = Color(0xFFBBBBBB),
                borderColor = Color(0xFF333333),
                edgeColor = Color(0xFF444444),
            ),
            noneSelected = GraphStateColor(
                nodeColor = Color(0xFFCCCCCC),
                borderColor = Color(0xFF555555),
                edgeColor = Color(0xFF666666),
            ),
        )

        // when
        val selectedNode = defaultNodeStyle(
            input = NodeStyleInput(
                nodeId = 0,
                key = "a",
                nodeInfo = nodeInfo,
                isSelected = true,
                selectionState = SelectionState.Selected,
            ),
            selectionColors = colors,
        )
        val otherNode = defaultNodeStyle(
            input = NodeStyleInput(
                nodeId = 1,
                key = "b",
                nodeInfo = nodeInfo,
                isSelected = false,
                selectionState = SelectionState.OtherSelected,
            ),
            selectionColors = colors,
        )
        val noneNode = defaultNodeStyle(
            input = NodeStyleInput(
                nodeId = 2,
                key = "c",
                nodeInfo = nodeInfo,
                isSelected = false,
                selectionState = SelectionState.NoneSelected,
            ),
            selectionColors = colors,
        )

        assertEquals(Color(0xFFAAAAAA), selectedNode.fillColor)
        assertEquals(Color(0xFF111111), selectedNode.strokeColor)
        assertEquals(Color(0xFFBBBBBB), otherNode.fillColor)
        assertEquals(Color(0xFF333333), otherNode.strokeColor)
        assertEquals(Color(0xFFCCCCCC), noneNode.fillColor)
        assertEquals(Color(0xFF555555), noneNode.strokeColor)

        val selectedEdge = defaultEdgeStyle(
            input = EdgeStyleInput(
                fromId = 0,
                toId = 1,
                from = "a",
                to = "b",
                isSelected = true,
                selectionState = SelectionState.Selected,
            ),
            selectionColors = colors,
        )
        val otherEdge = defaultEdgeStyle(
            input = EdgeStyleInput(
                fromId = 1,
                toId = 2,
                from = "b",
                to = "c",
                isSelected = false,
                selectionState = SelectionState.OtherSelected,
            ),
            selectionColors = colors,
        )
        val noneEdge = defaultEdgeStyle(
            input = EdgeStyleInput(
                fromId = 2,
                toId = 3,
                from = "c",
                to = "d",
                isSelected = false,
                selectionState = SelectionState.NoneSelected,
            ),
            selectionColors = colors,
        )

        // then
        assertEquals(Color(0xFF222222), selectedEdge.color)
        assertEquals(Color(0xFF444444), otherEdge.color)
        assertEquals(Color(0xFF666666), noneEdge.color)
    }

    @Test
    fun findNodeAt_returnsClosestNodeWithinRadius() {
        // given
        val nodes = listOf(
            Offset(10f, 10f),
            Offset(40f, 10f),
            Offset(80f, 10f),
        )

        // when
        val selected = findNodeAt(
            pointerPosition = Offset(37f, 12f),
            projectedPositions = nodes,
            radiusProvider = { 10f },
        )
        val notSelected = findNodeAt(
            pointerPosition = Offset(500f, 500f),
            projectedPositions = nodes,
            radiusProvider = { 10f },
        )

        // then
        assertEquals(1, selected)
        assertNull(notSelected)
    }

    @Test
    fun graphVisualizerState_applyTransformUpdatesScaleAndOffset() {
        // given
        val state = GraphVisualizerState(
            scaleState = mutableStateOf(1f),
            offsetState = mutableStateOf(Offset.Zero),
            selectedNodeIdState = mutableStateOf(null),
        )

        // when
        state.applyTransform(
            centroid = Offset(100f, 100f),
            pan = Offset(20f, -8f),
            zoom = 1.5f,
        )

        // then
        assertTrue(state.scale > 1f)
        assertTrue(state.offset != Offset.Zero)
    }

    @Test
    fun graphVisualizerState_resetViewClearsSelection() {
        // given
        val state = GraphVisualizerState(
            scaleState = mutableStateOf(2.2f),
            offsetState = mutableStateOf(Offset(80f, -30f)),
            selectedNodeIdState = mutableStateOf(3),
        )

        // when
        state.resetView()

        // then
        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
        assertNull(state.selectedNodeId)
    }

    @Test
    fun animationFlags_allowBitwiseCombinationWithPlusOrAnd() {
        // given
        val unknownFlag = GraphAnimationFlags.NONE + (1 shl 5)

        // when
        val flags = GraphAnimationFlags.COLOR_TRANSITION +
            GraphAnimationFlags.LABEL_VISIBILITY_FADE +
            GraphAnimationFlags.INITIAL_RENDER

        // then
        assertTrue(flags.hasAnimationFlag(GraphAnimationFlags.COLOR_TRANSITION))
        assertTrue(flags.hasAnimationFlag(GraphAnimationFlags.LABEL_VISIBILITY_FADE))
        assertTrue(flags.hasAnimationFlag(GraphAnimationFlags.INITIAL_RENDER))
        assertFalse(flags.hasAnimationFlag(unknownFlag))
    }

    @Test
    fun graphAnimationFlagsOf_mergesFlagsConveniently() {
        // given
        val targetFlags = intArrayOf(
            GraphAnimationFlags.COLOR_TRANSITION,
            GraphAnimationFlags.LABEL_VISIBILITY_FADE,
            GraphAnimationFlags.INITIAL_RENDER,
        )

        // when
        val flags = graphAnimationFlagsOf(
            *targetFlags,
        )

        // then
        assertTrue(flags.hasAnimationFlag(GraphAnimationFlags.COLOR_TRANSITION))
        assertTrue(flags.hasAnimationFlag(GraphAnimationFlags.LABEL_VISIBILITY_FADE))
        assertTrue(flags.hasAnimationFlag(GraphAnimationFlags.INITIAL_RENDER))
    }

    @Test
    fun graphVisualizerOptions_presets_areConvenientAndConsistent() {
        // given
        val defaultOptions = GraphVisualizerOptions()

        // when
        val defaults = GraphVisualizerOptions.default()
        val performance = GraphVisualizerOptions.performance()
        val presentation = GraphVisualizerOptions.presentation()

        // then
        assertEquals(defaultOptions, defaults)
        assertEquals(GraphAnimationFlags.NONE, performance.animationFlags)
        assertTrue(performance.layout.iterations < defaults.layout.iterations)
        assertTrue(presentation.animationFlags.hasAnimationFlag(GraphAnimationFlags.COLOR_TRANSITION))
        assertTrue(presentation.animationFlags.hasAnimationFlag(GraphAnimationFlags.LABEL_VISIBILITY_FADE))
        assertTrue(presentation.animationFlags.hasAnimationFlag(GraphAnimationFlags.INITIAL_RENDER))
    }

    @Test
    fun graphInteractionConfig_clampsUnsafeValues() {
        // given
        val minScale = -3f
        val maxScale = 0f
        val tapSelectionPadding = -2f

        // when
        val config = GraphInteractionConfig(
            minScale = minScale,
            maxScale = maxScale,
            tapSelectionPadding = tapSelectionPadding,
            clearSelectionOnBackgroundTap = false,
        )

        // then
        assertEquals(0.01f, config.resolvedMinScale)
        assertEquals(0.01f, config.resolvedMaxScale)
        assertEquals(0f, config.resolvedTapSelectionPadding)
        assertFalse(config.clearSelectionOnBackgroundTap)
    }

    @Test
    fun graphLabelConfig_clampsUnsafeValues() {
        // given
        val widthDp = 0f
        val fontSizeSp = 2f
        val verticalPaddingDp = -4f

        // when
        val config = GraphLabelConfig(
            widthDp = widthDp,
            fontSizeSp = fontSizeSp,
            verticalPaddingDp = verticalPaddingDp,
        )

        // then
        assertEquals(24f, config.resolvedWidthDp)
        assertEquals(8f, config.resolvedFontSizeSp)
        assertEquals(0f, config.resolvedVerticalPaddingDp)
    }
}
