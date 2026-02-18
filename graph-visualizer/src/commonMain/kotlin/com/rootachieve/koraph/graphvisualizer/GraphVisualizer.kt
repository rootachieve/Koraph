package com.rootachieve.koraph.graphvisualizer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun <K> SimpleGraphVisualizer(
    adjacency: Map<K, List<K>>,
    modifier: Modifier = Modifier,
    options: GraphVisualizerOptions = GraphVisualizerOptions.default(),
    state: GraphVisualizerState = rememberGraphVisualizerState(),
    nodeLabel: (K) -> String = { it.toString() },
    nodeInfoFactory: (K) -> NodeInfo = { key -> defaultNodeInfo(name = nodeLabel(key)) },
    onSelectionChange: (K?) -> Unit = {},
    onNodeClick: (K) -> Unit = {},
) {
    val generatedNodeInfo = remember(adjacency, nodeLabel, nodeInfoFactory) {
        collectAllNodeKeys(adjacency).associateWith { key ->
            nodeInfoFactory(key)
        }
    }

    GraphVisualizer(
        adjacency = adjacency,
        nodeInfo = generatedNodeInfo,
        modifier = modifier,
        options = options,
        state = state,
        onSelectionChange = onSelectionChange,
        onNodeClick = onNodeClick,
    )
}

