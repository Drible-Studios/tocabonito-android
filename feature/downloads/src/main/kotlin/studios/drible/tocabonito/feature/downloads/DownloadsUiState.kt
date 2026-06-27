package studios.drible.tocabonito.feature.downloads

import studios.drible.tocabonito.core.domain.model.DownloadItem

sealed class DownloadsUiState {
    data object Empty : DownloadsUiState()
    data class Content(
        val active: List<DownloadItem> = emptyList(),
        val completed: List<DownloadItem> = emptyList(),
        val failed: List<DownloadItem> = emptyList(),
    ) : DownloadsUiState()
}

sealed class DownloadsIntent {
    data class Pause(val id: String) : DownloadsIntent()
    data class Resume(val id: String) : DownloadsIntent()
    data class Delete(val id: String) : DownloadsIntent()
    data class Retry(val id: String) : DownloadsIntent()
}
