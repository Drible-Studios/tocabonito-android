package studios.drible.tocabonito.feature.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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

private fun qualityColor(quality: String): Color = when {
    quality.contains("4K", ignoreCase = true) || quality.contains("2160", ignoreCase = true) -> Quality4K
    quality.contains("1080", ignoreCase = true) -> Quality1080p
    quality.contains("720", ignoreCase = true) -> Quality720p
    else -> QualityDefault
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamSelectionSheet(
    streams: List<StreamOption>,
    onStreamSelected: (StreamOption) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val palette = LocalThemePalette.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardBackground,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Select Stream",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(streams, key = { it.id }) { stream ->
                    StreamCard(
                        stream = stream,
                        onClick = { onStreamSelected(stream) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun StreamCard(
    stream: StreamOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalThemePalette.current

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                QualityBadge(quality = stream.quality)

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stream.title,
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (stream.size.isNotBlank()) {
                    TagChip(text = stream.size)
                    Spacer(Modifier.width(6.dp))
                }

                if (stream.seeders > 0) {
                    TagChip(text = "${stream.seeders} seeds", color = palette.textSecondary)
                    Spacer(Modifier.width(6.dp))
                }

                val codec = stream.metadata.codec
                if (!codec.isNullOrBlank()) {
                    TagChip(text = codec)
                    Spacer(Modifier.width(6.dp))
                }

                val source = stream.metadata.source
                if (!source.isNullOrBlank()) {
                    TagChip(text = source)
                }
            }
        }
    }
}

@Composable
private fun QualityBadge(quality: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = qualityColor(quality),
    ) {
        Text(
            text = quality.uppercase(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TagChip(
    text: String,
    color: Color = Color(0xFF9CA3AF),
) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
    )
}
