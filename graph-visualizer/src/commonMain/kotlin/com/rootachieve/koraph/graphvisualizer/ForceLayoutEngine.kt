package com.rootachieve.koraph.graphvisualizer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private object ForceLayoutDefaults {
    const val initialPositionRange: Float = 100f
    const val defaultNodeRadius: Float = 18f
    const val minimumNodeRadius: Float = 4f
    const val minimumBaseEdgeLength: Float = 8f
    const val minimumEdgeDistanceScale: Float = 0.1f
    const val minimumDistanceEpsilon: Float = 0.001f
    const val minimumVelocityMagnitude: Float = 1f
    const val defaultViewportPadding: Float = 48f
    const val minimumWorldExtent: Float = 1f
    const val minimumProjectedScale: Float = 0.01f
    const val minimumHitRadius: Float = 1f
    const val minimumRenderedNodeRadius: Float = 8f
    const val maximumRenderedNodeRadius: Float = 44f
}

internal data class ForceLayoutResult(
    val positions: List<Offset>,
    val iterationsPerformed: Int,
)

internal data class BaseTransform(
    val worldCenter: Offset,
    val canvasCenter: Offset,
    val baseScale: Float,
)

internal fun computeForceLayout(
    nodeCount: Int,
    edges: List<LayoutEdge>,
    config: ForceLayoutConfig,
    nodeRadii: List<Float> = List(nodeCount) { ForceLayoutDefaults.defaultNodeRadius },
): ForceLayoutResult {
    if (nodeCount <= 0) {
        return ForceLayoutResult(emptyList(), 0)
    }

    val rng = SeededRandom(config.randomSeed)
    val x = FloatArray(nodeCount) {
        rng.nextFloat(-ForceLayoutDefaults.initialPositionRange, ForceLayoutDefaults.initialPositionRange)
    }
    val y = FloatArray(nodeCount) {
        rng.nextFloat(-ForceLayoutDefaults.initialPositionRange, ForceLayoutDefaults.initialPositionRange)
    }
    val vx = FloatArray(nodeCount)
    val vy = FloatArray(nodeCount)
    val fx = FloatArray(nodeCount)
    val fy = FloatArray(nodeCount)
    val centerTension = config.centerTension.coerceAtLeast(0f)
    val repulsionExponent = config.repulsionExponent.coerceIn(0.5f, 4f)
    val baseEdgeLength = config.baseEdgeLength.coerceAtLeast(ForceLayoutDefaults.minimumBaseEdgeLength)
    val edgeDistanceScale = config.edgeDistanceScale.coerceAtLeast(ForceLayoutDefaults.minimumEdgeDistanceScale)
    val idealEdgeLength = baseEdgeLength
    val collisionPadding = config.collisionPadding.coerceAtLeast(0f)
    val collisionStrength = config.collisionStrength.coerceAtLeast(0f)
    val maxVelocity = config.maxVelocity.coerceAtLeast(ForceLayoutDefaults.minimumVelocityMagnitude)
    val degreeAwareEdgeTension = config.degreeAwareEdgeTension
    val resolvedRadii = FloatArray(nodeCount) { nodeId ->
        nodeRadii.getOrNull(nodeId)?.coerceAtLeast(ForceLayoutDefaults.minimumNodeRadius)
            ?: ForceLayoutDefaults.defaultNodeRadius
    }
    val degree = IntArray(nodeCount)
    for (edge in edges) {
        if (
            edge.fromId !in 0 until nodeCount ||
            edge.toId !in 0 until nodeCount ||
            edge.fromId == edge.toId
        ) {
            continue
        }
        degree[edge.fromId] += 1
        degree[edge.toId] += 1
    }

    var iterationsPerformed = 0

    for (iteration in 0 until config.iterations.coerceAtLeast(1)) {
        iterationsPerformed = iteration + 1
        fx.fill(0f)
        fy.fill(0f)

        for (i in 0 until nodeCount) {
            for (j in i + 1 until nodeCount) {
                var dx = x[j] - x[i]
                var dy = y[j] - y[i]
                var distSq = dx * dx + dy * dy

                if (distSq < ForceLayoutDefaults.minimumDistanceEpsilon) {
                    dx = rng.nextFloat(-1f, 1f)
                    dy = rng.nextFloat(-1f, 1f)
                    distSq = dx * dx + dy * dy + ForceLayoutDefaults.minimumDistanceEpsilon
                }

                val distance = sqrt(distSq)
                val nx = dx / distance
                val ny = dy / distance
                val repulsionDenominator = distance
                    .toDouble()
                    .pow(repulsionExponent.toDouble())
                    .toFloat()
                    .coerceAtLeast(ForceLayoutDefaults.minimumDistanceEpsilon)
                val repulsiveForce = config.nodeRepulsion / repulsionDenominator

                fx[i] -= nx * repulsiveForce
                fy[i] -= ny * repulsiveForce
                fx[j] += nx * repulsiveForce
                fy[j] += ny * repulsiveForce

                val minimumDistance = resolvedRadii[i] + resolvedRadii[j] + collisionPadding
                if (distance < minimumDistance) {
                    val overlap = (minimumDistance - distance).coerceAtLeast(0f)
                    val collisionForce = overlap * collisionStrength
                    fx[i] -= nx * collisionForce
                    fy[i] -= ny * collisionForce
                    fx[j] += nx * collisionForce
                    fy[j] += ny * collisionForce
                }
            }
        }

        for (edge in edges) {
            val from = edge.fromId
            val to = edge.toId
            if (from !in 0 until nodeCount || to !in 0 until nodeCount || from == to) {
                continue
            }

            val dx = x[to] - x[from]
            val dy = y[to] - y[from]
            val distance = sqrt(max(ForceLayoutDefaults.minimumDistanceEpsilon, dx * dx + dy * dy))
            val nx = dx / distance
            val ny = dy / distance
            val hubDamp = if (degreeAwareEdgeTension) {
                sqrt(max(degree[from], degree[to]).toFloat()).coerceAtLeast(1f)
            } else {
                1f
            }
            val attractiveForce = config.edgeTension * (distance - idealEdgeLength) / hubDamp

            fx[from] += nx * attractiveForce
            fy[from] += ny * attractiveForce
            fx[to] -= nx * attractiveForce
            fy[to] -= ny * attractiveForce
        }

        if (centerTension > 0f) {
            for (nodeId in 0 until nodeCount) {
                // Keeps disconnected components from drifting too far apart.
                fx[nodeId] -= x[nodeId] * centerTension
                fy[nodeId] -= y[nodeId] * centerTension
            }
        }

        var maxDelta = 0f
        for (nodeId in 0 until nodeCount) {
            vx[nodeId] = ((vx[nodeId] + fx[nodeId]) * config.damping)
                .coerceIn(-maxVelocity, maxVelocity)
            vy[nodeId] = ((vy[nodeId] + fy[nodeId]) * config.damping)
                .coerceIn(-maxVelocity, maxVelocity)

            x[nodeId] += vx[nodeId]
            y[nodeId] += vy[nodeId]

            val delta = sqrt(vx[nodeId] * vx[nodeId] + vy[nodeId] * vy[nodeId])
            if (delta > maxDelta) {
                maxDelta = delta
            }
        }

        if (maxDelta < config.convergenceThreshold) {
            break
        }
    }

    val positions = List(nodeCount) { index ->
        Offset(
            x = x[index] * edgeDistanceScale,
            y = y[index] * edgeDistanceScale,
        )
    }

    return ForceLayoutResult(
        positions = positions,
        iterationsPerformed = iterationsPerformed,
    )
}

