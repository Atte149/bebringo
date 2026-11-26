package org.dueattendant149.bebringo.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.domain.model.Skill
import org.dueattendant149.bebringo.ui.components.BebringoCard
import org.dueattendant149.bebringo.ui.components.Mascot
import org.dueattendant149.bebringo.ui.components.MascotPose
import org.dueattendant149.bebringo.ui.components.ProgressBarWithLabel
import org.dueattendant149.bebringo.ui.components.SectionHeader
import org.dueattendant149.bebringo.ui.theme.BebringoTheme
import org.dueattendant149.bebringo.ui.theme.Radius
import org.dueattendant149.bebringo.ui.theme.Spacing

/**
 * Прогресс (Material Design 3 / R5):
 * пробег по тексту, лестница практических умений и коллекция собственных фраз.
 */
@Composable
fun ProgressScreen(
    coverage: Float,
    readingTitle: String,
    skills: List<Skill>,
    collectionCount: Int,
    modifier: Modifier = Modifier,
    collectionPreview: List<Pair<String, String>> = emptyList(),
) {
    val colors = BebringoTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Text(
            text = "Прогресс",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBg,
        )

        BebringoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionHeader("Пробег")
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = readingTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                Mascot(pose = MascotPose.STAR, size = 76.dp)
            }
            Spacer(Modifier.height(Spacing.m))
            ProgressBarWithLabel(fraction = coverage, label = "Освоение базового словаря A1")
        }

        BebringoCard {
            SectionHeader("Умения")
            Spacer(Modifier.height(Spacing.m))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                skills.forEach { skill ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    if (skill.achieved) colors.primary else colors.surfaceContainerHigh,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (skill.achieved) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onPrimary,
                                )
                            }
                        }
                        Text(
                            text = skill.titleRu,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (skill.achieved) colors.onBg else colors.muted,
                        )
                    }
                }
            }
        }

        BebringoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader("Коллекция фраз")
                Box(
                    modifier = Modifier
                        .background(colors.primaryContainer, RoundedCornerShape(Radius.pill))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "$collectionCount",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Фразы и примеры из пройденных вами блоков",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )

            if (collectionPreview.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.m))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    collectionPreview.forEachIndexed { idx, (ro, ru) ->
                        if (idx > 0) {
                            HorizontalDivider(color = colors.outlineVariant, thickness = 1.dp)
                        }
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = ro,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = colors.onBg,
                            )
                            if (ru.isNotBlank()) {
                                Text(
                                    text = ru,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.muted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
