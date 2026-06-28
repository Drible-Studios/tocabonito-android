package studios.drible.tocabonito.feature.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studios.drible.tocabonito.core.domain.model.StreamOption
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette

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
                    StreamRow(
                        stream = stream,
                        onPlay = { onStreamSelected(stream) },
                        onDownload = null,
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
