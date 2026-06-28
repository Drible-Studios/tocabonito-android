package studios.drible.tocabonito.feature.detail.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studios.drible.tocabonito.core.domain.model.StreamOption
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette

private val Quality4K = Color(0xFF7C3AED)
private val Quality1080p = Color(0xFF2563EB)
private val Quality720p = Color(0xFF16A34A)
private val QualityDefault = Color(0xFF6B7280)

private val BadgePurple = Color(0xFF7C3AED).copy(alpha = 0.15f)
private val BadgeAmber = Color(0xFFF59E0B).copy(alpha = 0.15f)
private val BadgeCyan = Color(0xFF06B6D4).copy(alpha = 0.15f)
private val BadgeGreen = Color(0xFF16A34A).copy(alpha = 0.15f)
private val BadgeGray = Color(0xFF6B7280).copy(alpha = 0.15f)

private val TextPurple = Color(0xFF7C3AED)
private val TextAmber = Color(0xFFF59E0B)
private val TextCyan = Color(0xFF06B6D4)
private val TextGreen = Color(0xFF16A34A)
private val TextGray = Color(0xFF9CA3AF)

private fun qualityColor(quality: String): Color = when {
    quality.contains("4K", ignoreCase = true) || quality.contains("2160", ignoreCase = true) -> Quality4K
    quality.contains("1080", ignoreCase = true) -> Quality1080p
    quality.contains("720", ignoreCase = true) -> Quality720p
    else -> QualityDefault
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamRow(
    stream: StreamOption,
    onPlay: () -> Unit,
    onDownload: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalThemePalette.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Title + action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stream.title,
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = palette.accent,
                    )
                }

                if (onDownload != null) {
                    IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = palette.textSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Row 2: Badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (stream.quality.isNotBlank()) {
                    Badge(
                        text = stream.quality.uppercase(),
                        bgColor = qualityColor(stream.quality),
                        textColor = Color.White,
                    )
                }

                val source = stream.metadata.source
                if (!source.isNullOrBlank()) {
                    Badge(text = source, bgColor = BadgePurple, textColor = TextPurple)
                }

                val hdr = stream.metadata.hdr
                if (!hdr.isNullOrBlank()) {
                    Badge(text = hdr, bgColor = BadgeAmber, textColor = TextAmber)
                }

                val codec = stream.metadata.codec
                if (!codec.isNullOrBlank()) {
                    Badge(text = codec, bgColor = BadgeCyan, textColor = TextCyan)
                }

                if (stream.seeders > 0) {
                    Badge(
                        text = "${stream.seeders} seeds",
                        bgColor = BadgeGreen,
                        textColor = TextGreen,
                    )
                }

                if (stream.size.isNotBlank()) {
                    Badge(text = stream.size, bgColor = BadgeGray, textColor = TextGray)
                }
            }

            // Row 3: Audio / Subtitle indicators
            val langLabel = stream.metadata.languageLabel
            val subLabel = stream.metadata.subtitleLabel
            if (langLabel.isNotBlank() || subLabel.isNotBlank()) {
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (langLabel.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "Audio",
                            tint = palette.textTertiary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = langLabel,
                            color = palette.textTertiary,
                            fontSize = 11.sp,
                        )
                    }

                    if (langLabel.isNotBlank() && subLabel.isNotBlank()) {
                        Spacer(Modifier.width(12.dp))
                    }

                    if (subLabel.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Subtitles",
                            tint = palette.textTertiary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = subLabel,
                            color = palette.textTertiary,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(
    text: String,
    bgColor: Color,
    textColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
