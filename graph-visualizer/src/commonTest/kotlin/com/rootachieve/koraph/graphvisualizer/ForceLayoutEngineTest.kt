package com.rootachieve.koraph.graphvisualizer

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForceLayoutEngineTest {

    @Test
    fun computeForceLayout_isDeterministicForSameSeed() {
        // given
        val edges = listOf(
            LayoutEdge(0, 1),
            LayoutEdge(1, 2),
            LayoutEdge(2, 3),
            LayoutEdge(3, 0),
        )
        val config = ForceLayoutConfig(
            iterations = 120,
            randomSeed = 2026,
        )

        // when
        val first = computeForceLayout(nodeCount = 4, edges = edges, config = config)
        val second = computeForceLayout(nodeCount = 4, edges = edges, config = config)

        // then
        assertEquals(first.positions.size, second.positions.size)
        assertEquals(first.iterationsPerformed, second.iterationsPerformed)

        first.positions.indices.forEach { index ->
            assertClose(first.positions[index].x, second.positions[index].x)
            assertClose(first.positions[index].y, second.positions[index].y)
        }
    }

    @Test
    fun computeForceLayout_stopsEarlyWhenConvergenceThresholdIsHigh() {
        // given
        val edges = listOf(
            LayoutEdge(0, 1),
            LayoutEdge(1, 2),
        )
        val config = ForceLayoutConfig(
            iterations = 300,
            convergenceThreshold = 9999f,
            randomSeed = 123,
        )

        // when
        val result = computeForceLayout(nodeCount = 3, edges = edges, config = config)

        // then
        assertEquals(1, result.iterationsPerformed)
    }

    @Test
    fun computeForceLayout_centerTensionCompactsDisconnectedComponents() {
        // given
        val disconnectedEdges = listOf(
            LayoutEdge(0, 1),
            LayoutEdge(1, 2),
            LayoutEdge(2, 3),
            LayoutEdge(4, 5),
            LayoutEdge(5, 6),
            LayoutEdge(6, 7),
        )
        val baseConfig = ForceLayoutConfig(
            iterations = 180,
            centerTension = 0f,
            randomSeed = 77,
        )
        val compactConfig = ForceLayoutConfig(
            iterations = 180,
            centerTension = 0.06f,
            randomSeed = 77,
        )

        // when
        val base = computeForceLayout(
            nodeCount = 8,
            edges = disconnectedEdges,
            config = baseConfig,
        )
        val compact = computeForceLayout(
            nodeCount = 8,
            edges = disconnectedEdges,
            config = compactConfig,
        )

        val baseMaxRadius = base.positions.maxOf { position ->
            sqrt(position.x * position.x + position.y * position.y)
        }
        val compactMaxRadius = compact.positions.maxOf { position ->
            sqrt(position.x * position.x + position.y * position.y)
        }

        // then
        assertTrue(compactMaxRadius < baseMaxRadius)
    }

    @Test
    fun computeForceLayout_collisionAvoidanceIncreasesMinimumSpacing() {
        // given
        val edges = listOf(
            LayoutEdge(0, 1),
            LayoutEdge(1, 2),
            LayoutEdge(2, 3),
            LayoutEdge(3, 4),
        )
        val nodeRadii = listOf(20f, 20f, 20f, 20f, 20f)
        val withoutCollisionConfig = ForceLayoutConfig(
            iterations = 180,
            nodeRepulsion = 250f,
            edgeTension = 0.075f,
            baseEdgeLength = 24f,
            collisionPadding = 0f,
            collisionStrength = 0f,
            randomSeed = 99,
        )
        val withCollisionConfig = ForceLayoutConfig(
            iterations = 180,
            nodeRepulsion = 250f,
            edgeTension = 0.075f,
            baseEdgeLength = 24f,
            collisionPadding = 10f,
            collisionStrength = 0.9f,
            randomSeed = 99,
        )

        // when
        val withoutCollision = computeForceLayout(
            nodeCount = 5,
            edges = edges,
            config = withoutCollisionConfig,
            nodeRadii = nodeRadii,
        )
        val withCollision = computeForceLayout(
            nodeCount = 5,
            edges = edges,
            config = withCollisionConfig,
            nodeRadii = nodeRadii,
        )

        val noCollisionMinDistance = minimumPairDistance(withoutCollision.positions)
        val withCollisionMinDistance = minimumPairDistance(withCollision.positions)

        // then
        assertTrue(withCollisionMinDistance > noCollisionMinDistance)
    }

    @Test
    fun computeForceLayout_edgeDistanceScaleExpandsEdgeLength() {
        // given
        val edges = listOf(
            LayoutEdge(0, 1),
            LayoutEdge(1, 2),
            LayoutEdge(2, 3),
            LayoutEdge(3, 0),
        )
        val baseConfig = ForceLayoutConfig(
            iterations = 220,
            randomSeed = 314,
            edgeDistanceScale = 1f,
        )
        val compactConfig = baseConfig.copy(edgeDistanceScale = 0.8f)
        val expandedConfig = baseConfig.copy(edgeDistanceScale = 1.8f)

        // when
        val compact = computeForceLayout(
            nodeCount = 4,
            edges = edges,
            config = compactConfig,
        )
        val expanded = computeForceLayout(
            nodeCount = 4,
            edges = edges,
            config = expandedConfig,
        )

        val compactAverage = averageEdgeLength(compact.positions, edges)
        val expandedAverage = averageEdgeLength(expanded.positions, edges)

        // then
        assertTrue(expandedAverage > compactAverage * 1.8f)
    }

    @Test
    fun buildLayoutEdges_deduplicatesBidirectionalAndSelfEdges() {
        // given
        val edges = listOf(
            GraphEdge(fromId = 0, toId = 1, from = "a", to = "b"),
            GraphEdge(fromId = 1, toId = 0, from = "b", to = "a"),
            GraphEdge(fromId = 0, toId = 1, from = "a", to = "b"),
            GraphEdge(fromId = 2, toId = 2, from = "c", to = "c"),
        )

        // when
        val layoutEdges = buildLayoutEdges(edges)

        // then
        assertEquals(listOf(LayoutEdge(0, 1)), layoutEdges)
    }

    @Test
    fun scaledNodeRadius_clampsToSafeRange() {
        // given
        val tooSmallBaseRadius = 2f
        val normalBaseRadius = 18f
        val tooLargeBaseRadius = 30f

        // when
        val clampedSmall = scaledNodeRadius(baseRadius = tooSmallBaseRadius, currentScale = 0.1f)
        val unchangedNormal = scaledNodeRadius(baseRadius = normalBaseRadius, currentScale = 1f)
        val clampedLarge = scaledNodeRadius(baseRadius = tooLargeBaseRadius, currentScale = 2f)

        // then
        assertEquals(8f, clampedSmall)
        assertEquals(44f, clampedLarge)
        assertEquals(18f, unchangedNormal)
    }

    @Test
    fun calculateBaseTransform_returnsFiniteScale() {
        // given
        val positions = listOf(
            androidx.compose.ui.geometry.Offset(-10f, -20f),
            androidx.compose.ui.geometry.Offset(40f, 30f),
        )

        // when
        val transform = calculateBaseTransform(
            positions = positions,
            canvasSize = androidx.compose.ui.unit.IntSize(1080, 720),
        )

        // then
        assertTrue(transform.baseScale.isFinite())
        assertTrue(transform.baseScale > 0f)
    }

    @Test
    fun calculateBaseTransform_canDisableAutoFit() {
        // given
        val positions = listOf(
            androidx.compose.ui.geometry.Offset(-200f, -200f),
            androidx.compose.ui.geometry.Offset(300f, 300f),
        )

        // when
        val transform = calculateBaseTransform(
            positions = positions,
            canvasSize = androidx.compose.ui.unit.IntSize(1080, 720),
            fitToBounds = false,
        )

        // then
        assertEquals(1f, transform.baseScale)
    }

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.0001f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "Expected $expected but was $actual (tol=$tolerance)",
        )
    }

    private fun minimumPairDistance(positions: List<androidx.compose.ui.geometry.Offset>): Float {
        var minDistance = Float.POSITIVE_INFINITY
        for (i in positions.indices) {
            for (j in i + 1 until positions.size) {
                val dx = positions[j].x - positions[i].x
                val dy = positions[j].y - positions[i].y
                val distance = sqrt(dx * dx + dy * dy)
                minDistance = min(minDistance, distance)
            }
        }
        return minDistance
    }

    private fun averageEdgeLength(
        positions: List<androidx.compose.ui.geometry.Offset>,
        edges: List<LayoutEdge>,
    ): Float {
        if (edges.isEmpty()) {
            return 0f
        }
        var total = 0f
        edges.forEach { edge ->
            val from = positions[edge.fromId]
            val to = positions[edge.toId]
            val dx = to.x - from.x
            val dy = to.y - from.y
            total += sqrt(dx * dx + dy * dy)
        }
        return total / edges.size
    }
}
