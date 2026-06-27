package studios.drible.tocabonito.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import studios.drible.tocabonito.core.data.db.toDomain
import studios.drible.tocabonito.core.data.db.toEntity
import studios.drible.tocabonito.core.data.db.dao.WatchProgressDao
import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.repository.ProgressRepository
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val dao: WatchProgressDao,
) : ProgressRepository {

    override suspend fun save(progress: WatchProgress) {
        dao.upsert(progress.toEntity())
    }

    override suspend fun get(mediaId: String, episodeId: String?): WatchProgress? =
        dao.get(mediaId, episodeId)?.toDomain()

    override fun observeContinueWatching(): Flow<List<WatchProgress>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }.filter { it.shouldShowInContinueWatching }
        }

    override suspend fun markFinished(mediaId: String, episodeId: String?) {
        dao.delete(mediaId, episodeId)
    }
}
