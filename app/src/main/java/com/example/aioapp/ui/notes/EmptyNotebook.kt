package com.example.aioapp.ui.notes

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.aioapp.R

private val Paper = Color(0xFFF7F3E8)
private val PaperEdge = Color(0xFFD9D2C0)
private val Ink = Color(0xFF2E2E2E)
private val Ribbon = Color(0xFFE53950)
private val RibbonShade = Color(0xFFB0233A)
private val MetalLight = Color(0xFFE8ECEF)
private val MetalDark = Color(0xFF7D868D)

/**
 * Empty-state illustration: a spiral notebook, drawn with
 * a stack of pages, shading and a soft shadow so it reads as a slightly 3D object.
 */
@Composable
fun EmptyNotebook(gradientColors: List<Color>, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.notes_none_yet)
    val coverColors = if (gradientColors.size >= 2) gradientColors else listOf(Color(0xFF26C6DA), Color(0xFF1E88E5))

    Canvas(
        modifier = modifier
            .size(width = 170.dp, height = 214.dp)
            .semantics { contentDescription = description }
    ) {
        val pageStep = 4.dp.toPx()
        val pageCount = 3
        val depth = pageStep * pageCount
        val shadowPad = 12.dp.toPx()
        val ringRadius = 8.dp.toPx()
        val corner = CornerRadius(14.dp.toPx())

        val cover = Rect(
            left = ringRadius * 1.7f,
            top = 2.dp.toPx(),
            right = size.width - depth,
            bottom = size.height - depth - shadowPad
        )

        drawSoftShadow(cover.translate(depth * 0.6f, depth + 6.dp.toPx()), corner)
        drawPageStack(cover, pageCount, pageStep, corner)
        drawRibbon(cover, depth)
        drawCover(cover, corner, coverColors, spineWidth = ringRadius * 1.5f)
        drawRings(cover, ringRadius)

        val faceLeft = cover.left + ringRadius * 1.5f
        val face = Rect(faceLeft, cover.top, cover.right, cover.bottom)
        drawLabel(face)
    }
}

/** Soft drop shadow: a real blur where hardware rendering supports it (API 28+), stacked layers otherwise. */
private fun DrawScope.drawSoftShadow(rect: Rect, corner: CornerRadius) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val blurRadius = 12.dp.toPx()
        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(70, 0, 0, 0)
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, corner.x, corner.y, paint)
        }
        return
    }
    val layers = 5
    val spread = 3.dp.toPx()
    for (i in layers downTo 1) {
        val grow = spread * i
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.035f),
            topLeft = Offset(rect.left - grow, rect.top - grow),
            size = Size(rect.width + grow * 2, rect.height + grow * 2),
            cornerRadius = CornerRadius(corner.x + grow, corner.y + grow)
        )
    }
}

/** Pages peeking out behind the cover on the right and bottom: the notebook's thickness. */
private fun DrawScope.drawPageStack(cover: Rect, count: Int, step: Float, corner: CornerRadius) {
    for (i in count downTo 1) {
        val page = cover.translate(step * i, step * i)
        drawRoundRect(color = Paper, topLeft = page.topLeft, size = page.size, cornerRadius = corner)
        drawRoundRect(
            color = PaperEdge,
            topLeft = page.topLeft,
            size = page.size,
            cornerRadius = corner,
            style = Stroke(1.dp.toPx())
        )
    }
}

/** Bookmark ribbon that hangs out from between the pages. */
private fun DrawScope.drawRibbon(cover: Rect, depth: Float) {
    val width = 12.dp.toPx()
    val left = cover.right - width - 22.dp.toPx()
    val top = cover.bottom - 24.dp.toPx()
    val bottom = cover.bottom + depth + 16.dp.toPx()
    val notch = 6.dp.toPx()
    val ribbon = Path().apply {
        moveTo(left, top)
        lineTo(left + width, top)
        lineTo(left + width, bottom)
        lineTo(left + width / 2, bottom - notch)
        lineTo(left, bottom)
        close()
    }
    drawPath(ribbon, Brush.horizontalGradient(listOf(Ribbon, RibbonShade), startX = left, endX = left + width))
}

