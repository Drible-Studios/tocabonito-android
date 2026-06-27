package studios.drible.tocabonito.core.domain.repository

import kotlinx.coroutines.flow.Flow
import studios.drible.tocabonito.core.domain.model.MediaItem

interface FavoritesRepository {
    fun observeAll(): Flow<List<MediaItem>>
    suspend fun add(item: MediaItem)
    suspend fun remove(id: String)
    fun observeIsFavorite(id: String): Flow<Boolean>
}
