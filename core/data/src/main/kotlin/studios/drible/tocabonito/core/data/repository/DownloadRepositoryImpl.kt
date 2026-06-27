package studios.drible.tocabonito.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import studios.drible.tocabonito.core.data.db.toDomain
import studios.drible.tocabonito.core.data.db.toEntity
import studios.drible.tocabonito.core.data.db.dao.DownloadDao
import studios.drible.tocabonito.core.domain.model.DownloadItem
import studios.drible.tocabonito.core.domain.model.DownloadState
import studios.drible.tocabonito.core.domain.repository.DownloadRepository
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
) : DownloadRepository {

    override fun observeAll(): Flow<List<DownloadItem>> =
        downloadDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeActive(): Flow<List<DownloadItem>> =
        downloadDao.observeActive().map { entities -> entities.map { it.toDomain() } }

    override fun observeCompleted(): Flow<List<DownloadItem>> =
        downloadDao.observeCompleted().map { entities -> entities.map { it.toDomain() } }

    override suspend fun get(downloadId: String): DownloadItem? =
        downloadDao.get(downloadId)?.toDomain()

    override suspend fun getByMedia(mediaId: String, episodeId: String?): DownloadItem? =
        downloadDao.getByMedia(mediaId, episodeId)?.toDomain()

    override suspend fun save(item: DownloadItem) {
        downloadDao.upsert(item.toEntity())
    }

    override suspend fun delete(downloadId: String) {
        downloadDao.delete(downloadId)
    }

    override suspend fun updateState(downloadId: String, state: DownloadState) {
        downloadDao.updateState(downloadId, state.name)
    }

    override suspend fun updateProgress(downloadId: String, progress: Double, bytesWritten: Long, speed: Double) {
        downloadDao.updateProgress(downloadId, progress, bytesWritten, speed)
    }

    override suspend fun updateError(downloadId: String, failureCount: Int, lastError: String?) {
        downloadDao.updateError(downloadId, failureCount, lastError)
    }

    override suspend fun updateLocalPath(downloadId: String, localFilePath: String) {
        downloadDao.updateLocalPath(downloadId, localFilePath)
    }

    override suspend fun totalStorageUsed(): Long =
        downloadDao.totalStorageUsed() ?: 0L
}