private fun DrawScope.drawCover(cover: Rect, corner: CornerRadius, colors: List<Color>, spineWidth: Float) {
    val coverPath = Path().apply { addRoundRect(RoundRect(cover, corner)) }

    drawPath(coverPath, Brush.linearGradient(colors, start = cover.topLeft, end = cover.bottomRight))

    clipPath(coverPath) {
        // Light from the top-left, shade towards the bottom-right
        drawRect(
            brush = Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.35f),
                0.45f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.22f),
                start = cover.topLeft,
                end = cover.bottomRight
            ),
            topLeft = cover.topLeft,
            size = cover.size
        )
        // Darker binding along the spine, with a thin highlight where it meets the cover
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Black.copy(alpha = 0.32f), Color.Black.copy(alpha = 0.12f)),
                startX = cover.left,
                endX = cover.left + spineWidth
            ),
            topLeft = cover.topLeft,
            size = Size(spineWidth, cover.height)
        )
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(cover.left + spineWidth, cover.top),
            end = Offset(cover.left + spineWidth, cover.bottom),
            strokeWidth = 1.5.dp.toPx()
        )
    }

    // Edge highlight on the top and left rims
    drawPath(coverPath, Color.White.copy(alpha = 0.18f), style = Stroke(1.5.dp.toPx()))
}

/** Metal rings looping from behind the cover into punched holes. */
private fun DrawScope.drawRings(cover: Rect, ringRadius: Float) {
    val rings = 7
    val spacing = cover.height / (rings + 1)
    val stroke = 3.dp.toPx()
    for (i in 1..rings) {
        val cy = cover.top + spacing * i
        val hole = Offset(cover.left + ringRadius * 0.75f, cy)
        drawCircle(color = Color.Black.copy(alpha = 0.55f), radius = 2.8.dp.toPx(), center = hole)

        val ovalSize = Size(ringRadius * 2.2f, ringRadius * 1.25f)
        val ovalTopLeft = Offset(hole.x - ovalSize.width, cy - ovalSize.height / 2)
        // Drop shadow of the ring on the cover
        drawArc(
            color = Color.Black.copy(alpha = 0.25f),
            startAngle = 70f, sweepAngle = 230f, useCenter = false,
            topLeft = ovalTopLeft + Offset(1.dp.toPx(), 1.5.dp.toPx()), size = ovalSize,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.verticalGradient(
                listOf(MetalLight, MetalDark),
                startY = ovalTopLeft.y,
                endY = ovalTopLeft.y + ovalSize.height
            ),
            startAngle = 70f, sweepAngle = 230f, useCenter = false,
            topLeft = ovalTopLeft, size = ovalSize,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
    }
}

/** Paper label on the cover with two "title" lines. */
private fun DrawScope.drawLabel(face: Rect) {
    val width = face.width * 0.58f
    val height = face.height * 0.17f
    val topLeft = Offset(face.center.x - width / 2, face.top + face.height * 0.12f)
    val labelCorner = CornerRadius(6.dp.toPx())
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.15f),
        topLeft = topLeft + Offset(0f, 2.dp.toPx()),
        size = Size(width, height),
        cornerRadius = labelCorner
    )
    drawRoundRect(color = Paper, topLeft = topLeft, size = Size(width, height), cornerRadius = labelCorner)
    val lineInset = width * 0.14f
    val lineStroke = 2.dp.toPx()
    listOf(0.38f, 0.66f).forEachIndexed { index, fraction ->
        val y = topLeft.y + height * fraction
        val end = topLeft.x + width - lineInset - if (index == 1) width * 0.2f else 0f
        drawLine(
            color = Ink.copy(alpha = 0.3f),
            start = Offset(topLeft.x + lineInset, y),
            end = Offset(end, y),
            strokeWidth = lineStroke,
            cap = StrokeCap.Round
        )
    }
}
