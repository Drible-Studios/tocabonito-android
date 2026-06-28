package studios.drible.tocabonito.core.data.download

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import studios.drible.tocabonito.core.domain.repository.DownloadEnqueuer
import javax.inject.Inject

class DefaultDownloadEnqueuer @Inject constructor(
    private val workManager: WorkManager,
) : DownloadEnqueuer {

    override suspend fun enqueue(downloadId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(DownloadWorker.KEY_DOWNLOAD_ID to downloadId))
            .addTag(TAG_DOWNLOAD)
            .addTag("$TAG_DOWNLOAD:$downloadId")
            .build()

        workManager.enqueue(request)
    }

    companion object {
        const val TAG_DOWNLOAD = "download"
    }
}
