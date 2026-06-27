package studios.drible.tocabonito.feature.detail

import studios.drible.tocabonito.core.domain.model.Episode
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamOption

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(
        val mediaItem: MediaItem,
        val streams: List<StreamOption> = emptyList(),
        val seasons: List<List<Episode>> = emptyList(),
        val isFavorite: Boolean = false,
        val isLoadingStreams: Boolean = false,
        val isResolvingStream: Boolean = false,
        val resolvedLink: StreamLink? = null,
        val streamError: String? = null,
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

sealed class DetailIntent {
    data object ToggleFavorite : DetailIntent()
    data class ResolveStream(val option: StreamOption) : DetailIntent()
    data class SelectEpisode(val season: Int, val episode: Int) : DetailIntent()
    data object DismissResolvedLink : DetailIntent()
}
