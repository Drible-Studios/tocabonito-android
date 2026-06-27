package studios.drible.tocabonito.feature.catalog.explore

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import studios.drible.tocabonito.core.domain.model.Genre
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.YearFilter
import studios.drible.tocabonito.core.domain.repository.CatalogRepository
import studios.drible.tocabonito.core.ui.mvi.MviViewModel
import studios.drible.tocabonito.core.ui.util.toUserMessage
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
) : MviViewModel<ExploreUiState, ExploreIntent>(ExploreUiState.Loading) {

    init {
        onIntent(ExploreIntent.LoadGenres)
    }

    override fun onIntent(intent: ExploreIntent) {
        when (intent) {
            ExploreIntent.LoadGenres -> loadGenres()
            is ExploreIntent.SelectGenre -> selectGenre(intent.genre)
            is ExploreIntent.SelectYear -> selectYear(intent.yearFilter)
            ExploreIntent.LoadMore -> loadMore()
        }
    }

    private fun loadGenres() {
        viewModelScope.launch {
            try {
                val genres = catalogRepository.genres(MediaType.MOVIE)
                setState { ExploreUiState.Success(genres = genres) }
            } catch (e: Exception) {
                setState { ExploreUiState.Error(e.toUserMessage()) }
            }
        }
    }

    private fun selectGenre(genre: Genre) {
        val current = currentState
        if (current is ExploreUiState.Success) {
            setState { current.copy(selectedGenre = genre, items = emptyList(), currentPage = 1, hasMorePages = true) }
            discover(genre.id, MediaType.MOVIE, current.selectedYear, 1)
        }
    }

    private fun selectYear(yearFilter: YearFilter?) {
        val current = currentState
        if (current is ExploreUiState.Success) {
            setState { current.copy(selectedYear = yearFilter, items = emptyList(), currentPage = 1, hasMorePages = true) }
            val genre = current.selectedGenre
            if (genre != null) {
                discover(genre.id, MediaType.MOVIE, yearFilter, 1)
            }
        }
    }

    private fun loadMore() {
        val current = currentState
        if (current is ExploreUiState.Success && current.hasMorePages && !current.isLoadingMore) {
            val genre = current.selectedGenre ?: return
            val nextPage = current.currentPage + 1
            setState { current.copy(isLoadingMore = true) }
            discover(genre.id, MediaType.MOVIE, current.selectedYear, nextPage)
        }
    }

    private fun discover(genreId: Int, type: MediaType, yearFilter: YearFilter?, page: Int) {
        viewModelScope.launch {
            try {
                val results = catalogRepository.discover(genreId, type, yearFilter, page)
                val current = currentState
                if (current is ExploreUiState.Success) {
                    val updatedItems = if (page == 1) results else current.items + results
                    setState {
                        current.copy(
                            items = updatedItems,
                            currentPage = page,
                            hasMorePages = results.isNotEmpty(),
                            isLoadingMore = false,
                        )
                    }
                }
            } catch (e: Exception) {
                val current = currentState
                if (current is ExploreUiState.Success) {
                    setState { current.copy(isLoadingMore = false) }
                } else {
                    setState { ExploreUiState.Error(e.toUserMessage()) }
                }
            }
        }
    }
}
