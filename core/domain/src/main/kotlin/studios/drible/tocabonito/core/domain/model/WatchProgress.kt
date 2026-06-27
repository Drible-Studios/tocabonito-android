package studios.drible.tocabonito.core.domain.model

data class WatchProgress(
    val id: String,
    val mediaItem: MediaItem,
    val currentTime: Double,
    val duration: Double,
    val lastWatched: Long,
    val episodeId: String?,
) {
    val percentComplete: Double
        get() = if (duration > 0) currentTime / duration else 0.0

    val shouldShowInContinueWatching: Boolean
        get() = currentTime > 120 && percentComplete < 0.9

    val isFinished: Boolean
        get() = percentComplete >= 0.9
}