@Composable
fun <K> GraphVisualizer(
    adjacency: Map<K, List<K>>,
    nodeInfo: Map<K, NodeInfo> = emptyMap(),
    fallbackNodeInfo: (K) -> NodeInfo = { key -> defaultNodeInfo(name = key.toString()) },
    modifier: Modifier = Modifier,
    options: GraphVisualizerOptions = GraphVisualizerOptions.default(),
    state: GraphVisualizerState = rememberGraphVisualizerState(),
    nodeStyle: ((NodeStyleInput<K>) -> NodeStyle)? = null,
    labelText: ((NodeStyleInput<K>) -> String?)? = null,
    edgeStyle: ((EdgeStyleInput<K>) -> EdgeStyle)? = null,
    onSelectionChange: (K?) -> Unit = {},
    onNodeClick: (K) -> Unit = {},
) {
    val signature = adjacencySignature(
        adjacency = adjacency,
        nodeInfo = nodeInfo,
    )
    val graphModel = remember(signature, fallbackNodeInfo) {
        buildGraphModel(
            adjacency = adjacency,
            nodeInfo = nodeInfo,
            fallbackNodeInfo = fallbackNodeInfo,
        )
    }
    LaunchedEffect(signature, options.clearSelectionOnInit) {
        if (options.clearSelectionOnInit) {
            state.updateSelectedNodeId(null)
            onSelectionChange(null)
        }
    }

    val layoutEdges = remember(graphModel.edges) {
        buildLayoutEdges(graphModel.edges)
    }
    val layoutNodeRadii = remember(graphModel.nodeKeys, graphModel.nodeInfoByKey) {
        graphModel.nodeKeys.map { key ->
            graphModel.nodeInfoByKey[key]?.size ?: 18f
        }
    }
    val layoutResult = remember(graphModel.nodeCount, layoutEdges, layoutNodeRadii, options.layout) {
        computeForceLayout(
            nodeCount = graphModel.nodeCount,
            edges = layoutEdges,
            config = options.layout,
            nodeRadii = layoutNodeRadii,
        )
    }
    val animatedWorldPositions = animatedLayoutPositions(
        targetPositions = layoutResult.positions,
        enabled = options.animationFlags.hasAnimationFlag(GraphAnimationFlags.LAYOUT_TRANSITION),
        durationMillis = options.layoutAnimationDurationMillis,
    )

    val renderableEdges = remember(graphModel.edges, options.directed, options.showArrows) {
        buildRenderableEdges(
            edges = graphModel.edges,
            directed = options.directed,
            showArrows = options.showArrows,
        )
    }
    val interaction = options.interaction
    val label = options.label

    val resolvedNodeStyle = nodeStyle ?: { input: NodeStyleInput<K> ->
        defaultNodeStyle(
            input = input,
            selectionColors = options.selectionColors,
        )
    }
    val resolvedEdgeStyle = edgeStyle ?: { input: EdgeStyleInput<K> ->
        defaultEdgeStyle(
            input = input,
            selectionColors = options.selectionColors,
        )
    }

    val nodeStyles = resolveNodeStyles(
        graphModel = graphModel,
        selectedNodeId = state.selectedNodeId,
        styleProvider = resolvedNodeStyle,
    )
    val nodeLabels = resolveNodeLabels(
        graphModel = graphModel,
        selectedNodeId = state.selectedNodeId,
        labelProvider = labelText,
    )
    val edgeStyles = resolveEdgeStyles(
        renderableEdges = renderableEdges,
        selectedNodeId = state.selectedNodeId,
        styleProvider = resolvedEdgeStyle,
    )

    val animatedNodeStyles = animatedNodeStyles(
        styles = nodeStyles,
        enabled = options.animationFlags.hasAnimationFlag(GraphAnimationFlags.COLOR_TRANSITION),
        durationMillis = options.colorAnimationDurationMillis,
    )
    val animatedEdgeStyles = animatedEdgeStyles(
        styles = edgeStyles,
        enabled = options.animationFlags.hasAnimationFlag(GraphAnimationFlags.COLOR_TRANSITION),
        durationMillis = options.colorAnimationDurationMillis,
    )
    val labelAlpha = animatedLabelAlpha(
        scale = state.scale,
        enabled = options.animationFlags.hasAnimationFlag(GraphAnimationFlags.LABEL_VISIBILITY_FADE),
        threshold = options.labelFadeZoomThreshold,
        durationMillis = options.labelFadeAnimationDurationMillis,
    )
    val entryProgress = initialRenderProgress(
        enabled = options.animationFlags.hasAnimationFlag(GraphAnimationFlags.INITIAL_RENDER),
        signature = signature,
        durationMillis = options.initialRenderAnimationDurationMillis,
    )

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val baseTransform = remember(
        animatedWorldPositions,
        canvasSize,
        options.fitToViewport,
        options.viewportPadding,
    ) {
        calculateBaseTransform(
            positions = animatedWorldPositions,
            canvasSize = canvasSize,
            padding = options.viewportPadding,
            fitToBounds = options.fitToViewport,
        )
    }
    val projectedPositions = remember(animatedWorldPositions, baseTransform, state.scale, state.offset) {
        projectPositions(
            positions = animatedWorldPositions,
            baseTransform = baseTransform,
            state = state,
        )
    }
    val renderedPositions = remember(projectedPositions, baseTransform, state.offset, entryProgress) {
        interpolateEntryPositions(
            finalPositions = projectedPositions,
            center = baseTransform.canvasCenter + state.offset,
            progress = entryProgress,
        )
    }
    val nodeRadii = remember(animatedNodeStyles, state.scale) {
        animatedNodeStyles.map { style ->
            scaledNodeRadius(style.radius, state.scale)
        }
    }
    val renderedNodeRadii = remember(nodeRadii, entryProgress) {
        val appearance = nodeAppearanceScale(entryProgress)
        nodeRadii.map { radius -> radius * appearance }
    }

    var interactionModifier: Modifier = Modifier
    if (options.enablePanZoom) {
        interactionModifier = interactionModifier.pointerInput(options.enablePanZoom, interaction) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                state.applyTransform(
                    centroid = centroid,
                    pan = pan,
                    zoom = zoom,
                    minScale = interaction.resolvedMinScale,
                    maxScale = interaction.resolvedMaxScale,
                )
            }
        }
    }
    if (options.enableTapSelection) {
        val hitPadding = interaction.resolvedTapSelectionPadding
        interactionModifier = interactionModifier.pointerInput(
            options.enableTapSelection,
            renderedPositions,
            renderedNodeRadii,
            interaction,
        ) {
            detectTapGestures { tapOffset ->
                val selectedNodeId = findNodeAt(
                    pointerPosition = tapOffset,
                    projectedPositions = renderedPositions,
                    radiusProvider = { nodeId ->
                        renderedNodeRadii.getOrElse(nodeId) { 12f } + hitPadding
                    },
                )
                if (selectedNodeId == null && !interaction.clearSelectionOnBackgroundTap) {
                    return@detectTapGestures
                }
                state.updateSelectedNodeId(selectedNodeId)
                val selectedKey = selectedNodeId?.let { nodeId ->
                    graphModel.nodeKeys.getOrNull(nodeId)
                }
                onSelectionChange(selectedKey)
                if (selectedKey != null) {
                    onNodeClick(selectedKey)
                }
            }
        }
    }

    val renderPriority = remember(renderableEdges, graphModel.nodeCount, state.selectedNodeId) {
        buildRenderPriority(
            renderableEdges = renderableEdges,
            nodeCount = graphModel.nodeCount,
            selectedNodeId = state.selectedNodeId,
        )
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .then(interactionModifier)
            .onSizeChanged { canvasSize = it },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGraphEdges(
                renderableEdges = renderableEdges,
                edgeStyles = animatedEdgeStyles,
                projectedPositions = renderedPositions,
                nodeRadii = renderedNodeRadii,
                drawProgress = entryProgress,
                edgeIndices = renderPriority.backgroundEdgeIndices,
            )
            drawGraphNodes(
                projectedPositions = renderedPositions,
                nodeStyles = animatedNodeStyles,
                nodeRadii = renderedNodeRadii,
                drawProgress = entryProgress,
                nodeIndices = renderPriority.backgroundNodeIndices,
            )
            if (renderPriority.foregroundEdgeIndices.isNotEmpty()) {
                drawGraphEdges(
                    renderableEdges = renderableEdges,
                    edgeStyles = animatedEdgeStyles,
                    projectedPositions = renderedPositions,
                    nodeRadii = renderedNodeRadii,
                    drawProgress = entryProgress,
                    edgeIndices = renderPriority.foregroundEdgeIndices,
                )
            }
            if (renderPriority.foregroundNodeIndices.isNotEmpty()) {
                drawGraphNodes(
                    projectedPositions = renderedPositions,
                    nodeStyles = animatedNodeStyles,
                    nodeRadii = renderedNodeRadii,
                    drawProgress = entryProgress,
                    nodeIndices = renderPriority.foregroundNodeIndices,
                )
            }
        }

        GraphLabels(
            projectedPositions = renderedPositions,
            nodeStyles = animatedNodeStyles,
            nodeRadii = renderedNodeRadii,
            labels = nodeLabels,
            alpha = (labelAlpha * entryProgress).coerceIn(0f, 1f),
            config = label,
        )
    }
}

