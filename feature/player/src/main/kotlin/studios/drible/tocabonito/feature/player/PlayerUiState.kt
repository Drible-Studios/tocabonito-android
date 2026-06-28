package studios.drible.tocabonito.feature.player

import studios.drible.tocabonito.core.domain.model.AudioTrack
import studios.drible.tocabonito.core.domain.model.SubtitleTrack

data class PlayerUiState(
    val streamUrl: String = "",
    val mediaTitle: String = "",
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val resumePositionMs: Long? = null,
    val showControls: Boolean = true,
    val isBuffering: Boolean = false,
    // Subtitle/Audio tracks
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedSubtitle: SubtitleTrack? = null,
    val audioTracks: List<AudioTrack> = emptyList(),
    val selectedAudio: AudioTrack? = null,
    val showTrackSelector: Boolean = false,
    // Error recovery
    val playerError: PlayerError? = null,
)

data class PlayerError(
    val message: String,
    val retryCount: Int,
    val canRetry: Boolean,
)

sealed class PlayerIntent {
    data class Initialize(
        val mediaId: String,
        val streamUrl: String,
        val title: String,
        val episodeId: String?,
        val imdbId: String? = null,
        val torrentId: String? = null,
    ) : PlayerIntent()

    data object Play : PlayerIntent()
    data object Pause : PlayerIntent()
    data class Seek(val positionMs: Long) : PlayerIntent()
    data object SkipForward : PlayerIntent()
    data object SkipBackward : PlayerIntent()
    data class UpdatePosition(val positionMs: Long, val durationMs: Long) : PlayerIntent()
    data object ToggleControls : PlayerIntent()
    // Track selection
    data class SetSubtitleTrack(val track: SubtitleTrack?) : PlayerIntent()
    data class SetAudioTrack(val track: AudioTrack) : PlayerIntent()
    data object ShowTrackSelector : PlayerIntent()
    data object DismissTrackSelector : PlayerIntent()
    // Error handling
    data class OnPlayerError(val message: String) : PlayerIntent()
    // Track discovery from ExoPlayer
    data class UpdateTracks(
        val audioTracks: List<AudioTrack>,
        val subtitleTracks: List<SubtitleTrack>,
    ) : PlayerIntent()
}
