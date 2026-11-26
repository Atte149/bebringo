package org.dueattendant149.bebringo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dueattendant149.bebringo.R

/**
 * Позы и эмоции маскота-бобра Bebringo.
 * Могут задаваться как из кода UI, так и из YAML-манифеста трека (`mascot_pose`).
 */
enum class MascotPose(
    val id: String,
    @DrawableRes val drawableRes: Int,
    val contentDescription: String,
) {
    WAVE("wave", R.drawable.mascot_wave, "Бобёр приветливо машет лапкой"),
    READING("reading", R.drawable.mascot_reading, "Бобёр читает зелёную книгу"),
    THUMBS_UP("thumbs_up", R.drawable.mascot_thumbs_up, "Бобёр показывает класс"),
    MEDITATE("meditate", R.drawable.mascot_meditate, "Бобёр спокойно медитирует"),
    SHRUG("shrug", R.drawable.mascot_shrug, "Бобёр дружелюбно разводит лапками"),
    FLAG_RO("flag_ro", R.drawable.mascot_flag_ro, "Бобёр с флагом Румынии"),
    INSIGHT("insight", R.drawable.mascot_insight, "Бобёр объясняет важное правило"),
    PENCIL("pencil", R.drawable.mascot_pencil, "Бобёр с большим карандашом"),
    STAR("star", R.drawable.mascot_star, "Бобёр держит золотую звезду"),
    CELEBRATE("celebrate", R.drawable.mascot_celebrate, "Бобёр радуется успеху"),
    THINKING("thinking", R.drawable.mascot_thinking, "Бобёр задумчиво чешет затылок"),
    LANTERN("lantern", R.drawable.mascot_lantern, "Бобёр освещает путь фонариком"),
    SCHOLAR("scholar", R.drawable.mascot_scholar, "Бобёр в очках изучает текст"),
    SLEEPING("sleeping", R.drawable.mascot_sleeping, "Бобёр мирно спит на подушке"),
    AVATAR("avatar", R.drawable.mascot_avatar, "Портрет бобра Bebringo");

    companion object {
        fun fromId(id: String?): MascotPose {
            if (id.isNullOrBlank()) return WAVE
            val normalized = id.trim().lowercase().removePrefix("mascot_")
            return entries.firstOrNull { it.id == normalized } ?: WAVE
        }
    }
}

/**
 * Основной компонент иллюстрации маскота-бобра.
 */
@Composable
fun Mascot(
    pose: MascotPose = MascotPose.WAVE,
    size: Dp = 84.dp,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (containerColor != Color.Transparent) {
                    Modifier
                        .clip(RoundedCornerShape(size * 0.28f))
                        .background(containerColor)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = pose.drawableRes),
            contentDescription = pose.contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(if (containerColor != Color.Transparent) size * 0.88f else size),
        )
    }
}

/**
 * Совместимый обёрточный вызов для мест, где ранее использовался `MascotPlaceholder`.
 */
@Composable
fun MascotPlaceholder(
    size: Dp = 96.dp,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    pose: MascotPose = MascotPose.WAVE,
) {
    Mascot(
        pose = pose,
        size = size,
        modifier = modifier,
        containerColor = background,
    )
}
