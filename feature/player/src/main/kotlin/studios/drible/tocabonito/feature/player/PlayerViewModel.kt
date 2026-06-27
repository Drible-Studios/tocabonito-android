package studios.drible.tocabonito.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.repository.ProgressRepository
import studios.drible.tocabonito.core.ui.mvi.MviViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val progressRepository: ProgressRepository,
) : MviViewModel<PlayerUiState, PlayerIntent>(PlayerUiState()) {

    private var currentMediaId: String = ""
    private var currentEpisodeId: String? = null

    init {
        val mediaId: String? = savedStateHandle["mediaId"]
        val rawStreamUrl: String? = savedStateHandle["streamUrl"]
        if (mediaId != null && rawStreamUrl != null) {
            val streamUrl = android.net.Uri.decode(rawStreamUrl)
            onIntent(PlayerIntent.Initialize(mediaId, streamUrl, mediaId, null))
        }
    }

    override fun onIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.Initialize -> handleInitialize(intent)
            is PlayerIntent.Play -> setState { copy(isPlaying = true) }
            is PlayerIntent.Pause -> handlePause()
            is PlayerIntent.Seek -> setState { copy(currentPositionMs = intent.positionMs) }
            is PlayerIntent.SkipForward -> setState {
                copy(currentPositionMs = (currentPositionMs + SKIP_AMOUNT_MS).coerceAtMost(durationMs))
            }
            is PlayerIntent.SkipBackward -> setState {
                copy(currentPositionMs = (currentPositionMs - SKIP_AMOUNT_MS).coerceAtLeast(0))
            }
            is PlayerIntent.UpdatePosition -> handleUpdatePosition(intent)
            is PlayerIntent.ToggleControls -> setState { copy(showControls = !showControls) }
        }
    }

    private fun handleInitialize(intent: PlayerIntent.Initialize) {
        currentMediaId = intent.mediaId
        currentEpisodeId = intent.episodeId
        setState {
            copy(
                streamUrl = intent.streamUrl,
                mediaTitle = intent.title,
            )
        }
        viewModelScope.launch {
            val existing = progressRepository.get(intent.mediaId, intent.episodeId)
            if (existing != null) {
                val resumeMs = (existing.currentTime * 1000).toLong()
                setState { copy(resumePositionMs = resumeMs) }
            }
        }
    }

    private fun handlePause() {
        setState { copy(isPlaying = false) }
        saveProgress()
    }

    private fun handleUpdatePosition(intent: PlayerIntent.UpdatePosition) {
        setState {
            copy(
                currentPositionMs = intent.positionMs,
                durationMs = intent.durationMs,
            )
        }
        if (!currentState.isPlaying) {
            saveProgress()
        }
    }

    private fun saveProgress() {
        val s = currentState
        if (currentMediaId.isBlank() || s.durationMs <= 0) return
        viewModelScope.launch {
            val mediaItem = MediaItem(
                id = currentMediaId,
                title = s.mediaTitle,
                overview = "",
                posterPath = null,
                backdropPath = null,
                mediaType = MediaType.MOVIE,
                releaseYear = 0,
                voteAverage = 0.0,
                genreIds = emptyList(),
            )
            val progress = WatchProgress(
                id = "wp_${currentMediaId}_${currentEpisodeId ?: ""}",
                mediaItem = mediaItem,
                currentTime = s.currentPositionMs / 1000.0,
                duration = s.durationMs / 1000.0,
                lastWatched = System.currentTimeMillis(),
                episodeId = currentEpisodeId,
            )
            progressRepository.save(progress)
        }
    }

    companion object {
        private const val SKIP_AMOUNT_MS = 10_000L
    }
}
