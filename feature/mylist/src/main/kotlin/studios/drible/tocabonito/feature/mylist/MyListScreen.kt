package studios.drible.tocabonito.feature.mylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.ui.components.PosterCard
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette
import studios.drible.tocabonito.core.ui.theme.Poster
import studios.drible.tocabonito.core.ui.theme.Spacing

@Composable
fun MyListScreen(
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is MyListUiState.Empty -> EmptyContent(modifier = modifier)
        is MyListUiState.Content -> ContentGrid(
            items = s.items,
            onItemClick = onItemClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    val palette = LocalThemePalette.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Spacing.xl),
        ) {
            Text(
                text = "Your list is empty",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Browse content and tap the heart to add favorites",
                color = palette.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun ContentGrid(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(Poster.width),
        contentPadding = PaddingValues(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Poster.gridSpacing),
        verticalArrangement = Arrangement.spacedBy(Poster.gridSpacing),
        modifier = modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { item ->
            PosterCard(
                posterUrl = item.posterUrl,
                contentDescription = item.title,
                onClick = { onItemClick(item) },
            )
        }
    }
}
