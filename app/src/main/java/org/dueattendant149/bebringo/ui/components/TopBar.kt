package org.dueattendant149.bebringo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Spacing

/**
 * Шапка экрана: назад, заголовок или полоса прогресса, счётчик шагов.
 * Используется в блоке, чтении и чате.
 */
@Composable
fun BebringoTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    stepCurrent: Int? = null,
    stepTotal: Int? = null,
    action: @Composable (() -> Unit)? = null,
) {
    val colors = BebringoTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .statusBarsPadding()
            .padding(horizontal = Spacing.s, vertical = Spacing.s),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Назад",
                    tint = colors.onBg,
                )
            }

            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBg,
                    modifier = Modifier.weight(1f),
                )
            } else if (stepCurrent != null && stepTotal != null) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    BlockProgressBar(
                        fraction = stepCurrent.toFloat() / stepTotal.coerceAtLeast(1),
                        modifier = Modifier.weight(1f),
                    )
                    StepCounter(current = stepCurrent, total = stepTotal)
                }
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }

            action?.invoke()
        }
    }
}

/**
 * Обычная шапка с заголовком и действием справа.
 */
@Composable
fun PlainTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
) {
    BebringoTopBar(
        onBack = onBack ?: {},
        title = title,
        modifier = modifier,
        action = action,
    )
}
