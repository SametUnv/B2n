package com.board2notes.app.data.image

import com.board2notes.app.domain.model.BooleanMask
import com.board2notes.app.domain.model.PointF2
import com.board2notes.app.domain.model.Quad
import com.board2notes.app.domain.model.RectBox
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class QuadDetection(
    val quad: Quad,
    val boundingBox: RectBox,
    val confidence: Float,
    val strategy: String
)

object MaskQuadDetector {
    private const val MAX_BOUNDARY_POINTS = 14000
    private const val MIN_BOUNDARY_POINTS = 12

    fun detect(mask: BooleanMask): QuadDetection? {
        val box = MaskPostprocessor.boundingBox(mask)
        if (!box.isValid) return null

        val boundary = extractBoundary(mask)
        if (boundary.size < MIN_BOUNDARY_POINTS) {
            return QuadDetection(
                quad = box.toQuad(),
                boundingBox = box,
                confidence = 0.15f,
                strategy = "bbox_fallback"
            )
        }

        val sampled = sampleBoundary(boundary)
        val hull = convexHull(sampled)
        if (hull.size < 4) {
            return QuadDetection(
                quad = box.toQuad(),
                boundingBox = box,
                confidence = 0.2f,
                strategy = "bbox_fallback"
            )
        }

        val extremesQuad = quadFromExtremes(hull)
        if (extremesQuad != null && isSane(extremesQuad, mask.width, mask.height)) {
            return QuadDetection(
                quad = clampQuad(extremesQuad, mask.width, mask.height),
                boundingBox = box,
                confidence = 0.85f,
                strategy = "convex_hull_extremes"
            )
        }

        val oriented = orientedRectangle(sampled)
        return QuadDetection(
            quad = clampQuad(oriented, mask.width, mask.height),
            boundingBox = box,
            confidence = 0.6f,
            strategy = "oriented_rectangle_fallback"
        )
    }

    private fun extractBoundary(mask: BooleanMask): List<IntPoint> {
        val out = ArrayList<IntPoint>()
        for (y in 0 until mask.height) {
            for (x in 0 until mask.width) {
                if (!mask[x, y]) continue
                val boundary = x == 0 ||
                    y == 0 ||
                    x == mask.width - 1 ||
                    y == mask.height - 1 ||
                    !mask[x - 1, y] ||
                    !mask[x + 1, y] ||
                    !mask[x, y - 1] ||
                    !mask[x, y + 1]
                if (boundary) out += IntPoint(x, y)
            }
        }
        return out
    }

    private fun sampleBoundary(points: List<IntPoint>): List<IntPoint> {
        if (points.size <= MAX_BOUNDARY_POINTS) return points
        val step = ceil(points.size.toDouble() / MAX_BOUNDARY_POINTS).toInt().coerceAtLeast(1)
        return points.filterIndexed { index, _ -> index % step == 0 }
    }

