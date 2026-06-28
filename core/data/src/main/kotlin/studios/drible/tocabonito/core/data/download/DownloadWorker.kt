package studios.drible.tocabonito.core.data.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import studios.drible.tocabonito.core.domain.model.DownloadState
import studios.drible.tocabonito.core.domain.repository.DownloadRepository
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloadRepository: DownloadRepository,
    private val httpDownloader: HttpDownloader,
    private val notificationHelper: DownloadNotificationHelper,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        val item = downloadRepository.get(downloadId) ?: return Result.failure()

        try {
            downloadRepository.updateState(downloadId, DownloadState.DOWNLOADING)

            val downloadsDir = File(applicationContext.filesDir, "downloads")
            val fileName = "${downloadId}.${item.fileExtension ?: "mkv"}"
            val destination = File(downloadsDir, fileName)

            setForeground(
                notificationHelper.buildForegroundInfo(
                    downloadId = downloadId,
                    title = item.title,
                    progress = 0,
                )
            )

            val totalBytes = item.totalBytes.takeIf { it > 0 } ?: item.estimatedBytes
            var lastNotificationUpdate = 0L

            httpDownloader.download(
                url = item.localFilePath ?: return Result.failure(),
                destination = destination,
                onProgress = { bytesWritten ->
                    val progress = if (totalBytes > 0) {
                        bytesWritten.toDouble() / totalBytes
                    } else 0.0
                    val speed = 0.0 // Speed calculation would need timestamps
                    downloadRepository.updateProgress(downloadId, progress, bytesWritten, speed)

                    val now = System.currentTimeMillis()
                    if (now - lastNotificationUpdate > NOTIFICATION_UPDATE_INTERVAL_MS) {
                        lastNotificationUpdate = now
                        setForeground(
                            notificationHelper.buildForegroundInfo(
                                downloadId = downloadId,
                                title = item.title,
                                progress = (progress * 100).toInt(),
                            )
                        )
                    }
                },
            )

            downloadRepository.updateLocalPath(downloadId, destination.absolutePath)
            downloadRepository.updateState(downloadId, DownloadState.COMPLETED)
            return Result.success()
        } catch (e: Exception) {
            val newFailureCount = item.failureCount + 1
            downloadRepository.updateError(downloadId, newFailureCount, e.message)

            return if (newFailureCount < MAX_RETRIES) {
                downloadRepository.updateState(downloadId, DownloadState.QUEUED)
                Result.retry()
            } else {
                downloadRepository.updateState(downloadId, DownloadState.FAILED)
                Result.failure()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: "unknown"
        return notificationHelper.buildForegroundInfo(
            downloadId = downloadId,
            title = "Downloading...",
            progress = 0,
        )
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        private const val MAX_RETRIES = 3
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 1000L
    }
}
