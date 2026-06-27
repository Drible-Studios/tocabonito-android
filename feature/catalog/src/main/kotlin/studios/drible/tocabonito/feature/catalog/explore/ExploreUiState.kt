package studios.drible.tocabonito.feature.catalog.explore

import studios.drible.tocabonito.core.domain.model.Genre
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.YearFilter

sealed class ExploreUiState {
    data object Loading : ExploreUiState()
    data class Success(
        val genres: List<Genre>,
        val selectedGenre: Genre? = null,
        val selectedYear: YearFilter? = null,
        val items: List<MediaItem> = emptyList(),
        val isLoadingMore: Boolean = false,
        val currentPage: Int = 1,
        val hasMorePages: Boolean = true,
    ) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}

sealed class ExploreIntent {
    data object LoadGenres : ExploreIntent()
    data class SelectGenre(val genre: Genre) : ExploreIntent()
    data class SelectYear(val yearFilter: YearFilter?) : ExploreIntent()
    data object LoadMore : ExploreIntent()
}