    private fun convexHull(points: List<IntPoint>): List<IntPoint> {
        val sorted = points.distinct().sortedWith(compareBy<IntPoint> { it.x }.thenBy { it.y })
        if (sorted.size <= 1) return sorted

        val lower = ArrayList<IntPoint>()
        for (p in sorted) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], p) <= 0) {
                lower.removeAt(lower.lastIndex)
            }
            lower += p
        }

        val upper = ArrayList<IntPoint>()
        for (p in sorted.asReversed()) {
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], p) <= 0) {
                upper.removeAt(upper.lastIndex)
            }
            upper += p
        }

        lower.removeAt(lower.lastIndex)
        upper.removeAt(upper.lastIndex)
        return lower + upper
    }

    private fun quadFromExtremes(points: List<IntPoint>): Quad? {
        val topLeft = points.minByOrNull { it.x + it.y } ?: return null
        val topRight = points.maxByOrNull { it.x - it.y } ?: return null
        val bottomRight = points.maxByOrNull { it.x + it.y } ?: return null
        val bottomLeft = points.minByOrNull { it.x - it.y } ?: return null

        val quad = Quad(
            topLeft = topLeft.toPointF2(),
            topRight = topRight.toPointF2(),
            bottomRight = bottomRight.toPointF2(),
            bottomLeft = bottomLeft.toPointF2()
        )
        return orderClockwise(quad)
    }

    private fun orientedRectangle(points: List<IntPoint>): Quad {
        val cx = points.map { it.x }.average().toFloat()
        val cy = points.map { it.y }.average().toFloat()
        var xx = 0.0
        var xy = 0.0
        var yy = 0.0
        for (p in points) {
            val dx = p.x - cx
            val dy = p.y - cy
            xx += dx * dx
            xy += dx * dy
            yy += dy * dy
        }
        val theta = 0.5f * atan2((2.0 * xy).toFloat(), (xx - yy).toFloat())
        val axisX = PointF2(cos(theta), sin(theta))
        val axisY = PointF2(-sin(theta), cos(theta))

        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (p in points) {
            val dx = p.x - cx
            val dy = p.y - cy
            val px = dx * axisX.x + dy * axisX.y
            val py = dx * axisY.x + dy * axisY.y
            minX = min(minX, px)
            maxX = max(maxX, px)
            minY = min(minY, py)
            maxY = max(maxY, py)
        }

        fun point(px: Float, py: Float): PointF2 = PointF2(
            x = cx + axisX.x * px + axisY.x * py,
            y = cy + axisX.y * px + axisY.y * py
        )

        return orderClockwise(
            Quad(
                topLeft = point(minX, minY),
                topRight = point(maxX, minY),
                bottomRight = point(maxX, maxY),
                bottomLeft = point(minX, maxY)
            )
        )
    }

    private fun orderClockwise(quad: Quad): Quad {
        val points = listOf(quad.topLeft, quad.topRight, quad.bottomRight, quad.bottomLeft)
        val cx = points.map { it.x }.average().toFloat()
        val cy = points.map { it.y }.average().toFloat()
        val ordered = points.sortedBy { atan2(it.y - cy, it.x - cx) }
        val topCandidates = ordered.sortedBy { it.y }.take(2).sortedBy { it.x }
        val bottomCandidates = ordered.sortedByDescending { it.y }.take(2).sortedBy { it.x }
        return Quad(
            topLeft = topCandidates[0],
            topRight = topCandidates[1],
            bottomRight = bottomCandidates[1],
            bottomLeft = bottomCandidates[0]
        )
    }

    private fun isSane(quad: Quad, width: Int, height: Int): Boolean {
        val area = abs(polygonArea(quad))
        val minArea = width * height * 0.003f
        val top = distanceSquared(quad.topLeft, quad.topRight)
        val bottom = distanceSquared(quad.bottomLeft, quad.bottomRight)
        val left = distanceSquared(quad.topLeft, quad.bottomLeft)
        val right = distanceSquared(quad.topRight, quad.bottomRight)
        return area >= minArea && top > 25f && bottom > 25f && left > 25f && right > 25f
    }

    private fun clampQuad(quad: Quad, width: Int, height: Int): Quad {
        fun clamp(point: PointF2): PointF2 = PointF2(
            x = point.x.coerceIn(0f, (width - 1).toFloat()),
            y = point.y.coerceIn(0f, (height - 1).toFloat())
        )
        return Quad(
            topLeft = clamp(quad.topLeft),
            topRight = clamp(quad.topRight),
            bottomRight = clamp(quad.bottomRight),
            bottomLeft = clamp(quad.bottomLeft)
        )
    }

    private fun RectBox.toQuad(): Quad = Quad(
        topLeft = PointF2(left.toFloat(), top.toFloat()),
        topRight = PointF2(right.toFloat(), top.toFloat()),
        bottomRight = PointF2(right.toFloat(), bottom.toFloat()),
        bottomLeft = PointF2(left.toFloat(), bottom.toFloat())
    )

    private fun polygonArea(quad: Quad): Float {
        val points = listOf(quad.topLeft, quad.topRight, quad.bottomRight, quad.bottomLeft)
        var sum = 0f
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            sum += a.x * b.y - b.x * a.y
        }
        return sum / 2f
    }

    private fun distanceSquared(a: PointF2, b: PointF2): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    private fun cross(o: IntPoint, a: IntPoint, b: IntPoint): Long =
        (a.x - o.x).toLong() * (b.y - o.y).toLong() - (a.y - o.y).toLong() * (b.x - o.x).toLong()

    private data class IntPoint(val x: Int, val y: Int) {
        fun toPointF2(): PointF2 = PointF2(x.toFloat(), y.toFloat())
    }
}
