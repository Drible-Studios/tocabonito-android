package studios.drible.tocabonito.feature.catalog.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.YearFilter
import studios.drible.tocabonito.core.ui.components.ErrorState
import studios.drible.tocabonito.core.ui.components.PosterCard
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette
import studios.drible.tocabonito.core.ui.theme.Poster

@Composable
fun ExploreScreen(
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalThemePalette.current

    when (val s = state) {
        is ExploreUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.accent)
            }
        }

        is ExploreUiState.Error -> {
            ErrorState(
                message = s.message,
                onRetry = { viewModel.onIntent(ExploreIntent.LoadGenres) },
                modifier = modifier,
            )
        }

        is ExploreUiState.Success -> {
            Column(modifier = modifier.fillMaxSize()) {
                // Genre chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(s.genres, key = { it.id }) { genre ->
                        val isSelected = s.selectedGenre?.id == genre.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onIntent(ExploreIntent.SelectGenre(genre)) },
                            label = { Text(genre.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.accent,
                                selectedLabelColor = palette.background,
                                containerColor = palette.cardBackground,
                                labelColor = palette.textPrimary,
                            ),
                        )
                    }
                }

                // Year filter chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(YearFilter.entries, key = { it.name }) { yearFilter ->
                        val isSelected = s.selectedYear == yearFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val next = if (isSelected) null else yearFilter
                                viewModel.onIntent(ExploreIntent.SelectYear(next))
                            },
                            label = { Text(yearFilter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.accent,
                                selectedLabelColor = palette.background,
                                containerColor = palette.cardBackground,
                                labelColor = palette.textPrimary,
                            ),
                        )
                    }
                }

                // Content grid with infinite scroll
                val gridState = rememberLazyGridState()
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val layoutInfo = gridState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        totalItems > 0 && lastVisibleIndex >= totalItems - 6
                    }
                }

                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore) {
                        viewModel.onIntent(ExploreIntent.LoadMore)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Poster.width),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Poster.gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(Poster.gridSpacing),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                ) {
                    items(s.items, key = { it.id }) { item ->
                        PosterCard(
                            posterUrl = item.posterUrl,
                            contentDescription = item.title,
                            onClick = { onItemClick(item) },
                        )
                    }
                }

                if (s.isLoadingMore) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = palette.accent)
                    }
                }
            }
        }
    }
}
