package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.analytics.CategorySpending
import com.example.domain.analytics.DailySpending
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import java.util.Locale
import kotlin.math.atan2

@Composable
fun SpendingTrendBarChart(
    dailyData: List<DailySpending>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (dailyData.isEmpty()) return

    val maxAmount = remember(dailyData) {
        val max = dailyData.maxOfOrNull { it.amount } ?: 1.0
        if (max <= 0.0) 100.0 else max
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(dailyData) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_trend_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Spending Trend",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = onSurface
                )

                if (selectedIndex != null && selectedIndex!! < dailyData.size) {
                    val item = dailyData[selectedIndex!!]
                    Text(
                        text = "${item.dateLabel}: $currency${formatAmount(item.amount)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = primaryColor
                    )
                } else {
                    Text(
                        text = "Last 7 Days",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(dailyData) {
                            detectTapGestures { offset ->
                                val barWidth = size.width / dailyData.size
                                val index = (offset.x / barWidth).toInt().coerceIn(0, dailyData.size - 1)
                                selectedIndex = if (selectedIndex == index) null else index
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barSlotWidth = canvasWidth / dailyData.size
                    val barWidth = (barSlotWidth * 0.45f).coerceAtMost(28.dp.toPx())
                    val maxBarHeight = canvasHeight - 24.dp.toPx()

                    // Draw baseline
                    drawLine(
                        color = secondaryColor,
                        start = Offset(0f, maxBarHeight),
                        end = Offset(canvasWidth, maxBarHeight),
                        strokeWidth = 2.dp.toPx()
                    )

                    dailyData.forEachIndexed { index, item ->
                        val ratio = (item.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                        val barHeight = (ratio * maxBarHeight * animationProgress.value).coerceAtLeast(4.dp.toPx())
                        val centerX = (index * barSlotWidth) + (barSlotWidth / 2f)
                        val left = centerX - (barWidth / 2f)
                        val top = maxBarHeight - barHeight

                        val isSelected = selectedIndex == index
                        val barColor = when {
                            isSelected -> primaryColor
                            item.isToday -> primaryColor.copy(alpha = 0.85f)
                            item.amount > 0 -> primaryColor.copy(alpha = 0.35f)
                            else -> secondaryColor
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dailyData.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    ) {
                        Text(
                            text = item.dateLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (item.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.isToday || isSelected) primaryColor else onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDonutChart(
    categorySpending: List<CategorySpending>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (categorySpending.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No category expense records yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val totalExpense = remember(categorySpending) { categorySpending.sumOf { it.amount } }
    var selectedCategory by remember { mutableStateOf<CategorySpending?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categorySpending) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_donut_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Spending by Category",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Donut Chart Canvas with Center Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(170.dp)
                        .pointerInput(categorySpending) {
                            detectTapGestures { offset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                var currentStart = 0f
                                for (item in categorySpending) {
                                    val sweep = (item.percentage / 100f) * 360f
                                    if (angle >= currentStart && angle <= currentStart + sweep) {
                                        selectedCategory = if (selectedCategory?.category == item.category) null else item
                                        break
                                    }
                                    currentStart += sweep
                                }
                            }
                        }
                ) {
                    val strokeWidth = 24.dp.toPx()
                    val chartRadius = (size.minDimension - strokeWidth) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    var startAngle = -90f
                    categorySpending.forEach { item ->
                        val sweepAngle = (item.percentage / 100f) * 360f * animationProgress.value
                        val itemColor = resolveCategoryColor(item.category)
                        val isSelected = selectedCategory?.category == item.category

                        drawArc(
                            color = if (isSelected) itemColor else itemColor.copy(alpha = 0.9f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 2f, // slight gap
                            useCenter = false,
                            topLeft = Offset(center.x - chartRadius, center.y - chartRadius),
                            size = Size(chartRadius * 2, chartRadius * 2),
                            style = Stroke(
                                width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                        startAngle += sweepAngle
                    }
                }

                // Center Text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedCategory != null) {
                        Text(
                            text = selectedCategory!!.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "$currency${formatAmount(selectedCategory!!.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedCategory!!.percentage.toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = resolveCategoryColor(selectedCategory!!.category)
                        )
                    } else {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currency${formatAmount(totalExpense)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${categorySpending.size} categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories Grid / Legend
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categorySpending.forEach { item ->
                    val isSelected = selectedCategory?.category == item.category
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .clickable {
                                selectedCategory = if (selectedCategory?.category == item.category) null else item
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(resolveCategoryColor(item.category))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.category} (${item.percentage.toInt()}%)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
