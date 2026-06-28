package studios.drible.tocabonito.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.repository.ProgressRepository
import studios.drible.tocabonito.core.domain.repository.StreamRepository
import studios.drible.tocabonito.core.domain.repository.SubtitleRepository
import studios.drible.tocabonito.core.ui.mvi.MviViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val progressRepository: ProgressRepository,
    private val subtitleRepository: SubtitleRepository,
    private val streamRepository: StreamRepository,
) : MviViewModel<PlayerUiState, PlayerIntent>(PlayerUiState()) {

    private var currentMediaId: String = ""
    private var currentEpisodeId: String? = null
    private var currentImdbId: String? = null
    private var currentTorrentId: String? = null
    private var retryCount: Int = 0
    private var periodicSaveJob: Job? = null

    companion object {
        private const val SKIP_AMOUNT_MS = 10_000L
        private const val PERIODIC_SAVE_INTERVAL_MS = 10_000L
        private const val MAX_RETRY_COUNT = 3
    }

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
            is PlayerIntent.Play -> handlePlay()
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
            is PlayerIntent.SetSubtitleTrack -> setState { copy(selectedSubtitle = intent.track) }
            is PlayerIntent.SetAudioTrack -> setState { copy(selectedAudio = intent.track) }
            is PlayerIntent.ShowTrackSelector -> setState { copy(showTrackSelector = true) }
            is PlayerIntent.DismissTrackSelector -> setState { copy(showTrackSelector = false) }
            is PlayerIntent.OnPlayerError -> handlePlayerError(intent)
            is PlayerIntent.UpdateTracks -> handleUpdateTracks(intent)
        }
    }

    private fun handleInitialize(intent: PlayerIntent.Initialize) {
        currentMediaId = intent.mediaId
        currentEpisodeId = intent.episodeId
        currentImdbId = intent.imdbId
        currentTorrentId = intent.torrentId
        retryCount = 0

        setState {
            copy(
                streamUrl = intent.streamUrl,
                mediaTitle = intent.title,
                playerError = null,
            )
        }

        // Load resume position
        viewModelScope.launch {
            val existing = progressRepository.get(intent.mediaId, intent.episodeId)
            if (existing != null) {
                val resumeMs = (existing.currentTime * 1000).toLong()
                setState { copy(resumePositionMs = resumeMs) }
            }
        }

        // Fetch subtitles from OpenSubtitles
        intent.imdbId?.let { imdbId ->
            viewModelScope.launch {
                val track = subtitleRepository.fetchSubtitle(imdbId)
                if (track != null) {
                    setState {
                        copy(
                            subtitleTracks = subtitleTracks + track,
                            selectedSubtitle = track,
                        )
                    }
                }
            }
        }
    }

    private fun handlePlay() {
        setState { copy(isPlaying = true, playerError = null) }
        retryCount = 0
        startPeriodicSave()
    }

    private fun handlePause() {
        setState { copy(isPlaying = false) }
        stopPeriodicSave()
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

    private fun handleUpdateTracks(intent: PlayerIntent.UpdateTracks) {
        setState {
            copy(
                audioTracks = intent.audioTracks,
                // Merge: keep sideloaded subs + add embedded ones
                subtitleTracks = intent.subtitleTracks + subtitleTracks.filter { it.isExternal },
            )
        }
    }

    private fun handlePlayerError(intent: PlayerIntent.OnPlayerError) {
        val torrentId = currentTorrentId
        if (torrentId != null && retryCount < MAX_RETRY_COUNT) {
            retryCount++
            viewModelScope.launch {
                try {
                    val newLink = streamRepository.resolveTranscode(torrentId)
                    setState {
                        copy(
                            streamUrl = newLink.directUrl,
                            playerError = null,
                        )
                    }
                } catch (_: Exception) {
                    setState {
                        copy(playerError = PlayerError(
                            message = intent.message,
                            retryCount = retryCount,
                            canRetry = false,
                        ))
                    }
                }
            }
        } else {
            setState {
                copy(playerError = PlayerError(
                    message = intent.message,
                    retryCount = retryCount,
                    canRetry = false,
                ))
            }
        }
    }

    private fun startPeriodicSave() {
        periodicSaveJob?.cancel()
        periodicSaveJob = viewModelScope.launch {
            while (true) {
                delay(PERIODIC_SAVE_INTERVAL_MS)
                if (currentState.isPlaying) {
                    saveProgress()
                }
            }
        }
    }

    private fun stopPeriodicSave() {
        periodicSaveJob?.cancel()
        periodicSaveJob = null
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
}
