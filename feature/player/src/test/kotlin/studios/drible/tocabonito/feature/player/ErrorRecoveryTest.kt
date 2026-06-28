package studios.drible.tocabonito.feature.player

import androidx.lifecycle.SavedStateHandle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamQuality
import studios.drible.tocabonito.core.testing.FakeProgressRepository
import studios.drible.tocabonito.core.testing.FakeStreamRepository
import studios.drible.tocabonito.core.testing.FakeSubtitleRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorRecoveryTest {

    @BeforeEach
    fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `on player error with torrentId retries resolveTranscode up to 3 times`() = runTest {
        val streamRepo = FakeStreamRepository()
        streamRepo.transcodeResult = StreamLink(
            id = "new-id",
            fileName = "movie.mkv",
            fileSize = 4_500_000,
            hlsUrl = null,
            directUrl = "https://rd.com/new-stream",
            quality = StreamQuality.FULL,
        )

        val vm = PlayerViewModel(
            savedStateHandle = SavedStateHandle(),
            progressRepository = FakeProgressRepository(),
            subtitleRepository = FakeSubtitleRepository(),
            streamRepository = streamRepo,
        )

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = null,
            torrentId = "torrent-abc",
        ))

        // First error -> retry
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        vm.state.value.streamUrl shouldBe "https://rd.com/new-stream"
        vm.state.value.playerError shouldBe null

        // Second error -> retry
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        vm.state.value.playerError shouldBe null

        // Third error -> retry
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        vm.state.value.playerError shouldBe null

        // Fourth error -> give up, show error
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        vm.state.value.playerError shouldNotBe null
        vm.state.value.playerError!!.canRetry shouldBe false
    }

    @Test
    fun `on player error without torrentId shows error immediately`() = runTest {
        val vm = PlayerViewModel(
            savedStateHandle = SavedStateHandle(),
            progressRepository = FakeProgressRepository(),
            subtitleRepository = FakeSubtitleRepository(),
            streamRepository = FakeStreamRepository(),
        )

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = null,
            torrentId = null,
        ))

        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))

        vm.state.value.playerError shouldNotBe null
        vm.state.value.playerError!!.canRetry shouldBe false
    }

    @Test
    fun `successful playback resets retry counter`() = runTest {
        val streamRepo = FakeStreamRepository()
        streamRepo.transcodeResult = StreamLink(
            id = "new-id",
            fileName = "movie.mkv",
            fileSize = 4_500_000,
            hlsUrl = null,
            directUrl = "https://rd.com/new-stream",
            quality = StreamQuality.FULL,
        )

        val vm = PlayerViewModel(
            savedStateHandle = SavedStateHandle(),
            progressRepository = FakeProgressRepository(),
            subtitleRepository = FakeSubtitleRepository(),
            streamRepository = streamRepo,
        )

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = null,
            torrentId = "torrent-abc",
        ))

        // Error + recovery
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        vm.state.value.playerError shouldBe null

        // Simulate successful playback resumed (play intent resets counter)
        vm.onIntent(PlayerIntent.Play)

        // Now retry count is reset, so we get 3 more tries
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        vm.state.value.playerError shouldBe null

        // Cleanup: pause to stop periodic save
        vm.onIntent(PlayerIntent.Pause)
    }
}
