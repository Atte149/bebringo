package org.dueattendant149.bebringo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Radius
import org.dueattendant149.bebringo.ui.theme.Spacing

/** Вариант выбора M3: тональный контейнер `primaryContainer` при выборе, 52dp+ высота касания. */
@Composable
fun ChoiceRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    enabled: Boolean = true,
) {
    val colors = BebringoTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(
                if (selected) colors.primaryContainer else colors.surfaceContainerLow,
                shape,
            )
            .border(
                BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) colors.primary else colors.outlineVariant,
                ),
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        if (emoji != null) {
            Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) colors.onPrimaryContainer else colors.onBg,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Выбрано",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/** Строка-ссылка с эмодзи, заголовком и шевроном. */
@Composable
fun NavRow(
    title: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = BebringoTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(colors.surfaceContainerLow, shape)
            .border(BorderStroke(1.dp, colors.outlineVariant), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.surfaceContainerHigh, RoundedCornerShape(Radius.sm)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = colors.onBg)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            }
        }
        Text(text = "›", style = MaterialTheme.typography.headlineSmall, color = colors.muted)
    }
}

/**
 * M3 сгруппированный контейнер списка (`surfaceContainerLow` + `HorizontalDivider`),
 * избавляющий от визуального шума «карточка на каждую строку».
 */
@Composable
fun GroupedListCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = BebringoTheme.colors
    val shape = RoundedCornerShape(Radius.lg)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerLow, shape)
            .border(BorderStroke(1.dp, colors.outlineVariant), shape),
        content = content,
    )
}

/** Строка внутри `GroupedListCard`. */
@Composable
fun GroupedListRow(
    title: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    showDivider: Boolean = true,
) {
    val colors = BebringoTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 60.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surfaceContainerHigh, RoundedCornerShape(Radius.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = colors.onBg)
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                }
            }
            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .background(colors.primaryContainer, RoundedCornerShape(Radius.pill))
                        .padding(horizontal = Spacing.m, vertical = 4.dp),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPrimaryContainer,
                    )
                }
            }
            Text(text = "›", style = MaterialTheme.typography.headlineSmall, color = colors.muted)
        }
        if (showDivider) {
            HorizontalDivider(
                color = colors.outlineVariant,
                thickness = 1.dp,
                modifier = Modifier.padding(start = 68.dp),
            )
        }
    }
}

/** Заголовок секции. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = BebringoTheme.colors.onBg,
        modifier = modifier,
    )
}

/** Карточка M3: тональная поверхность с мягким контуром и корректным clip для ripple. */
@Composable
fun BebringoCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = BebringoTheme.colors
    val shape = RoundedCornerShape(Radius.lg)
    val resolvedContainer = containerColor ?: colors.surfaceContainerLow
    val resolvedBorder = borderColor ?: colors.outlineVariant
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(resolvedContainer, shape)
        .border(BorderStroke(1.dp, resolvedBorder), shape)
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
    ) {
        Column(modifier = Modifier.padding(Spacing.l), content = content)
    }
}

/** Плитка слова для сборки фразы (поддерживает и пул слов, и снятие выбранного слова). */
@Composable
fun WordTile(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    used: Boolean = false,
    pickedStyle: Boolean = false,
) {
    val colors = BebringoTheme.colors
    val shape = RoundedCornerShape(Radius.sm)
    val bgColor = when {
        pickedStyle -> colors.primaryContainer
        used -> colors.surfaceContainer.copy(alpha = 0.45f)
        else -> colors.surfaceContainerHigh
    }
    val textColor = when {
        pickedStyle -> colors.onPrimaryContainer
        used -> colors.muted.copy(alpha = 0.45f)
        else -> colors.onBg
    }
    val borderStroke = when {
        pickedStyle -> BorderStroke(1.dp, colors.primary)
        used -> BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f))
        else -> BorderStroke(1.dp, colors.outline)
    }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(shape)
            .background(bgColor, shape)
            .border(borderStroke, shape)
            .clickable(enabled = !used, onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** Сообщение об ошибке в уроке и разборе блока (R4). */
@Composable
fun FeedbackCard(
    prompt: String,
    correction: String,
    explanation: String,
    modifier: Modifier = Modifier,
) {
    val colors = BebringoTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.errorSurface, shape)
            .border(BorderStroke(1.dp, colors.error.copy(alpha = 0.35f)), shape)
            .padding(Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(colors.error, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "!", style = MaterialTheme.typography.labelLarge, color = colors.onError)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = prompt, style = MaterialTheme.typography.bodyMedium, color = colors.muted)
            Text(
                text = correction,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.error,
            )
            Text(text = explanation, style = MaterialTheme.typography.bodyMedium, color = colors.onBg)
        }
    }
}

/** Плашка успешного ответа или мягкого напоминания о диакритике в уроке. */
@Composable
fun SuccessFeedbackCard(
    title: String,
    subtitle: String? = null,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = BebringoTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    val bg = if (isWarning) colors.warningContainer else colors.successContainer
    val accent = if (isWarning) colors.warning else colors.success
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg, shape)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.4f)), shape)
            .padding(Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isWarning) "~" else "✓",
                style = MaterialTheme.typography.labelLarge,
                color = colors.surfaceContainerLowest,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onBg,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onBg)
            }
        }
    }
}
