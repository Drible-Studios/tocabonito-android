package studios.drible.tocabonito.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import studios.drible.tocabonito.core.domain.model.AudioTrack
import studios.drible.tocabonito.core.domain.model.SubtitleTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorSheet(
    state: PlayerUiState,
    onSelectAudio: (AudioTrack) -> Unit,
    onSelectSubtitle: (SubtitleTrack?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Audio section
            if (state.audioTracks.isNotEmpty()) {
                Text(
                    text = "Audio",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                state.audioTracks.forEach { track ->
                    TrackRow(
                        label = track.displayName,
                        isSelected = track == state.selectedAudio,
                        onClick = { onSelectAudio(track) },
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // Subtitle section
            Text(
                text = "Subtitles",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            // "Off" option
            TrackRow(
                label = "Off",
                isSelected = state.selectedSubtitle == null,
                onClick = { onSelectSubtitle(null) },
            )
            state.subtitleTracks.forEach { track ->
                TrackRow(
                    label = track.displayName,
                    isSelected = track == state.selectedSubtitle,
                    onClick = { onSelectSubtitle(track) },
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
