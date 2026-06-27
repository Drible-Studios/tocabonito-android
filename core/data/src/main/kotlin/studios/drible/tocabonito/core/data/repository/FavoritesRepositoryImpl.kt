package studios.drible.tocabonito.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import studios.drible.tocabonito.core.data.db.toDomain
import studios.drible.tocabonito.core.data.db.toEntity
import studios.drible.tocabonito.core.data.db.dao.FavoriteDao
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.repository.FavoritesRepository
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
) : FavoritesRepository {

    override fun observeAll(): Flow<List<MediaItem>> =
        favoriteDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun add(item: MediaItem) {
        favoriteDao.insert(item.toEntity())
    }

    override suspend fun remove(id: String) {
        favoriteDao.delete(id)
    }

    override fun observeIsFavorite(id: String): Flow<Boolean> =
        favoriteDao.observeIsFavorite(id)
}
