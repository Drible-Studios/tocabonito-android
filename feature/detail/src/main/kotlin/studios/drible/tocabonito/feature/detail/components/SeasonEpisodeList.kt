package studios.drible.tocabonito.feature.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import studios.drible.tocabonito.core.domain.model.Episode
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette

@Composable
fun SeasonEpisodeList(
    seasons: List<List<Episode>>,
    onEpisodeSelected: (season: Int, episode: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        seasons.forEachIndexed { index, episodes ->
            SeasonSection(
                seasonNumber = index + 1,
                episodes = episodes,
                onEpisodeSelected = onEpisodeSelected,
            )
        }
    }
}

@Composable
private fun SeasonSection(
    seasonNumber: Int,
    episodes: List<Episode>,
    onEpisodeSelected: (season: Int, episode: Int) -> Unit,
) {
    val palette = LocalThemePalette.current
    var expanded by remember { mutableStateOf(seasonNumber == 1) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Season $seasonNumber",
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse season" else "Expand season",
                tint = palette.textSecondary,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                episodes.forEach { episode ->
                    EpisodeRow(
                        episode = episode,
                        onClick = { onEpisodeSelected(episode.seasonNumber, episode.episodeNumber) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit,
) {
    val palette = LocalThemePalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (episode.stillUrl != null) {
            AsyncImage(
                model = episode.stillUrl,
                contentDescription = episode.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(96.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.formattedCode,
                color = palette.textTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = episode.name,
                color = palette.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
            )
        }
    }
}
