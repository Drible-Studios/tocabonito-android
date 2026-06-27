package studios.drible.tocabonito.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.repository.FavoritesRepository

class FakeFavoritesRepository : FavoritesRepository {
    private val items = MutableStateFlow<List<MediaItem>>(emptyList())

    override fun observeAll(): Flow<List<MediaItem>> = items

    override suspend fun add(item: MediaItem) {
        items.value = items.value + item
    }

    override suspend fun remove(id: String) {
        items.value = items.value.filter { it.id != id }
    }

    override fun observeIsFavorite(id: String): Flow<Boolean> =
        items.map { list -> list.any { it.id == id } }
}
