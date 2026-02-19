package com.rootachieve.koraph

import androidx.compose.ui.graphics.Color
import com.rootachieve.koraph.graphvisualizer.NodeInfo
import com.rootachieve.koraph.graphvisualizer.NodeShape

internal data class VisualizerSampleGraphData(
    val adjacency: Map<SampleNode, List<SampleNode>>,
    val nodeInfo: Map<SampleNode, NodeInfo>,
    val groupByNode: Map<SampleNode, SampleGroup>,
    val edgeTypeByKey: Map<SampleEdgeKey, SampleEdgeType>,
    val edgeWeightByKey: Map<SampleEdgeKey, Float>,
    val neighborsByNode: Map<SampleNode, Set<SampleNode>>,
)

internal data class SampleEdgeDef(
    val from: SampleNode,
    val to: SampleNode,
    val type: SampleEdgeType,
    val weight: Float,
)

internal data class SampleEdgeKey(
    val first: SampleNode,
    val second: SampleNode,
)

internal data class EdgeWeightRange(
    val min: Float,
    val max: Float,
)

internal enum class SampleGroup(
    val baseColor: Color,
) {
    Core(baseColor = Color(0xFFE9D5FF)),
    Feature(baseColor = Color(0xFFBBF7D0)),
    Infra(baseColor = Color(0xFFBFDBFE)),
    Support(baseColor = Color(0xFFFDE68A)),
}

internal enum class SampleEdgeType {
    Related,
    Critical,
    DataFlow,
}

internal enum class SampleNode {
    Gateway,
    Auth,
    Profile,
    Feed,
    Search,
    Messaging,
    Payments,
    Notifications,
    Analytics,
    Storage,
}

private object SampleDataDefaults {
    const val minEdgeWeight: Float = 1f
    const val maxEdgeWeight: Float = 3f
    const val nodeSize: Float = 18f
}

internal fun buildVisualizerSampleGraphData(): VisualizerSampleGraphData {
    val edgeDefs = listOf(
        SampleEdgeDef(SampleNode.Gateway, SampleNode.Auth, SampleEdgeType.Critical, 2.8f),
        SampleEdgeDef(SampleNode.Gateway, SampleNode.Feed, SampleEdgeType.Related, 1.6f),
        SampleEdgeDef(SampleNode.Gateway, SampleNode.Search, SampleEdgeType.Related, 1.5f),
        SampleEdgeDef(SampleNode.Auth, SampleNode.Profile, SampleEdgeType.Critical, 2.4f),
        SampleEdgeDef(SampleNode.Profile, SampleNode.Feed, SampleEdgeType.Related, 1.4f),
        SampleEdgeDef(SampleNode.Feed, SampleNode.Notifications, SampleEdgeType.DataFlow, 1.9f),
        SampleEdgeDef(SampleNode.Search, SampleNode.Analytics, SampleEdgeType.DataFlow, 1.8f),
        SampleEdgeDef(SampleNode.Search, SampleNode.Storage, SampleEdgeType.DataFlow, 2.2f),
        SampleEdgeDef(SampleNode.Payments, SampleNode.Auth, SampleEdgeType.Critical, 2.6f),
        SampleEdgeDef(SampleNode.Payments, SampleNode.Storage, SampleEdgeType.DataFlow, 2.5f),
        SampleEdgeDef(SampleNode.Messaging, SampleNode.Notifications, SampleEdgeType.Related, 1.3f),
        SampleEdgeDef(SampleNode.Messaging, SampleNode.Profile, SampleEdgeType.Related, 1.2f),
        SampleEdgeDef(SampleNode.Analytics, SampleNode.Storage, SampleEdgeType.DataFlow, 1.7f),
    )

    val groups = mapOf(
        SampleNode.Gateway to SampleGroup.Core,
        SampleNode.Auth to SampleGroup.Core,
        SampleNode.Profile to SampleGroup.Feature,
        SampleNode.Feed to SampleGroup.Feature,
        SampleNode.Search to SampleGroup.Feature,
        SampleNode.Messaging to SampleGroup.Feature,
        SampleNode.Payments to SampleGroup.Feature,
        SampleNode.Notifications to SampleGroup.Support,
        SampleNode.Analytics to SampleGroup.Support,
        SampleNode.Storage to SampleGroup.Infra,
    )

    val names = SampleNode.entries.associateWith { node -> node.name }

    val adjacencyMutable = SampleNode.entries
        .associateWith { linkedSetOf<SampleNode>() }
        .toMutableMap()

    val edgeTypes = mutableMapOf<SampleEdgeKey, SampleEdgeType>()
    val edgeWeights = mutableMapOf<SampleEdgeKey, Float>()

    edgeDefs.forEach { edge ->
        adjacencyMutable.getValue(edge.from).add(edge.to)
        adjacencyMutable.getValue(edge.to).add(edge.from)

        val edgeKey = sampleEdgeKey(edge.from, edge.to)
        edgeTypes[edgeKey] = edge.type

        val resolvedWeight = edge.weight.coerceIn(
            minimumValue = SampleDataDefaults.minEdgeWeight,
            maximumValue = SampleDataDefaults.maxEdgeWeight,
        )

        edgeWeights[edgeKey] = maxOf(edgeWeights[edgeKey] ?: resolvedWeight, resolvedWeight)
    }

    val adjacency = adjacencyMutable.mapValues { (_, neighbors) -> neighbors.toList() }
    val nodeInfo = SampleNode.entries.associateWith { node ->
        NodeInfo(
            name = names.getValue(node),
            style = NodeShape.Circle,
            size = SampleDataDefaults.nodeSize,
        )
    }
    val neighbors = adjacency.mapValues { (_, list) -> list.toSet() }

    return VisualizerSampleGraphData(
        adjacency = adjacency,
        nodeInfo = nodeInfo,
        groupByNode = groups,
        edgeTypeByKey = edgeTypes,
        edgeWeightByKey = edgeWeights,
        neighborsByNode = neighbors,
    )
}

internal fun sampleEdgeKey(a: SampleNode, b: SampleNode): SampleEdgeKey {
    return if (a.ordinal <= b.ordinal) {
        SampleEdgeKey(a, b)
    } else {
        SampleEdgeKey(b, a)
    }
}
