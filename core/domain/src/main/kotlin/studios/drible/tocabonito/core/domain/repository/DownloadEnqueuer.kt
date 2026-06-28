package studios.drible.tocabonito.core.domain.repository

interface DownloadEnqueuer {
    suspend fun enqueue(downloadId: String)
}
