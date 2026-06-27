package studios.drible.tocabonito.core.domain.model

data class DownloadItem(
    val id: String,
    val mediaId: String,
    val episodeId: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val mediaType: MediaType,
    val quality: String,
    val source: String,
    val codec: String,
    val state: DownloadState,
    val progress: Double,
    val bytesWritten: Long,
    val totalBytes: Long,
    val estimatedBytes: Long,
    val localFilePath: String?,
    val fileExtension: String?,
    val dateQueued: Long,
    val dateCompleted: Long?,
    val failureCount: Int,
    val lastError: String?,
    val priority: DownloadPriority,
    val allowedOnCellular: Boolean,
    val speedBytesPerSecond: Double?,
) {
    val isActive: Boolean
        get() = state == DownloadState.QUEUED || state == DownloadState.RESOLVING || state == DownloadState.DOWNLOADING

    val isTerminal: Boolean
        get() = state == DownloadState.COMPLETED || state == DownloadState.CANCELLED

    val estimatedTimeRemaining: Double?
        get() {
            val speed = speedBytesPerSecond ?: return null
            if (speed <= 0 || totalBytes <= 0) return null
            return (totalBytes - bytesWritten).toDouble() / speed
        }
}

data class DownloadProgress(
    val downloadId: String,
    val progress: Double,
    val bytesWritten: Long,
    val totalBytes: Long,
    val speed: Double,
)
