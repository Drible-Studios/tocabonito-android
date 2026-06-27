package studios.drible.tocabonito.core.domain.repository

import kotlinx.coroutines.flow.Flow
import studios.drible.tocabonito.core.domain.model.DownloadItem
import studios.drible.tocabonito.core.domain.model.DownloadState

interface DownloadRepository {
    fun observeAll(): Flow<List<DownloadItem>>
    fun observeActive(): Flow<List<DownloadItem>>
    fun observeCompleted(): Flow<List<DownloadItem>>
    suspend fun get(downloadId: String): DownloadItem?
    suspend fun getByMedia(mediaId: String, episodeId: String?): DownloadItem?
    suspend fun save(item: DownloadItem)
    suspend fun delete(downloadId: String)
    suspend fun updateState(downloadId: String, state: DownloadState)
    suspend fun updateProgress(downloadId: String, progress: Double, bytesWritten: Long, speed: Double)
    suspend fun updateError(downloadId: String, failureCount: Int, lastError: String?)
    suspend fun updateLocalPath(downloadId: String, localFilePath: String)
    suspend fun totalStorageUsed(): Long
}
