package studios.drible.tocabonito.feature.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasSeekOnReady by remember { mutableStateOf(false) }
    var controlsHideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val player = remember(state.streamUrl) {
        ExoPlayer.Builder(context).build().also { exoPlayer ->
            if (state.streamUrl.isNotEmpty()) {
                exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(state.streamUrl)))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }
    }

    // Listen for playback state changes to seek to resume position and sync isPlaying
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !hasSeekOnReady) {
                    state.resumePositionMs?.let { resumeMs ->
                        player.seekTo(resumeMs)
                    }
                    hasSeekOnReady = true
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    viewModel.onIntent(PlayerIntent.Play)
                } else if (player.playbackState != Player.STATE_ENDED) {
                    viewModel.onIntent(PlayerIntent.Pause)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Seek when resumePositionMs is set after initialization
    LaunchedEffect(state.resumePositionMs, hasSeekOnReady) {
        if (!hasSeekOnReady && state.resumePositionMs != null &&
            player.playbackState == Player.STATE_READY
        ) {
            player.seekTo(state.resumePositionMs!!)
            hasSeekOnReady = true
        }
    }

    // Update position every second
    LaunchedEffect(player) {
        while (true) {
            delay(1_000)
            val pos = player.currentPosition
            val dur = player.duration.coerceAtLeast(0)
            viewModel.onIntent(PlayerIntent.UpdatePosition(pos, dur))
        }
    }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(state.showControls) {
        if (state.showControls) {
            controlsHideJob?.cancel()
            controlsHideJob = scope.launch {
                delay(3_000)
                viewModel.onIntent(PlayerIntent.ToggleControls)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                controlsHideJob?.cancel()
                viewModel.onIntent(PlayerIntent.ToggleControls)
                if (state.showControls) {
                    controlsHideJob = scope.launch {
                        delay(3_000)
                        viewModel.onIntent(PlayerIntent.ToggleControls)
                    }
                }
            },
    ) {
        // Video surface using AndroidView + PlayerView (classic approach; PlayerView is in media3-exoplayer)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Controls overlay
        AnimatedVisibility(
            visible = state.showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
            ) {
                // Top bar: back + title
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                ) {
                    IconButton(onClick = {
                        player.pause()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                }

                if (state.mediaTitle.isNotEmpty()) {
                    Text(
                        text = state.mediaTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 56.dp, end = 56.dp),
                    )
                }

                // Transport controls at bottom
                TransportControls(
                    state = state,
                    onPlay = { player.play() },
                    onPause = { player.pause() },
                    onSkipForward = {
                        viewModel.onIntent(PlayerIntent.SkipForward)
                        player.seekTo(state.currentPositionMs + 10_000)
                    },
                    onSkipBackward = {
                        viewModel.onIntent(PlayerIntent.SkipBackward)
                        player.seekTo((state.currentPositionMs - 10_000).coerceAtLeast(0))
                    },
                    onSeek = { posMs ->
                        viewModel.onIntent(PlayerIntent.Seek(posMs))
                        player.seekTo(posMs)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        // Buffering indicator
        if (state.isBuffering) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
