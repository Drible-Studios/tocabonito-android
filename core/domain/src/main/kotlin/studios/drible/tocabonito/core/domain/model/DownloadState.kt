package studios.drible.tocabonito.core.domain.model

enum class DownloadState {
    QUEUED,
    RESOLVING,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

enum class PauseReason {
    USER_REQUESTED,
    NETWORK_LOST,
    CELLULAR_NOT_ALLOWED,
    STORAGE_FULL,
}

enum class DownloadPriority(val value: Int) : Comparable<DownloadPriority> {
    AUTO_NEXT_EPISODE(50),
    USER_INITIATED(100),
}

object DownloadStateMachine {
    private val validTransitions = setOf(
        DownloadState.QUEUED to DownloadState.RESOLVING,
        DownloadState.QUEUED to DownloadState.CANCELLED,
        DownloadState.RESOLVING to DownloadState.DOWNLOADING,
        DownloadState.RESOLVING to DownloadState.FAILED,
        DownloadState.DOWNLOADING to DownloadState.PAUSED,
        DownloadState.DOWNLOADING to DownloadState.VERIFYING,
        DownloadState.DOWNLOADING to DownloadState.FAILED,
        DownloadState.DOWNLOADING to DownloadState.CANCELLED,
        DownloadState.PAUSED to DownloadState.QUEUED,
        DownloadState.PAUSED to DownloadState.RESOLVING,
        DownloadState.PAUSED to DownloadState.CANCELLED,
        DownloadState.VERIFYING to DownloadState.COMPLETED,
        DownloadState.VERIFYING to DownloadState.FAILED,
        DownloadState.FAILED to DownloadState.QUEUED,
        DownloadState.FAILED to DownloadState.CANCELLED,
    )

    fun canTransition(from: DownloadState, to: DownloadState): Boolean =
        (from to to) in validTransitions

    fun transition(from: DownloadState, to: DownloadState): DownloadState {
        if (!canTransition(from, to)) {
            throw DownloadError.InvalidTransition(from, to)
        }
        return to
    }
}
