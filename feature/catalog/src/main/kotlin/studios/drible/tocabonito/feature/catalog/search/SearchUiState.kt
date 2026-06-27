package studios.drible.tocabonito.feature.catalog.search

import studios.drible.tocabonito.core.domain.model.MediaItem

sealed class SearchUiState {
    data object Idle : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val results: List<MediaItem>, val query: String) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

sealed class SearchIntent {
    data class UpdateQuery(val query: String) : SearchIntent()
    data object Clear : SearchIntent()
}
