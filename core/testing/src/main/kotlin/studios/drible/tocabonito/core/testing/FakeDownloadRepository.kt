package studios.drible.tocabonito.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import studios.drible.tocabonito.core.domain.model.DownloadItem
import studios.drible.tocabonito.core.domain.model.DownloadState
import studios.drible.tocabonito.core.domain.repository.DownloadRepository

class FakeDownloadRepository : DownloadRepository {
    private val store = MutableStateFlow<List<DownloadItem>>(emptyList())

    override fun observeAll(): Flow<List<DownloadItem>> = store

    override fun observeActive(): Flow<List<DownloadItem>> =
        store.map { list -> list.filter { it.isActive } }

    override fun observeCompleted(): Flow<List<DownloadItem>> =
        store.map { list -> list.filter { it.isTerminal } }

    override suspend fun get(downloadId: String): DownloadItem? =
        store.value.find { it.id == downloadId }

    override suspend fun getByMedia(mediaId: String, episodeId: String?): DownloadItem? =
        store.value.find { it.mediaId == mediaId && it.episodeId == episodeId }

    override suspend fun save(item: DownloadItem) {
        store.value = store.value.filter { it.id != item.id } + item
    }

    override suspend fun delete(downloadId: String) {
        store.value = store.value.filter { it.id != downloadId }
    }

    override suspend fun updateState(downloadId: String, state: DownloadState) {
        store.value = store.value.map { item ->
            if (item.id == downloadId) item.copy(state = state) else item
        }
    }

    override suspend fun updateProgress(
        downloadId: String,
        progress: Double,
        bytesWritten: Long,
        speed: Double
    ) {
        store.value = store.value.map { item ->
            if (item.id == downloadId) {
                item.copy(
                    progress = progress,
                    bytesWritten = bytesWritten,
                    speedBytesPerSecond = speed
                )
            } else item
        }
    }

    override suspend fun updateError(downloadId: String, failureCount: Int, lastError: String?) {
        store.value = store.value.map { item ->
            if (item.id == downloadId) {
                item.copy(failureCount = failureCount, lastError = lastError)
            } else item
        }
    }

    override suspend fun updateLocalPath(downloadId: String, localFilePath: String) {
        store.value = store.value.map { item ->
            if (item.id == downloadId) item.copy(localFilePath = localFilePath) else item
        }
    }

    override suspend fun totalStorageUsed(): Long =
        store.value.sumOf { it.bytesWritten }
}
