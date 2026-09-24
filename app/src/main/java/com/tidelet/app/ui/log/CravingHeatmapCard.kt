package com.tidelet.app.ui.log

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tidelet.app.ui.theme.ElectricCobalt
import com.tidelet.app.ui.theme.SteelGray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoField

private val CellSize = 12.dp
private val CellGap = 2.dp
private val LabelWidth = 16.dp
private const val WEEKS = 13
private const val DAYS_IN_WEEK = 7

@Composable
fun CravingHeatmapCard(
    data: Map<LocalDate, DayCravingSummary>,
    selectedDay: LocalDate?,
    onSelectDay: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val grid = remember(today, data) { buildGrid(today, data) }
    val textMeasurer = rememberTextMeasurer()

    val density = LocalDensity.current
    val cellSizePx = with(density) { CellSize.toPx() }
    val cellGapPx = with(density) { CellGap.toPx() }
    val labelWidthPx = with(density) { LabelWidth.toPx() }
    val headerHeightPx = with(density) { 14.dp.toPx() }
    val totalStep = cellSizePx + cellGapPx

    val canvasWidth = labelWidthPx + WEEKS * totalStep
    val canvasHeight = headerHeightPx + DAYS_IN_WEEK * totalStep

    val labelStyle = TextStyle(
        fontSize = 9.sp,
        fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedBorder = MaterialTheme.colorScheme.secondary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CRAVINGS ANALYSIS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { canvasHeight.toDp() })
                    .pointerInput(grid) {
                        detectTapGestures { offset ->
                            val col = ((offset.x - labelWidthPx) / totalStep).toInt()
                            val row = ((offset.y - headerHeightPx) / totalStep).toInt()
                            if (col in 0 until WEEKS && row in 0 until DAYS_IN_WEEK) {
                                val cell = grid.getOrNull(col)?.getOrNull(row)
                                if (cell != null && cell.date != selectedDay) {
                                    onSelectDay(cell.date)
                                } else {
                                    onSelectDay(null)
                                }
                            } else {
                                onSelectDay(null)
                            }
                        }
                    },
            ) {
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                dayLabels.forEachIndexed { row, label ->
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(0f, headerHeightPx + row * totalStep + (cellSizePx - 9.sp.toPx()) / 2),
                        style = labelStyle,
                    )
                }

                grid.forEachIndexed { col, column ->
                    column.forEachIndexed { row, cell ->
                        val x = labelWidthPx + col * totalStep
                        val y = headerHeightPx + row * totalStep
                        val color = cellColor(cell.count, cell.meanIntensity, emptyColor)
                        drawRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(cellSizePx, cellSizePx),
                        )
                        if (cell.date == selectedDay) {
                            drawRect(
                                color = selectedBorder,
                                topLeft = Offset(x - 1f, y - 1f),
                                size = Size(cellSizePx + 2f, cellSizePx + 2f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class GridCell(
    val date: LocalDate,
    val count: Int,
    val meanIntensity: Double?,
)

private fun buildGrid(
    today: LocalDate,
    data: Map<LocalDate, DayCravingSummary>,
): List<List<GridCell>> {
    val endOfWeek = today.with(ChronoField.DAY_OF_WEEK, DayOfWeek.SUNDAY.value.toLong())
    val startDate = endOfWeek.minusWeeks(WEEKS.toLong() - 1).with(ChronoField.DAY_OF_WEEK, DayOfWeek.MONDAY.value.toLong())

    return (0 until WEEKS).map { week ->
        (0 until DAYS_IN_WEEK).map { dayIndex ->
            val date = startDate.plusWeeks(week.toLong()).plusDays(dayIndex.toLong())
            val summary = data[date]
            GridCell(
                date = date,
                count = summary?.count ?: 0,
                meanIntensity = summary?.meanIntensity,
            )
        }
    }
}

private fun cellColor(count: Int, meanIntensity: Double?, empty: Color): Color {
    if (count == 0) return empty
    val alpha = (count / 3f).coerceIn(0f, 1f) * 0.85f + 0.15f
    return ElectricCobalt.copy(alpha = alpha)
}
