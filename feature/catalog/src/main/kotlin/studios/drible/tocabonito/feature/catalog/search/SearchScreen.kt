package studios.drible.tocabonito.feature.catalog.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.ui.components.ErrorState
import studios.drible.tocabonito.core.ui.components.PosterCard
import studios.drible.tocabonito.core.ui.components.ShimmerBox
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette
import studios.drible.tocabonito.core.ui.theme.Poster

@Composable
fun SearchScreen(
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalThemePalette.current
    var queryText by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = queryText,
            onValueChange = { value ->
                queryText = value
                viewModel.onIntent(SearchIntent.UpdateQuery(value))
            },
            placeholder = { Text("Search movies and series", color = palette.textSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = palette.textSecondary,
                )
            },
            trailingIcon = {
                if (queryText.isNotEmpty()) {
                    IconButton(onClick = {
                        queryText = ""
                        viewModel.onIntent(SearchIntent.Clear)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear",
                            tint = palette.textSecondary,
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.textSecondary,
                cursorColor = palette.accent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when (val s = state) {
            is SearchUiState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Search movies and series",
                        color = palette.textSecondary,
                    )
                }
            }

            is SearchUiState.Loading -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Poster.width),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(Poster.gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(Poster.gridSpacing),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(12) {
                        ShimmerBox(width = Poster.width, height = Poster.height)
                    }
                }
            }

            is SearchUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Poster.width),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(Poster.gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(Poster.gridSpacing),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(s.results, key = { it.id }) { item ->
                        PosterCard(
                            posterUrl = item.posterUrl,
                            contentDescription = item.title,
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }

            is SearchUiState.Error -> {
                ErrorState(
                    message = s.message,
                    onRetry = { viewModel.onIntent(SearchIntent.UpdateQuery(queryText)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