private fun <K> collectAllNodeKeys(adjacency: Map<K, List<K>>): List<K> {
    val ordered = linkedSetOf<K>()
    adjacency.forEach { (from, neighbors) ->
        ordered += from
        neighbors.forEach { to -> ordered += to }
    }
    return ordered.toList()
}

private data class RenderPriority(
    val backgroundEdgeIndices: List<Int>,
    val foregroundEdgeIndices: List<Int>,
    val backgroundNodeIndices: List<Int>,
    val foregroundNodeIndices: List<Int>,
)

private fun <K> buildRenderPriority(
    renderableEdges: List<RenderableEdge<K>>,
    nodeCount: Int,
    selectedNodeId: Int?,
): RenderPriority {
    if (selectedNodeId == null || selectedNodeId !in 0 until nodeCount) {
        return RenderPriority(
            backgroundEdgeIndices = renderableEdges.indices.toList(),
            foregroundEdgeIndices = emptyList(),
            backgroundNodeIndices = (0 until nodeCount).toList(),
            foregroundNodeIndices = emptyList(),
        )
    }

    val backgroundEdgeIndices = mutableListOf<Int>()
    val foregroundEdgeIndices = mutableListOf<Int>()
    val highlightedNodeIds = linkedSetOf(selectedNodeId)

    renderableEdges.forEachIndexed { index, edge ->
        val isHighlighted = edge.fromId == selectedNodeId || edge.toId == selectedNodeId
        if (isHighlighted) {
            foregroundEdgeIndices += index
            highlightedNodeIds += edge.fromId
            highlightedNodeIds += edge.toId
        } else {
            backgroundEdgeIndices += index
        }
    }

    val foregroundNodeIndices = highlightedNodeIds
        .filter { it in 0 until nodeCount }
        .sorted()
    val highlightedNodeSet = foregroundNodeIndices.toSet()
    val backgroundNodeIndices = (0 until nodeCount)
        .filter { it !in highlightedNodeSet }

    return RenderPriority(
        backgroundEdgeIndices = backgroundEdgeIndices,
        foregroundEdgeIndices = foregroundEdgeIndices,
        backgroundNodeIndices = backgroundNodeIndices,
        foregroundNodeIndices = foregroundNodeIndices,
    )
}

