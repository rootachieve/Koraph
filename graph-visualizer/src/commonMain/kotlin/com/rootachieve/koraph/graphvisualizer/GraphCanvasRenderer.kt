package com.rootachieve.koraph.graphvisualizer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max
import kotlin.math.sqrt

internal fun <D> DrawScope.drawGraphEdges(
    renderableEdges: List<RenderableEdge<D>>,
    edgeStyles: List<EdgeStyle>,
    projectedPositions: List<Offset>,
    nodeRadii: List<Float>,
    drawProgress: Float = 1f,
    edgeIndices: List<Int>? = null,
) {
    val progress = drawProgress.coerceIn(0f, 1f)
    if (progress <= 0f) {
        return
    }

    val indices = edgeIndices ?: renderableEdges.indices
    indices.forEach { index ->
        val edge = renderableEdges.getOrNull(index) ?: return@forEach
        if (edge.fromId !in projectedPositions.indices || edge.toId !in projectedPositions.indices) {
            return@forEach
        }

        val style = edgeStyles.getOrElse(index) { defaultEdgeStyle(isSelected = false) }
        val from = projectedPositions[edge.fromId]
        val to = projectedPositions[edge.toId]
        val radiusFrom = nodeRadii.getOrElse(edge.fromId) { 12f }
        val radiusTo = nodeRadii.getOrElse(edge.toId) { 12f }

        val dx = to.x - from.x
        val dy = to.y - from.y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance < 0.001f) {
            return@forEach
        }

        val direction = Offset(dx / distance, dy / distance)
        val start = Offset(
            x = from.x + direction.x * radiusFrom,
            y = from.y + direction.y * radiusFrom,
        )
        val end = Offset(
            x = to.x - direction.x * radiusTo,
            y = to.y - direction.y * radiusTo,
        )
        val animatedEnd = Offset(
            x = start.x + (end.x - start.x) * progress,
            y = start.y + (end.y - start.y) * progress,
        )

        drawLine(
            color = style.color,
            start = start,
            end = animatedEnd,
            strokeWidth = style.width.coerceAtLeast(1f),
            cap = StrokeCap.Round,
            pathEffect = if (style.dashed) {
                PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            } else {
                null
            },
        )

        if (edge.drawArrow && progress >= 0.96f) {
            drawArrowHead(
                tip = animatedEnd,
                direction = direction,
                color = style.color,
                strokeWidth = style.width,
            )
        }
    }
}

internal fun DrawScope.drawGraphNodes(
    projectedPositions: List<Offset>,
    nodeStyles: List<NodeStyle>,
    nodeRadii: List<Float>,
    drawProgress: Float = 1f,
    nodeIndices: List<Int>? = null,
) {
    val progress = drawProgress.coerceIn(0f, 1f)
    if (progress <= 0f) {
        return
    }

    val indices = nodeIndices ?: projectedPositions.indices
    indices.forEach { nodeId ->
        val center = projectedPositions.getOrNull(nodeId) ?: return@forEach
        val style = nodeStyles.getOrElse(nodeId) {
            nodeStyleFromInfo(defaultNodeInfo(name = ""), isSelected = false)
        }
        val radius = nodeRadii.getOrElse(nodeId) { style.radius }
        val strokeWidth = max(1.25f, radius * 0.1f)
        val fillColor = style.fillColor.copy(alpha = style.fillColor.alpha * progress)
        val strokeColor = style.strokeColor.copy(alpha = style.strokeColor.alpha * progress)

        drawNodeShape(
            center = center,
            radius = radius,
            shape = style.shape,
            color = fillColor,
            drawStyle = Fill,
        )

        drawNodeShape(
            center = center,
            radius = radius,
            shape = style.shape,
            color = strokeColor,
            drawStyle = Stroke(strokeWidth),
        )
    }
}

private fun DrawScope.drawNodeShape(
    center: Offset,
    radius: Float,
    shape: NodeShape,
    color: Color,
    drawStyle: DrawStyle,
) {
    when (shape) {
        NodeShape.Circle -> {
            drawCircle(
                color = color,
                radius = radius,
                center = center,
                style = drawStyle,
            )
        }

        NodeShape.RoundedRect -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                cornerRadius = CornerRadius(radius * 0.35f, radius * 0.35f),
                style = drawStyle,
            )
        }

        NodeShape.Diamond -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius, center.y)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius, center.y)
                close()
            }
            drawPath(path = path, color = color, style = drawStyle)
        }

        NodeShape.Hexagon -> {
            val half = radius * 0.5f
            val path = Path().apply {
                moveTo(center.x - half, center.y - radius)
                lineTo(center.x + half, center.y - radius)
                lineTo(center.x + radius, center.y)
                lineTo(center.x + half, center.y + radius)
                lineTo(center.x - half, center.y + radius)
                lineTo(center.x - radius, center.y)
                close()
            }
            drawPath(path = path, color = color, style = drawStyle)
        }

        is NodeShape.Custom -> {
            drawPath(
                path = shape.pathBuilder(center, radius),
                color = color,
                style = drawStyle,
            )
        }
    }
}

private fun DrawScope.drawArrowHead(
    tip: Offset,
    direction: Offset,
    color: Color,
    strokeWidth: Float,
) {
    val arrowLength = max(10f, strokeWidth * 3f)
    val arrowWidth = max(6f, strokeWidth * 2.4f)
    val base = Offset(
        x = tip.x - direction.x * arrowLength,
        y = tip.y - direction.y * arrowLength,
    )
    val perpendicular = Offset(-direction.y, direction.x)

    val left = Offset(
        x = base.x + perpendicular.x * arrowWidth * 0.5f,
        y = base.y + perpendicular.y * arrowWidth * 0.5f,
    )
    val right = Offset(
        x = base.x - perpendicular.x * arrowWidth * 0.5f,
        y = base.y - perpendicular.y * arrowWidth * 0.5f,
    )

    val arrowPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path = arrowPath, color = color, style = Fill)
}
