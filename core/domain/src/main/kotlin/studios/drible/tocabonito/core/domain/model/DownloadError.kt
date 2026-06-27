package studios.drible.tocabonito.core.domain.model

sealed class DownloadError(override val message: String) : Exception(message) {
    data class InvalidTransition(val from: DownloadState, val to: DownloadState) :
        DownloadError("Invalid state transition: ${from.name} → ${to.name}")

    data class StorageLimitExceeded(val needed: Long, val available: Long) :
        DownloadError("Storage limit exceeded: need $needed bytes, have $available")

    data class CellularConfirmationRequired(val sizeBytes: Long) :
        DownloadError("Cellular confirmation required for $sizeBytes bytes")

    data class IntegrityCheckFailed(val reason: String) :
        DownloadError("Integrity check failed: $reason")

    data class ResolutionFailed(val underlying: String) :
        DownloadError("Stream resolution failed: $underlying")

    data class DownloadFailed(val underlying: String) :
        DownloadError("Download failed: $underlying")

    data class FileNotFound(val path: String) :
        DownloadError("File not found: $path")

    data class AlreadyDownloaded(val existingQuality: String) :
        DownloadError("Already downloaded in $existingQuality")

    data class CircuitBreakerOpen(val retryAfterMillis: Long) :
        DownloadError("Circuit breaker open, retry after $retryAfterMillis ms")

    data object Cancelled : DownloadError("Download cancelled")
}
