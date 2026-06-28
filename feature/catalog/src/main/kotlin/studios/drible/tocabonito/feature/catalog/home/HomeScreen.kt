package studios.drible.tocabonito.feature.catalog.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.ui.components.ErrorState
import studios.drible.tocabonito.core.ui.components.PosterCard
import studios.drible.tocabonito.core.ui.components.ShimmerBox
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette
import studios.drible.tocabonito.core.ui.theme.Poster
import studios.drible.tocabonito.feature.catalog.home.components.HeroBanner
import studios.drible.tocabonito.feature.catalog.home.components.SilhouetteBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalThemePalette.current

    when (val s = state) {
        is HomeUiState.Loading -> {
            LoadingContent(modifier = modifier)
        }

        is HomeUiState.Error -> {
            ErrorState(
                message = s.message,
                onRetry = { viewModel.onIntent(HomeIntent.Load) },
                modifier = modifier,
            )
        }

        is HomeUiState.Success -> {
            PullToRefreshBox(
                isRefreshing = s.isRefreshing,
                onRefresh = { viewModel.onIntent(HomeIntent.Refresh) },
                modifier = modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SilhouetteBackground()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // Hero Banner
                        s.heroItem?.let { hero ->
                            HeroBanner(
                                item = hero,
                                onPlay = { onItemClick(hero) },
                                onInfo = { onItemClick(hero) },
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Trending carousel
                        if (s.trending.isNotEmpty()) {
                            SectionHeader("Trending")
                            MediaCarousel(items = s.trending, onItemClick = onItemClick)
                            Spacer(Modifier.height(24.dp))
                        }

                        // Popular Movies carousel
                        if (s.popularMovies.isNotEmpty()) {
                            SectionHeader("Popular Movies")
                            MediaCarousel(items = s.popularMovies, onItemClick = onItemClick)
                            Spacer(Modifier.height(24.dp))
                        }

                        // Popular Series carousel
                        if (s.popularSeries.isNotEmpty()) {
                            SectionHeader("Popular Series")
                            MediaCarousel(items = s.popularSeries, onItemClick = onItemClick)
                            Spacer(Modifier.height(24.dp))
                        }

                        // Continue Watching carousel
                        if (s.continueWatching.isNotEmpty()) {
                            SectionHeader("Continue Watching")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(s.continueWatching, key = { it.id }) { progress ->
                                    PosterCard(
                                        posterUrl = progress.mediaItem.posterUrl,
                                        contentDescription = progress.mediaItem.title,
                                        onClick = { onItemClick(progress.mediaItem) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val palette = LocalThemePalette.current
    Text(
        text = title,
        color = palette.textPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun MediaCarousel(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Spacer(Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(6) {
                ShimmerBox(width = Poster.width, height = Poster.height)
            }
        }
    }
}