internal fun calculateBaseTransform(
    positions: List<Offset>,
    canvasSize: IntSize,
    padding: Float = ForceLayoutDefaults.defaultViewportPadding,
    fitToBounds: Boolean = true,
): BaseTransform {
    if (positions.isEmpty() || canvasSize.width <= 0 || canvasSize.height <= 0) {
        return BaseTransform(
            worldCenter = Offset.Zero,
            canvasCenter = Offset(
                x = canvasSize.width * 0.5f,
                y = canvasSize.height * 0.5f,
            ),
            baseScale = 1f,
        )
    }

    var minX = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    for (position in positions) {
        minX = min(minX, position.x)
        maxX = max(maxX, position.x)
        minY = min(minY, position.y)
        maxY = max(maxY, position.y)
    }

    val scale = if (fitToBounds) {
        val worldWidth = (maxX - minX).coerceAtLeast(ForceLayoutDefaults.minimumWorldExtent)
        val worldHeight = (maxY - minY).coerceAtLeast(ForceLayoutDefaults.minimumWorldExtent)
        val usableWidth = (canvasSize.width - (padding * 2f)).coerceAtLeast(ForceLayoutDefaults.minimumWorldExtent)
        val usableHeight = (canvasSize.height - (padding * 2f)).coerceAtLeast(ForceLayoutDefaults.minimumWorldExtent)
        min(usableWidth / worldWidth, usableHeight / worldHeight)
            .takeIf { it.isFinite() && it > 0f }
            ?: 1f
    } else {
        1f
    }

    return BaseTransform(
        worldCenter = Offset((minX + maxX) * 0.5f, (minY + maxY) * 0.5f),
        canvasCenter = Offset(canvasSize.width * 0.5f, canvasSize.height * 0.5f),
        baseScale = scale,
    )
}

