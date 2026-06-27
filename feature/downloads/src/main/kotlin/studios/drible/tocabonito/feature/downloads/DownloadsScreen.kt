package studios.drible.tocabonito.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import studios.drible.tocabonito.core.domain.model.DownloadItem
import studios.drible.tocabonito.core.domain.model.DownloadState

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    DownloadsContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
    )
}

@Composable
private fun DownloadsContent(
    uiState: DownloadsUiState,
    onIntent: (DownloadsIntent) -> Unit,
) {
    when (uiState) {
        is DownloadsUiState.Empty -> EmptyDownloads()
        is DownloadsUiState.Content -> DownloadsList(content = uiState, onIntent = onIntent)
    }
}

@Composable
private fun EmptyDownloads() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No Downloads",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your downloaded content will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DownloadsList(
    content: DownloadsUiState.Content,
    onIntent: (DownloadsIntent) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (content.active.isNotEmpty()) {
            item {
                SectionHeader(title = "Downloading")
            }
            items(content.active, key = { it.id }) { item ->
                ActiveDownloadCard(item = item, onIntent = onIntent)
            }
        }
        if (content.completed.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Completed")
            }
            items(content.completed, key = { it.id }) { item ->
                CompletedDownloadCard(item = item, onIntent = onIntent)
            }
        }
        if (content.failed.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Failed")
            }
            items(content.failed, key = { it.id }) { item ->
                FailedDownloadCard(item = item, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun ActiveDownloadCard(
    item: DownloadItem,
    onIntent: (DownloadsIntent) -> Unit,
) {
    DownloadCard(item = item) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StateLabel(state = item.state)
                QualityBadge(quality = item.quality)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { item.progress.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(item.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompletedDownloadCard(
    item: DownloadItem,
    onIntent: (DownloadsIntent) -> Unit,
) {
    DownloadCard(item = item) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StateLabel(state = item.state)
            QualityBadge(quality = item.quality)
        }
    }
}

@Composable
private fun FailedDownloadCard(
    item: DownloadItem,
    onIntent: (DownloadsIntent) -> Unit,
) {
    DownloadCard(item = item) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                StateLabel(state = item.state)
                item.lastError?.let { error ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Button(onClick = { onIntent(DownloadsIntent.Retry(item.id)) }) {
                Text(text = "Retry")
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadItem,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            PosterThumbnail()
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun PosterThumbnail() {
    Surface(
        modifier = Modifier
            .size(width = 56.dp, height = 80.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {}
}

@Composable
private fun StateLabel(state: DownloadState) {
    val label = when (state) {
        DownloadState.QUEUED -> "Queued"
        DownloadState.RESOLVING -> "Resolving"
        DownloadState.DOWNLOADING -> "Downloading"
        DownloadState.PAUSED -> "Paused"
        DownloadState.VERIFYING -> "Verifying"
        DownloadState.COMPLETED -> "Completed"
        DownloadState.FAILED -> "Failed"
        DownloadState.CANCELLED -> "Cancelled"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun QualityBadge(quality: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = quality,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
