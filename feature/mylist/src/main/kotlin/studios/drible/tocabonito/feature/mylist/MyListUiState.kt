package studios.drible.tocabonito.feature.mylist

import studios.drible.tocabonito.core.domain.model.MediaItem

sealed class MyListUiState {
    data object Empty : MyListUiState()
    data class Content(val items: List<MediaItem>) : MyListUiState()
}
