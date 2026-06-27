package studios.drible.tocabonito.feature.catalog.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
                val trending = catalogRepository.trending()
                setState { HomeUiState.Success(trending = trending, continueWatching = emptyList()) }
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
