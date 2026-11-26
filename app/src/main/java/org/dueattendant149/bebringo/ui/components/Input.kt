package org.dueattendant149.bebringo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Radius
import org.dueattendant149.bebringo.ui.theme.Spacing

/** Буквы с диакритикой, которые чаще всего нужны при вводе румынского. */
val RomanianDiacritics = listOf('ă', 'â', 'î', 'ș', 'ț')

/**
 * Полоска диакритики над клавиатурой.
 *
 * Нужна потому, что системная клавиатура не всегда даёт эти буквы в один тап,
 * а печатать придётся много.
 */
@Composable
fun DiacriticsBar(
    onInsert: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BebringoTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        RomanianDiacritics.forEach { char ->
            Text(
                text = char.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBg,
                modifier = Modifier
                    .weight(1f)
                    .background(colors.surfaceVariant, RoundedCornerShape(Radius.sm))
                    .clickable { onInsert(char) }
                    .padding(vertical = Spacing.m),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** Поле ввода в стиле приложения. */
@Composable
fun BebringoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Введите ответ…",
    imeAction: ImeAction = ImeAction.Done,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    val colors = BebringoTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        placeholder = {
            Text(text = placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.muted)
        },
        textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = colors.onBg)),
        shape = RoundedCornerShape(Radius.md),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline,
            errorBorderColor = colors.error,
            cursorColor = colors.primary,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedTextColor = colors.onBg,
            unfocusedTextColor = colors.onBg,
        ),
        singleLine = true,
    )
}

/** Кнопка воспроизведения аудио. */
@Composable
fun AudioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Прослушать",
    isPlaying: Boolean = false,
) {
    val colors = BebringoTheme.colors
    val bg = if (isPlaying) colors.primaryContainer else colors.surfaceVariant
    val iconColor = if (isPlaying) colors.onPrimaryContainer else colors.primary
    val icon = if (isPlaying) "🔊" else "▶"
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(Radius.pill))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(text = icon, style = MaterialTheme.typography.bodyLarge, color = iconColor)
        Text(
            text = if (isPlaying) "Звучит..." else label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onBg,
        )
    }
}

/** Разделитель с отступом. */
@Composable
fun BebringoDivider(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BebringoTheme.colors.outline)
            .padding(top = 1.dp, bottom = 0.dp),
    )
}

/** Подпись поля. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = BebringoTheme.colors.muted,
        modifier = modifier,
    )
}

/** Маркер выбранного чипа. */
@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BebringoTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) colors.onPrimary else colors.onBg,
        modifier = modifier
            .background(
                if (selected) colors.primary else colors.surface,
                RoundedCornerShape(Radius.pill),
            )
            .then(
                if (selected) Modifier else Modifier.background(
                    colors.surface,
                    RoundedCornerShape(Radius.pill),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
    )
}

/** Обводка карточки, используемая в чипах без заливки. */
@Composable
fun ChipBorder() = BorderStroke(1.dp, BebringoTheme.colors.outline)
