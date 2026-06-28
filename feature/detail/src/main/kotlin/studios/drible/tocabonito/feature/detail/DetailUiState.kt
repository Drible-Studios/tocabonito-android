package studios.drible.tocabonito.feature.detail

import studios.drible.tocabonito.core.domain.model.Episode
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamOption
import studios.drible.tocabonito.feature.detail.model.StreamFilters

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
        val filters: StreamFilters = StreamFilters.EMPTY,
    ) : DetailUiState() {
        val filteredStreams: List<StreamOption>
            get() = if (!filters.isActive) streams
            else streams.filter { option ->
                (filters.quality == null || option.quality.equals(filters.quality, ignoreCase = true)) &&
                    (filters.source == null || option.metadata.source.equals(filters.source, ignoreCase = true)) &&
                    (filters.language == null || option.metadata.languages.any { it.equals(filters.language, ignoreCase = true) })
            }

        val availableQualities: List<String>
            get() = streams.map { it.quality }.distinct().sorted()

        val availableSources: List<String>
            get() = streams.mapNotNull { it.metadata.source }.distinct().sorted()

        val availableLanguages: List<String>
            get() = streams.flatMap { it.metadata.languages }.distinct().sorted()
    }
    data class Error(val message: String) : DetailUiState()
}

sealed class DetailIntent {
    data object ToggleFavorite : DetailIntent()
    data class ResolveStream(val option: StreamOption) : DetailIntent()
    data class SelectEpisode(val season: Int, val episode: Int) : DetailIntent()
    data object DismissResolvedLink : DetailIntent()
    data class UpdateFilters(val filters: StreamFilters) : DetailIntent()
}