internal fun projectPositions(
    positions: List<Offset>,
    baseTransform: BaseTransform,
    state: GraphVisualizerState,
): List<Offset> {
    val scaled = (baseTransform.baseScale * state.scale).coerceAtLeast(ForceLayoutDefaults.minimumProjectedScale)
    return positions.map { world ->
        val dx = (world.x - baseTransform.worldCenter.x) * scaled
        val dy = (world.y - baseTransform.worldCenter.y) * scaled
        Offset(
            x = baseTransform.canvasCenter.x + state.offset.x + dx,
            y = baseTransform.canvasCenter.y + state.offset.y + dy,
        )
    }
}

internal fun findNodeAt(
    pointerPosition: Offset,
    projectedPositions: List<Offset>,
    radiusProvider: (nodeId: Int) -> Float,
): Int? {
    var selectedNode: Int? = null
    var selectedDistanceSq = Float.POSITIVE_INFINITY

    for (nodeId in projectedPositions.indices) {
        val nodePosition = projectedPositions[nodeId]
        val dx = pointerPosition.x - nodePosition.x
        val dy = pointerPosition.y - nodePosition.y
        val distanceSq = dx * dx + dy * dy
        val hitRadius = radiusProvider(nodeId).coerceAtLeast(ForceLayoutDefaults.minimumHitRadius)

        if (distanceSq <= hitRadius * hitRadius && distanceSq < selectedDistanceSq) {
            selectedDistanceSq = distanceSq
            selectedNode = nodeId
        }
    }

    return selectedNode
}

internal fun scaledNodeRadius(baseRadius: Float, currentScale: Float): Float {
    return (baseRadius * currentScale).coerceIn(
        ForceLayoutDefaults.minimumRenderedNodeRadius,
        ForceLayoutDefaults.maximumRenderedNodeRadius,
    )
}

private class SeededRandom(seed: Int) {
    private var state = seed.toLong() and Mask

    fun nextFloat(minValue: Float, maxValue: Float): Float {
        state = (LcgMultiplier * state + LcgIncrement) and Mask
        val normalized = (state.toDouble() / Mask.toDouble()).toFloat()
        return minValue + (maxValue - minValue) * normalized
    }

    private companion object {
        // Numerical Recipes LCG parameters (32-bit).
        const val LcgMultiplier: Long = 1_664_525L
        const val LcgIncrement: Long = 1_013_904_223L
        const val Mask: Long = 0xFFFF_FFFFL
    }
}
