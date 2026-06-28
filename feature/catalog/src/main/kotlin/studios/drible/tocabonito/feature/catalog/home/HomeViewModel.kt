package studios.drible.tocabonito.feature.catalog.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.repository.CatalogRepository
import studios.drible.tocabonito.core.domain.repository.ProgressRepository
import studios.drible.tocabonito.core.ui.mvi.MviViewModel
import studios.drible.tocabonito.core.ui.util.toUserMessage
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val progressRepository: ProgressRepository,
) : MviViewModel<HomeUiState, HomeIntent>(HomeUiState.Loading) {

    init {
        onIntent(HomeIntent.Load)
        observeContinueWatching()
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> load()
            HomeIntent.Refresh -> refresh()
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val (trending, popularMovies, popularSeries) = coroutineScope {
                    val trendingDeferred = async { catalogRepository.trending() }
                    val moviesDeferred = async { catalogRepository.popular(MediaType.MOVIE) }
                    val seriesDeferred = async { catalogRepository.popular(MediaType.SERIES) }
                    Triple(trendingDeferred.await(), moviesDeferred.await(), seriesDeferred.await())
                }
                val heroItem = trending.firstOrNull { it.backdropPath != null } ?: trending.firstOrNull()
                setState {
                    HomeUiState.Success(
                        trending = trending,
                        heroItem = heroItem,
                        popularMovies = popularMovies,
                        popularSeries = popularSeries,
                        continueWatching = emptyList(),
                    )
                }
            } catch (e: Exception) {
                setState { HomeUiState.Error(e.toUserMessage()) }
            }
        }
    }

    private fun refresh() {
        val current = currentState
        if (current is HomeUiState.Success) {
            setState { current.copy(isRefreshing = true) }
        }
        load()
    }

    private fun observeContinueWatching() {
        viewModelScope.launch {
            progressRepository.observeContinueWatching().collectLatest { progress ->
                val current = currentState
                if (current is HomeUiState.Success) {
                    setState { current.copy(continueWatching = progress) }
                }
            }
        }
    }
}
