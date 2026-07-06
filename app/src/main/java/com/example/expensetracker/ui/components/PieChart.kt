package com.example.expensetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.expensetracker.CategorySpending

@Composable
fun PieChart(
    data: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.totalAmount }.toFloat()

    if (total <= 0f) return

    val colors = listOf(
        Color(0xFF2563EB), // Blue
        Color(0xFF3B82F6), // Light Blue
        Color(0xFF60A5FA), // Sky Blue
        Color(0xFF06B6D4), // Cyan
        Color(0xFF8B5CF6), // Purple
        Color(0xFF6366F1), // Indigo
        Color(0xFF0EA5E9), // Azure
        Color(0xFF38BDF8)  // Light Cyan
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(16.dp)
    ) {

        val diameter = minOf(size.width, size.height)
        val strokeWidth = diameter * 0.22f

        val arcSize = Size(
            diameter - strokeWidth,
            diameter - strokeWidth
        )

        val topLeft = androidx.compose.ui.geometry.Offset(
            (size.width - arcSize.width) / 2f,
            (size.height - arcSize.height) / 2f
        )

        var startAngle = -90f

        data.forEachIndexed { index, item ->

            val sweepAngle = (item.totalAmount / total) * 360f

            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            startAngle += sweepAngle
        }
    }
}