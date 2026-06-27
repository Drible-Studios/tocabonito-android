package studios.drible.tocabonito.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.repository.ProgressRepository

class FakeProgressRepository : ProgressRepository {
    private val store = MutableStateFlow<List<WatchProgress>>(emptyList())

    override suspend fun save(progress: WatchProgress) {
        store.value = store.value.filter { it.id != progress.id } + progress
    }

    override suspend fun get(mediaId: String, episodeId: String?): WatchProgress? =
        store.value.find { it.mediaItem.id == mediaId && it.episodeId == episodeId }

    override fun observeContinueWatching(): Flow<List<WatchProgress>> =
        store.map { list -> list.filter { it.shouldShowInContinueWatching } }

    override suspend fun markFinished(mediaId: String, episodeId: String?) {
        store.value = store.value.filter { !(it.mediaItem.id == mediaId && it.episodeId == episodeId) }
    }
}
