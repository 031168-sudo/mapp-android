package com.minews1.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minews1.HistoryDb
import java.util.Locale

@Composable
fun HistoryChart(readings: List<HistoryDb.Reading>, from: Long, to: Long, modifier: Modifier = Modifier) {
    Column(modifier) {
        ChartPanel(
            title = "Температура",
            readings = readings,
            from = from,
            to = to,
            valueOf = { it.temp },
            lineColor = MaterialTheme.colorScheme.primary,
            axisFormat = { String.format(Locale.getDefault(), "%.1f", it) },
            modifier = Modifier.weight(1f).fillMaxSize().padding(bottom = 6.dp),
        )
        ChartPanel(
            title = "Влажность",
            readings = readings,
            from = from,
            to = to,
            valueOf = { it.hum },
            lineColor = MaterialTheme.colorScheme.tertiary,
            axisFormat = { String.format(Locale.getDefault(), "%.0f", it) },
            modifier = Modifier.weight(1f).fillMaxSize(),
        )
    }
}

@Composable
private fun ChartPanel(
    title: String,
    readings: List<HistoryDb.Reading>,
    from: Long,
    to: Long,
    valueOf: (HistoryDb.Reading) -> Float,
    lineColor: Color,
    axisFormat: (Float) -> String,
    modifier: Modifier = Modifier,
) {
    // A temperature-only sensor stores no humidity, so each panel plots only the rows it has a value for.
    val points = readings.filter { !valueOf(it).isNaN() }
    ElevatedCard(modifier = modifier) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = lineColor)
            Box(Modifier.weight(1f).fillMaxSize()) {
                if (points.isEmpty()) {
                    Text(
                        "Нет данных за выбранный период",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    val gridColor = MaterialTheme.colorScheme.outlineVariant
                    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val fillColor = lineColor.copy(alpha = 0.15f)
                    Canvas(Modifier.fillMaxSize()) {
                        val values = points.map(valueOf)
                        var min = values.min()
                        var max = values.max()
                        val pad = (max - min).let { if (it <= 0f) 1f else it * 0.15f }
                        min -= pad
                        max += pad

                        val l = 4.dp.toPx()
                        val r = size.width - 44.dp.toPx()
                        val t = 4.dp.toPx()
                        val b = size.height - 4.dp.toPx()
                        val span = (to - from).coerceAtLeast(1L).toFloat()

                        val labelPaint = Paint().apply {
                            color = labelColor.toArgb()
                            textSize = 11.sp.toPx()
                            isAntiAlias = true
                        }

                        for (i in 0..4) {
                            val y = t + (b - t) * i / 4f
                            drawLine(gridColor, Offset(l, y), Offset(r, y), strokeWidth = 1f)
                            val v = max - (max - min) * i / 4f
                            drawContext.canvas.nativeCanvas.drawText(axisFormat(v), r + 6.dp.toPx(), y + 4.dp.toPx(), labelPaint)
                        }

                        fun xOf(ts: Long) = l + (r - l) * (((ts - from).toFloat() / span).coerceIn(0f, 1f))
                        fun yOf(v: Float) = b - (v - min) / (max - min) * (b - t)

                        val path = Path()
                        val fillPath = Path()
                        points.forEachIndexed { index, reading ->
                            val x = xOf(reading.ts)
                            val y = yOf(valueOf(reading))
                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }
                        fillPath.lineTo(xOf(points.last().ts), b)
                        fillPath.lineTo(xOf(points.first().ts), b)
                        fillPath.close()
                        drawPath(fillPath, color = fillColor, style = Fill)
                        drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx()))

                        val last = points.last()
                        val lx = xOf(last.ts)
                        val ly = yOf(valueOf(last))
                        drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(lx, ly))
                        drawCircle(lineColor, radius = 6.dp.toPx(), center = Offset(lx, ly), style = Stroke(width = 2.5.dp.toPx()))
                    }
                }
            }
        }
    }
}
