package studios.drible.tocabonito.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import studios.drible.tocabonito.core.domain.model.DownloadState
import studios.drible.tocabonito.core.domain.repository.DownloadRepository
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val uiState: StateFlow<DownloadsUiState> = downloadRepository.observeAll()
        .map { items ->
            if (items.isEmpty()) {
                DownloadsUiState.Empty
            } else {
                DownloadsUiState.Content(
                    active = items.filter { it.state in setOf(DownloadState.QUEUED, DownloadState.RESOLVING, DownloadState.DOWNLOADING, DownloadState.PAUSED, DownloadState.VERIFYING) },
                    completed = items.filter { it.state == DownloadState.COMPLETED },
                    failed = items.filter { it.state == DownloadState.FAILED || it.state == DownloadState.CANCELLED },
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DownloadsUiState.Empty,
        )

    fun handleIntent(intent: DownloadsIntent) {
        viewModelScope.launch {
            when (intent) {
                is DownloadsIntent.Pause -> downloadRepository.updateState(intent.id, DownloadState.PAUSED)
                is DownloadsIntent.Resume -> downloadRepository.updateState(intent.id, DownloadState.QUEUED)
                is DownloadsIntent.Delete -> downloadRepository.delete(intent.id)
                is DownloadsIntent.Retry -> downloadRepository.updateState(intent.id, DownloadState.QUEUED)
            }
        }
    }
}
