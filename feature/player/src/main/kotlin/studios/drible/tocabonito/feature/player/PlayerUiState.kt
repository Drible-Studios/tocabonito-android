package studios.drible.tocabonito.feature.player

data class PlayerUiState(
    val streamUrl: String = "",
    val mediaTitle: String = "",
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val resumePositionMs: Long? = null,
    val showControls: Boolean = true,
    val isBuffering: Boolean = false,
)

sealed class PlayerIntent {
    data class Initialize(
        val mediaId: String,
        val streamUrl: String,
        val title: String,
        val episodeId: String?,
    ) : PlayerIntent()

    data object Play : PlayerIntent()
    data object Pause : PlayerIntent()
    data class Seek(val positionMs: Long) : PlayerIntent()
    data object SkipForward : PlayerIntent()
    data object SkipBackward : PlayerIntent()
    data class UpdatePosition(val positionMs: Long, val durationMs: Long) : PlayerIntent()
    data object ToggleControls : PlayerIntent()
}
