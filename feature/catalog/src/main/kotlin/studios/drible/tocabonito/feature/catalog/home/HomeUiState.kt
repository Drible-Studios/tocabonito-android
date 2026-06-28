package studios.drible.tocabonito.feature.catalog.home

import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.WatchProgress

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val trending: List<MediaItem>,
        val heroItem: MediaItem? = null,
        val popularMovies: List<MediaItem> = emptyList(),
        val popularSeries: List<MediaItem> = emptyList(),
        val continueWatching: List<WatchProgress> = emptyList(),
        val isRefreshing: Boolean = false,
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

sealed class HomeIntent {
    data object Load : HomeIntent()
    data object Refresh : HomeIntent()
}
