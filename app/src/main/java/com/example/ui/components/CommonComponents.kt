package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.theme.FinanceAmber
import com.example.ui.theme.FinanceAmberContainer
import com.example.ui.theme.FinanceBlue
import com.example.ui.theme.FinanceBlueContainer
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceGreenContainer
import com.example.ui.theme.FinanceRed
import com.example.ui.theme.FinanceRedContainer
import java.util.Locale

@Composable
fun MetricCard(
    title: String,
    amount: Double,
    currency: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    testTag: String = "metric_card",
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$currency${formatAmount(amount)}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (actionLabel != null && onActionClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = actionLabel,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun FinancialProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = color,
        trackColor = trackColor
    )
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    testTag: String = "empty_state_view"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (!actionButtonText.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("empty_state_action_btn")
                ) {
                    Text(actionButtonText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

fun formatAmount(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}

fun resolveCategoryIcon(category: String): ImageVector {
    val lower = category.lowercase()
    return when {
        lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") -> Icons.Default.Restaurant
        lower.contains("grocer") -> Icons.Default.ShoppingCart
        lower.contains("transport") || lower.contains("gas") || lower.contains("transit") -> Icons.Default.DirectionsCar
        lower.contains("bill") || lower.contains("utility") || lower.contains("electric") -> Icons.Default.Receipt
        lower.contains("house") || lower.contains("rent") || lower.contains("mortgage") -> Icons.Default.Home
        lower.contains("health") || lower.contains("med") || lower.contains("doctor") -> Icons.Default.MedicalServices
        lower.contains("entertain") || lower.contains("movie") || lower.contains("cinema") -> Icons.Default.Movie
        lower.contains("shop") || lower.contains("cloth") -> Icons.Default.LocalMall
        lower.contains("travel") || lower.contains("vacation") || lower.contains("flight") -> Icons.Default.Flight
        lower.contains("education") || lower.contains("school") || lower.contains("course") -> Icons.Default.School
        lower.contains("care") || lower.contains("beauty") || lower.contains("spa") -> Icons.Default.Spa
        lower.contains("salary") || lower.contains("wage") || lower.contains("income") -> Icons.Default.Payments
        lower.contains("freelance") || lower.contains("side") -> Icons.Default.AttachMoney
        lower.contains("save") || lower.contains("invest") -> Icons.Default.Savings
        else -> Icons.Default.AttachMoney
    }
}

fun resolveCategoryColor(category: String): Color {
    val lower = category.lowercase()
    return when {
        lower.contains("food") || lower.contains("dining") -> Color(0xFFE65100)
        lower.contains("grocer") -> Color(0xFF2E7D32)
        lower.contains("transport") -> Color(0xFF0277BD)
        lower.contains("bill") || lower.contains("util") -> Color(0xFFC62828)
        lower.contains("house") || lower.contains("rent") -> Color(0xFF4527A0)
        lower.contains("health") -> Color(0xFFAD1457)
        lower.contains("entertain") -> Color(0xFF6A1B9A)
        lower.contains("shop") -> Color(0xFFD81B60)
        lower.contains("salary") || lower.contains("income") -> Color(0xFF1B5E20)
        lower.contains("save") -> Color(0xFF00695C)
        else -> Color(0xFF546E7A)
    }
}
