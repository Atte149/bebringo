package org.dueattendant149.bebringo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Radius
import org.dueattendant149.bebringo.ui.theme.Spacing

/** Тонкая полоса прогресса. */
@Composable
fun BlockProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    trackColor: Color? = null,
    fillColor: Color? = null,
) {
    val colors = BebringoTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(trackColor ?: colors.surfaceVariant, RoundedCornerShape(Radius.xs)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .background(fillColor ?: colors.primary, RoundedCornerShape(Radius.xs)),
        )
    }
}

/** Счётчик шагов вида «2 / 5». */
@Composable
fun StepCounter(current: Int, total: Int, modifier: Modifier = Modifier) {
    Text(
        text = "$current / $total",
        style = MaterialTheme.typography.labelMedium,
        color = BebringoTheme.colors.muted,
        modifier = modifier,
    )
}

/** Точки глав: заполненная, наполовину, пустая. */
@Composable
fun ChapterDots(
    done: Int,
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val colors = BebringoTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val color = when {
                index < done -> colors.primary
                index == current -> colors.primary.copy(alpha = 0.45f)
                else -> colors.outline
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

/** Полоса прогресса с подписью и процентом. */
@Composable
fun ProgressBarWithLabel(
    fraction: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = BebringoTheme.colors
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBg,
            )
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.primary,
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(Spacing.s))
        BlockProgressBar(fraction = fraction)
    }
}