@Composable
private fun animatedNodeStyles(
    styles: List<NodeStyle>,
    enabled: Boolean,
    durationMillis: Int,
): List<NodeStyle> {
    if (!enabled) {
        return styles
    }

    return styles.mapIndexed { index, style ->
        val fillColor by animateColorAsState(
            targetValue = style.fillColor,
            animationSpec = tween(durationMillis),
            label = "gv-node-fill-$index",
        )
        val strokeColor by animateColorAsState(
            targetValue = style.strokeColor,
            animationSpec = tween(durationMillis),
            label = "gv-node-stroke-$index",
        )
        val labelColor by animateColorAsState(
            targetValue = style.labelColor,
            animationSpec = tween(durationMillis),
            label = "gv-node-label-$index",
        )
        val radius by animateFloatAsState(
            targetValue = style.radius,
            animationSpec = tween(durationMillis),
            label = "gv-node-radius-$index",
        )

        style.copy(
            fillColor = fillColor,
            strokeColor = strokeColor,
            labelColor = labelColor,
            radius = radius,
        )
    }
}

@Composable
private fun animatedEdgeStyles(
    styles: List<EdgeStyle>,
    enabled: Boolean,
    durationMillis: Int,
): List<EdgeStyle> {
    if (!enabled) {
        return styles
    }

    return styles.mapIndexed { index, style ->
        val color by animateColorAsState(
            targetValue = style.color,
            animationSpec = tween(durationMillis),
            label = "gv-edge-color-$index",
        )
        val width by animateFloatAsState(
            targetValue = style.width,
            animationSpec = tween(durationMillis),
            label = "gv-edge-width-$index",
        )
        style.copy(
            color = color,
            width = width,
        )
    }
}

@Composable
private fun animatedLayoutPositions(
    targetPositions: List<androidx.compose.ui.geometry.Offset>,
    enabled: Boolean,
    durationMillis: Int,
): List<androidx.compose.ui.geometry.Offset> {
    var startPositions by remember { mutableStateOf(targetPositions) }
    var endPositions by remember { mutableStateOf(targetPositions) }
    val transitionProgress = remember { Animatable(1f) }

    LaunchedEffect(targetPositions, enabled, durationMillis) {
        if (!enabled) {
            startPositions = targetPositions
            endPositions = targetPositions
            transitionProgress.snapTo(1f)
            return@LaunchedEffect
        }
        if (
            startPositions.size != targetPositions.size ||
            endPositions.size != targetPositions.size
        ) {
            startPositions = targetPositions
            endPositions = targetPositions
            transitionProgress.snapTo(1f)
            return@LaunchedEffect
        }

        startPositions = interpolateLayoutPositions(
            from = startPositions,
            to = endPositions,
            progress = transitionProgress.value,
        )
        endPositions = targetPositions
        transitionProgress.snapTo(0f)
        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis.coerceAtLeast(1),
                easing = FastOutSlowInEasing,
            ),
        )
    }

    return if (!enabled) {
        targetPositions
    } else {
        interpolateLayoutPositions(
            from = startPositions,
            to = endPositions,
            progress = transitionProgress.value,
        )
    }
}

