package studios.drible.tocabonito.feature.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import android.view.SurfaceView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import studios.drible.tocabonito.core.domain.model.AudioTrack
import studios.drible.tocabonito.core.domain.model.SubtitleTrack

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
                // Don't prepare here — surface must be set first via AndroidView.factory
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

            override fun onPlayerError(error: PlaybackException) {
                viewModel.onIntent(
                    PlayerIntent.OnPlayerError(error.localizedMessage ?: "Playback error"),
                )
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audioTracks = tracks.groups
                    .filter { it.type == C.TRACK_TYPE_AUDIO }
                    .flatMapIndexed { groupIdx, group ->
                        (0 until group.length).map { trackIdx ->
                            val format = group.getTrackFormat(trackIdx)
                            AudioTrack(
                                index = groupIdx * 100 + trackIdx,
                                name = format.label ?: format.language ?: "Track ${trackIdx + 1}",
                                languageCode = format.language,
                            )
                        }
                    }
                val subtitleTracks = tracks.groups
                    .filter { it.type == C.TRACK_TYPE_TEXT }
                    .flatMapIndexed { groupIdx, group ->
                        (0 until group.length).map { trackIdx ->
                            val format = group.getTrackFormat(trackIdx)
                            SubtitleTrack(
                                index = groupIdx * 100 + trackIdx,
                                name = format.label ?: format.language ?: "Subtitle ${trackIdx + 1}",
                                languageCode = format.language,
                                codec = format.codecs,
                            )
                        }
                    }
                viewModel.onIntent(PlayerIntent.UpdateTracks(audioTracks, subtitleTracks))
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
        // Video surface — set before prepare() so decoder has a valid surface
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).also { surfaceView ->
                    player.setVideoSurfaceView(surfaceView)
                    if (state.streamUrl.isNotEmpty()) {
                        player.prepare()
                        player.playWhenReady = true
                    }
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

                // Track selector button (top-right)
                if (state.audioTracks.isNotEmpty() || state.subtitleTracks.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.onIntent(PlayerIntent.ShowTrackSelector) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Track selector",
                            tint = Color.White,
                        )
                    }
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

        // Error overlay
        state.playerError?.let { error ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Playback Error",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = error.message,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }
        }

        // Track selector bottom sheet
        if (state.showTrackSelector) {
            TrackSelectorSheet(
                state = state,
                onSelectAudio = { viewModel.onIntent(PlayerIntent.SetAudioTrack(it)) },
                onSelectSubtitle = { viewModel.onIntent(PlayerIntent.SetSubtitleTrack(it)) },
                onDismiss = { viewModel.onIntent(PlayerIntent.DismissTrackSelector) },
            )
        }
    }
}
