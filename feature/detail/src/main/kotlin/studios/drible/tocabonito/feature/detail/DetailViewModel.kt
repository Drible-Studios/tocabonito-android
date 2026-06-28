package studios.drible.tocabonito.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.repository.CatalogRepository
import studios.drible.tocabonito.core.domain.repository.FavoritesRepository
import studios.drible.tocabonito.core.domain.repository.StreamRepository
import studios.drible.tocabonito.feature.detail.model.StreamFilters
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val streamRepository: StreamRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val mediaId: String = checkNotNull(savedStateHandle["mediaId"])
    private val mediaTypeValue: String = checkNotNull(savedStateHandle["mediaType"])
    private val mediaType: MediaType = MediaType.entries.first { it.value == mediaTypeValue }

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            try {
                val mediaItem = catalogRepository.details(mediaId, mediaType)
                _state.value = DetailUiState.Success(
                    mediaItem = mediaItem,
                    isLoadingStreams = true,
                )

                // Load streams and seasons (for series) in parallel
                val streamsJob = launch {
                    try {
                        val streams = streamRepository.availableStreams(
                            imdbId = mediaId,
                            type = mediaType,
                            season = null,
                            episode = null,
                        )
                        updateSuccess { copy(streams = streams, isLoadingStreams = false) }
                    } catch (e: Exception) {
                        updateSuccess { copy(isLoadingStreams = false, streamError = e.message) }
                    }
                }

                val seasonsJob = if (mediaType == MediaType.SERIES) {
                    launch {
                        try {
                            val seasons = catalogRepository.seasons(mediaId)
                            updateSuccess { copy(seasons = seasons) }
                        } catch (_: Exception) {
                            // seasons failure is non-fatal
                        }
                    }
                } else null

                streamsJob.join()
                seasonsJob?.join()

                observeFavoriteState()
            } catch (e: Exception) {
                _state.value = DetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun observeFavoriteState() {
        viewModelScope.launch {
            favoritesRepository.observeIsFavorite(mediaId).collectLatest { isFav ->
                updateSuccess { copy(isFavorite = isFav) }
            }
        }
    }

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.ToggleFavorite -> toggleFavorite()
            is DetailIntent.ResolveStream -> resolveStream(intent)
            is DetailIntent.SelectEpisode -> selectEpisode(intent)
            is DetailIntent.DismissResolvedLink -> dismissResolvedLink()
            is DetailIntent.UpdateFilters -> updateFilters(intent.filters)
        }
    }

    private fun toggleFavorite() {
        val current = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            if (current.isFavorite) {
                favoritesRepository.remove(mediaId)
            } else {
                favoritesRepository.add(current.mediaItem)
            }
        }
    }

    private fun resolveStream(intent: DetailIntent.ResolveStream) {
        updateSuccess { copy(isResolvingStream = true, streamError = null) }
        viewModelScope.launch {
            try {
                val link = streamRepository.resolveStream(intent.option)
                updateSuccess { copy(isResolvingStream = false, resolvedLink = link) }
            } catch (e: Exception) {
                updateSuccess { copy(isResolvingStream = false, streamError = e.message) }
            }
        }
    }

    private fun selectEpisode(intent: DetailIntent.SelectEpisode) {
        updateSuccess { copy(isLoadingStreams = true, streams = emptyList(), streamError = null) }
        viewModelScope.launch {
            try {
                val streams = streamRepository.availableStreams(
                    imdbId = mediaId,
                    type = mediaType,
                    season = intent.season,
                    episode = intent.episode,
                )
                updateSuccess { copy(streams = streams, isLoadingStreams = false) }
            } catch (e: Exception) {
                updateSuccess { copy(isLoadingStreams = false, streamError = e.message) }
            }
        }
    }

    private fun dismissResolvedLink() {
        updateSuccess { copy(resolvedLink = null) }
    }

    private fun updateFilters(filters: StreamFilters) {
        updateSuccess { copy(filters = filters) }
    }

    private inline fun updateSuccess(reducer: DetailUiState.Success.() -> DetailUiState.Success) {
        val current = _state.value
        if (current is DetailUiState.Success) {
            _state.value = current.reducer()
        }
    }
}