private fun interpolateLayoutPositions(
    from: List<androidx.compose.ui.geometry.Offset>,
    to: List<androidx.compose.ui.geometry.Offset>,
    progress: Float,
): List<androidx.compose.ui.geometry.Offset> {
    if (from.size != to.size) {
        return to
    }
    val t = progress.coerceIn(0f, 1f)
    if (t >= 0.999f) {
        return to
    }
    if (t <= 0.001f) {
        return from
    }
    val size = min(from.size, to.size)
    return List(size) { index ->
        val start = from[index]
        val end = to[index]
        androidx.compose.ui.geometry.Offset(
            x = start.x + (end.x - start.x) * t,
            y = start.y + (end.y - start.y) * t,
        )
    }
}

@Composable
private fun animatedLabelAlpha(
    scale: Float,
    enabled: Boolean,
    threshold: Float,
    durationMillis: Int,
): Float {
    if (!enabled) {
        return 1f
    }

    val targetAlpha = if (scale < threshold) 0f else 1f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis),
        label = "gv-label-alpha",
    )
    return alpha
}

@Composable
private fun initialRenderProgress(
    enabled: Boolean,
    signature: Int,
    durationMillis: Int,
): Float {
    val animatable = remember(signature, enabled) {
        Animatable(if (enabled) 0f else 1f)
    }

    LaunchedEffect(signature, enabled, durationMillis) {
        if (!enabled) {
            animatable.snapTo(1f)
            return@LaunchedEffect
        }
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis.coerceAtLeast(1),
                easing = FastOutSlowInEasing,
            ),
        )
    }

    return animatable.value
}

private fun interpolateEntryPositions(
    finalPositions: List<androidx.compose.ui.geometry.Offset>,
    center: androidx.compose.ui.geometry.Offset,
    progress: Float,
): List<androidx.compose.ui.geometry.Offset> {
    val t = progress.coerceIn(0f, 1f)
    if (t >= 0.999f) {
        return finalPositions
    }

    return finalPositions.map { target ->
        androidx.compose.ui.geometry.Offset(
            x = center.x + (target.x - center.x) * t,
            y = center.y + (target.y - center.y) * t,
        )
    }
}

private fun nodeAppearanceScale(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return 0.4f + (0.6f * clamped)
}

@Composable
private fun GraphLabels(
    projectedPositions: List<androidx.compose.ui.geometry.Offset>,
    nodeStyles: List<NodeStyle>,
    nodeRadii: List<Float>,
    labels: List<String>,
    alpha: Float,
    config: GraphLabelConfig,
) {
    val labelWidth = config.resolvedWidthDp.dp
    val fontSize = config.resolvedFontSizeSp.sp
    val verticalPaddingPx = with(LocalDensity.current) { config.resolvedVerticalPaddingDp.dp.toPx() }
    val labelWidthPx = with(LocalDensity.current) { labelWidth.toPx() }

    projectedPositions.forEachIndexed { nodeId, position ->
        val style = nodeStyles.getOrElse(nodeId) { nodeStyleFromInfo(defaultNodeInfo(""), isSelected = false) }
        val radius = nodeRadii.getOrElse(nodeId) { style.radius }
        val labelText = labels.getOrElse(nodeId) { "Node $nodeId" }
        if (labelText.isBlank()) {
            return@forEachIndexed
        }
        BasicText(
            text = labelText,
            style = TextStyle(
                color = style.labelColor,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .alpha(alpha)
                .offset {
                    IntOffset(
                        x = (position.x - (labelWidthPx * 0.5f)).roundToInt(),
                        y = (position.y + radius + verticalPaddingPx).roundToInt(),
                    )
                }
                .width(labelWidth),
        )
    }
}
