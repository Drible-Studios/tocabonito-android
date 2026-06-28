package studios.drible.tocabonito.feature.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.ui.components.ErrorState
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette
import studios.drible.tocabonito.feature.detail.components.SeasonEpisodeList
import studios.drible.tocabonito.feature.detail.components.StreamFilterChips
import studios.drible.tocabonito.feature.detail.components.StreamSelectionSheet
import studios.drible.tocabonito.feature.detail.model.StreamFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onNavigateToPlayer: (mediaId: String, streamUrl: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is DetailUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val palette = LocalThemePalette.current
                CircularProgressIndicator(color = palette.accent)
            }
        }

        is DetailUiState.Error -> {
            ErrorState(
                message = s.message,
                onRetry = { /* ViewModel init retriggers on recreation */ },
                modifier = modifier,
            )
        }

        is DetailUiState.Success -> {
            val resolvedLink = s.resolvedLink
            LaunchedEffect(resolvedLink) {
                resolvedLink?.let { link ->
                    onNavigateToPlayer(s.mediaItem.id, link.directUrl)
                    viewModel.onIntent(DetailIntent.DismissResolvedLink)
                }
            }

            DetailContent(
                state = s,
                onBack = onBack,
                onToggleFavorite = { viewModel.onIntent(DetailIntent.ToggleFavorite) },
                onStreamSelected = { viewModel.onIntent(DetailIntent.ResolveStream(it)) },
                onEpisodeSelected = { season, episode ->
                    viewModel.onIntent(DetailIntent.SelectEpisode(season, episode))
                },
                onFiltersChanged = { viewModel.onIntent(DetailIntent.UpdateFilters(it)) },
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    state: DetailUiState.Success,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onStreamSelected: (studios.drible.tocabonito.core.domain.model.StreamOption) -> Unit,
    onEpisodeSelected: (season: Int, episode: Int) -> Unit,
    onFiltersChanged: (StreamFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalThemePalette.current
    var showStreamSheet by remember { mutableStateOf(false) }
    var overviewExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(palette.background),
        ) {
            // Backdrop header
            BackdropHeader(
                mediaItem = state.mediaItem,
                onBack = onBack,
            )

            // Metadata row
            MetadataRow(
                mediaItem = state.mediaItem,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Overview
            if (state.mediaItem.overview.isNotBlank()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = state.mediaItem.overview,
                        color = palette.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = if (overviewExpanded) Int.MAX_VALUE else 3,
                        modifier = Modifier.clickable { overviewExpanded = !overviewExpanded },
                    )
                    if (!overviewExpanded) {
                        Text(
                            text = "Show more",
                            color = palette.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { overviewExpanded = true }
                                .padding(top = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Stream error
            if (state.streamError != null) {
                Text(
                    text = state.streamError,
                    color = Color(0xFFEF4444),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            // Stream filter chips
            if (state.streams.size > 1) {
                StreamFilterChips(
                    filters = state.filters,
                    availableQualities = state.availableQualities,
                    availableSources = state.availableSources,
                    availableLanguages = state.availableLanguages,
                    onFiltersChanged = onFiltersChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            // Play / Streams buttons
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                AnimatedVisibility(visible = state.filteredStreams.isNotEmpty() || state.isResolvingStream) {
                    Column {
                        Button(
                            onClick = {
                                if (state.filteredStreams.isNotEmpty() && !state.isResolvingStream) {
                                    onStreamSelected(state.filteredStreams.first())
                                }
                            },
                            enabled = !state.isResolvingStream && state.filteredStreams.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accent,
                                contentColor = if (palette.isLight) palette.background else Color.Black,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.isResolvingStream) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = if (palette.isLight) palette.background else Color.Black,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                )
                                Text(
                                    text = "Play",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // My List button
                        Button(
                            onClick = onToggleFavorite,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.surfaceElevated,
                                contentColor = palette.textPrimary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = if (state.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (state.isFavorite) "In My List" else "My List",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showStreamSheet = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Streams (${state.filteredStreams.size})",
                                color = palette.textPrimary,
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }

                AnimatedVisibility(visible = state.isLoadingStreams) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = palette.accent,
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "  Loading streams...",
                            color = palette.textTertiary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // Seasons / Episodes
            if (state.seasons.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Episodes",
                    color = palette.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                SeasonEpisodeList(
                    seasons = state.seasons,
                    onEpisodeSelected = onEpisodeSelected,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Stream selection bottom sheet
    if (showStreamSheet && state.filteredStreams.isNotEmpty()) {
        StreamSelectionSheet(
            streams = state.filteredStreams,
            onStreamSelected = { stream ->
                showStreamSheet = false
                onStreamSelected(stream)
            },
            onDismiss = { showStreamSheet = false },
        )
    }
}

@Composable
private fun BackdropHeader(
    mediaItem: MediaItem,
    onBack: () -> Unit,
) {
    val palette = LocalThemePalette.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        AsyncImage(
            model = mediaItem.backdropUrl ?: mediaItem.posterUrl,
            contentDescription = mediaItem.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, palette.background),
                    ),
                ),
        )

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }

        // Title + year
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                text = mediaItem.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(
                text = mediaItem.releaseYear.toString(),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun MetadataRow(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
) {
    val palette = LocalThemePalette.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Year badge
        Text(
            text = mediaItem.releaseYear.toString(),
            color = palette.background,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(palette.textTertiary, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )

        Spacer(Modifier.weight(1f))

        // Vote average
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Rating",
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = " %.1f".format(mediaItem.voteAverage),
                color = palette.textSecondary,
                fontSize = 14.sp,
            )
        }
    }
}
