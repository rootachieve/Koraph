package com.rootachieve.koraph.graphvisualizer

internal data class GraphEdge<K>(
    val fromId: Int,
    val toId: Int,
    val from: K,
    val to: K,
)

internal data class GraphModel<K>(
    val nodeKeys: List<K>,
    val nodeInfoByKey: Map<K, NodeInfo>,
    val edges: List<GraphEdge<K>>,
) {
    val nodeCount: Int
        get() = nodeKeys.size
}

internal data class LayoutEdge(
    val fromId: Int,
    val toId: Int,
)

internal data class RenderableEdge<K>(
    val fromId: Int,
    val toId: Int,
    val from: K,
    val to: K,
    val drawArrow: Boolean,
)

internal fun <K> adjacencySignature(
    adjacency: Map<K, List<K>>,
    nodeInfo: Map<K, NodeInfo>,
): Int {
    return (adjacency.hashCode() * 31) + nodeInfo.hashCode()
}

internal fun <K> buildGraphModel(
    adjacency: Map<K, List<K>>,
    nodeInfo: Map<K, NodeInfo>,
    fallbackNodeInfo: (K) -> NodeInfo = { key -> defaultNodeInfo(name = key.toString()) },
): GraphModel<K> {
    val orderedKeys = linkedSetOf<K>()
    adjacency.forEach { (from, neighbors) ->
        orderedKeys += from
        neighbors.forEach { to -> orderedKeys += to }
    }
    nodeInfo.keys.forEach { key -> orderedKeys += key }

    val nodeKeys = orderedKeys.toList()
    val nodeInfoByKey = nodeKeys.associateWith { key ->
        nodeInfo[key] ?: fallbackNodeInfo(key)
    }
    val idByKey = nodeKeys.withIndex().associate { (index, key) -> key to index }

    val edges = buildList {
        adjacency.forEach { (from, neighbors) ->
            val fromId = idByKey[from] ?: return@forEach
            neighbors.forEach { to ->
                val toId = idByKey[to] ?: return@forEach
                add(
                    GraphEdge(
                        fromId = fromId,
                        toId = toId,
                        from = from,
                        to = to,
                    ),
                )
            }
        }
    }

    return GraphModel(
        nodeKeys = nodeKeys,
        nodeInfoByKey = nodeInfoByKey,
        edges = edges,
    )
}

internal fun <K> buildLayoutEdges(
    edges: List<GraphEdge<K>>,
): List<LayoutEdge> {
    val seenPairs = mutableSetOf<Long>()
    val result = mutableListOf<LayoutEdge>()

    for (edge in edges) {
        if (edge.fromId == edge.toId) {
            continue
        }
        val first = minOf(edge.fromId, edge.toId)
        val second = maxOf(edge.fromId, edge.toId)
        val key = edgePairKey(first, second)
        if (seenPairs.add(key)) {
            result += LayoutEdge(first, second)
        }
    }

    return result
}

internal fun <K> buildRenderableEdges(
    edges: List<GraphEdge<K>>,
    directed: Boolean,
    showArrows: Boolean,
): List<RenderableEdge<K>> {
    if (directed) {
        return edges.map { edge ->
            RenderableEdge(
                fromId = edge.fromId,
                toId = edge.toId,
                from = edge.from,
                to = edge.to,
                drawArrow = showArrows,
            )
        }
    }

    val seenPairs = mutableSetOf<Long>()
    val result = mutableListOf<RenderableEdge<K>>()

    for (edge in edges) {
        val smaller = minOf(edge.fromId, edge.toId)
        val larger = maxOf(edge.fromId, edge.toId)
        val key = edgePairKey(smaller, larger)
        if (seenPairs.add(key)) {
            val canonicalFrom = if (edge.fromId == smaller) {
                edge.from
            } else {
                edge.to
            }
            val canonicalTo = if (edge.toId == larger) {
                edge.to
            } else {
                edge.from
            }
            result += RenderableEdge(
                fromId = smaller,
                toId = larger,
                from = canonicalFrom,
                to = canonicalTo,
                drawArrow = false,
            )
        }
    }

    return result
}

internal fun <K> resolveNodeStyles(
    graphModel: GraphModel<K>,
    selectedNodeId: Int?,
    styleProvider: (NodeStyleInput<K>) -> NodeStyle,
): List<NodeStyle> {
    return graphModel.nodeKeys.mapIndexed { nodeId, key ->
        val info = graphModel.nodeInfoByKey[key] ?: defaultNodeInfo(name = key.toString())
        val selectionState = when {
            selectedNodeId == null -> SelectionState.NoneSelected
            selectedNodeId == nodeId -> SelectionState.Selected
            else -> SelectionState.OtherSelected
        }
        styleProvider(
            NodeStyleInput(
                nodeId = nodeId,
                key = key,
                nodeInfo = info,
                isSelected = selectedNodeId == nodeId,
                selectionState = selectionState,
            ),
        )
    }
}

internal fun <K> resolveNodeLabels(
    graphModel: GraphModel<K>,
): List<String> {
    return graphModel.nodeKeys.map { key ->
        graphModel.nodeInfoByKey[key]?.name ?: key.toString()
    }
}

internal fun <K> resolveNodeLabels(
    graphModel: GraphModel<K>,
    selectedNodeId: Int?,
    labelProvider: ((NodeStyleInput<K>) -> String?)?,
): List<String> {
    if (labelProvider == null) {
        return resolveNodeLabels(graphModel)
    }

    return graphModel.nodeKeys.mapIndexed { nodeId, key ->
        val info = graphModel.nodeInfoByKey[key] ?: defaultNodeInfo(name = key.toString())
        val selectionState = when {
            selectedNodeId == null -> SelectionState.NoneSelected
            selectedNodeId == nodeId -> SelectionState.Selected
            else -> SelectionState.OtherSelected
        }
        labelProvider(
            NodeStyleInput(
                nodeId = nodeId,
                key = key,
                nodeInfo = info,
                isSelected = selectedNodeId == nodeId,
                selectionState = selectionState,
            ),
        ) ?: ""
    }
}

internal fun <K> resolveEdgeStyles(
    renderableEdges: List<RenderableEdge<K>>,
    selectedNodeId: Int?,
    styleProvider: (EdgeStyleInput<K>) -> EdgeStyle,
): List<EdgeStyle> {
    return renderableEdges.map { edge ->
        val isSelected = selectedNodeId != null &&
            (selectedNodeId == edge.fromId || selectedNodeId == edge.toId)
        val selectionState = when {
            selectedNodeId == null -> SelectionState.NoneSelected
            isSelected -> SelectionState.Selected
            else -> SelectionState.OtherSelected
        }
        styleProvider(
            EdgeStyleInput(
                fromId = edge.fromId,
                toId = edge.toId,
                from = edge.from,
                to = edge.to,
                isSelected = isSelected,
                selectionState = selectionState,
            ),
        )
    }
}

private fun edgePairKey(first: Int, second: Int): Long {
    return (first.toLong() shl 32) or (second.toLong() and 0xFFFFFFFFL)
}
