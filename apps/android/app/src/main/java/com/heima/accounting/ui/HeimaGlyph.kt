package com.heima.accounting.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun HeimaGlyph(
    destination: AppDestination,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.085f
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val w = size.width
        val h = size.height

        when (destination) {
            AppDestination.HOME -> {
                val roof = Path().apply {
                    moveTo(w * 0.16f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.18f)
                    lineTo(w * 0.84f, h * 0.48f)
                }
                drawPath(roof, color, style = stroke)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.25f, h * 0.45f),
                    size = androidx.compose.ui.geometry.Size(w * 0.50f, h * 0.39f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = stroke,
                )
            }

            AppDestination.STATISTICS -> {
                drawLine(color, Offset(w * 0.20f, h * 0.78f), Offset(w * 0.20f, h * 0.55f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.78f), Offset(w * 0.50f, h * 0.30f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.80f, h * 0.78f), Offset(w * 0.80f, h * 0.18f), strokeWidth, StrokeCap.Round)
            }

            AppDestination.RECORD -> {
                drawLine(color, Offset(w * 0.28f, h * 0.72f), Offset(w * 0.70f, h * 0.30f), strokeWidth * 1.05f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.23f, h * 0.78f), Offset(w * 0.37f, h * 0.74f), strokeWidth * 0.85f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.62f, h * 0.25f), Offset(w * 0.75f, h * 0.38f), strokeWidth * 0.7f, StrokeCap.Round)
            }

            AppDestination.BUDGET -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.14f, h * 0.27f),
                    size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.14f, h * 0.40f), Offset(w * 0.72f, h * 0.40f), strokeWidth, StrokeCap.Round)
                drawCircle(color, radius = strokeWidth * 0.55f, center = Offset(w * 0.69f, h * 0.59f))
            }

            AppDestination.PROFILE -> {
                drawCircle(
                    color = color,
                    radius = w * 0.17f,
                    center = Offset(w * 0.50f, h * 0.34f),
                    style = stroke,
                )
                drawArc(
                    color = color,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.20f, h * 0.46f),
                    size = androidx.compose.ui.geometry.Size(w * 0.60f, h * 0.42f),
                    style = stroke,
                )
            }
        }
    }
}
