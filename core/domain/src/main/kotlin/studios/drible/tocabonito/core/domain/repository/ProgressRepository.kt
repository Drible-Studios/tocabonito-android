package studios.drible.tocabonito.core.domain.repository

import kotlinx.coroutines.flow.Flow
import studios.drible.tocabonito.core.domain.model.WatchProgress

interface ProgressRepository {
    suspend fun save(progress: WatchProgress)
    suspend fun get(mediaId: String, episodeId: String?): WatchProgress?
    fun observeContinueWatching(): Flow<List<WatchProgress>>
    suspend fun markFinished(mediaId: String, episodeId: String?)
}
