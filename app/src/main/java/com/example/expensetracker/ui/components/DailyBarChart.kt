package com.example.expensetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.expensetracker.DailySpending

@Composable
fun DailyBarChart(
    data: List<DailySpending>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxAmount = data.maxOf { it.totalAmount }.toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .padding(16.dp)
    ) {

        val chartHeight = size.height - 20f
        val spacing = 20f
        val barWidth =
            (size.width - spacing * (data.size + 1)) / data.size

        // Bottom axis
        drawLine(
            color = Color(0xFF374151),
            start = Offset(0f, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 3f
        )

        data.forEachIndexed { index, item ->

            val ratio = item.totalAmount / maxAmount

            val barHeight = ratio * (chartHeight - 20f)

            val left = spacing + index * (barWidth + spacing)

            val top = chartHeight - barHeight

            drawRoundRect(
                color = Color(0xFF3B82F6),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(18f, 18f)
            )

            // Small highlight on top
            drawRoundRect(
                color = Color(0xFF60A5FA),
                topLeft = Offset(left, top),
                size = Size(barWidth, 8f),
                cornerRadius = CornerRadius(18f, 18f)
            )
        }
    }
}