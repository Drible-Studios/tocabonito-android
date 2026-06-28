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
import studios.drible.tocabonito.core.testing.FakeProgressRepository
import studios.drible.tocabonito.core.testing.FakeStreamRepository
import studios.drible.tocabonito.core.testing.FakeSubtitleRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressSyncTimingTest {

    @BeforeEach
    fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel(
        progressRepository: FakeProgressRepository = FakeProgressRepository(),
    ) = PlayerViewModel(
        savedStateHandle = SavedStateHandle(),
        progressRepository = progressRepository,
        subtitleRepository = FakeSubtitleRepository(),
        streamRepository = FakeStreamRepository(),
    )

    @Test
    fun `saves progress on pause`() = runTest {
        val repo = FakeProgressRepository()
        val vm = createViewModel(repo)

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = null,
        ))
        vm.onIntent(PlayerIntent.UpdatePosition(60_000, 7_200_000))
        vm.onIntent(PlayerIntent.Pause)

        val saved = repo.get("tt999", null)
        saved shouldNotBe null
        saved!!.currentTime shouldBe 60.0
        saved.duration shouldBe 7200.0
    }

    @Test
    fun `does not save if duration is zero`() = runTest {
        val repo = FakeProgressRepository()
        val vm = createViewModel(repo)

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = null,
        ))
        vm.onIntent(PlayerIntent.UpdatePosition(60_000, 0))
        vm.onIntent(PlayerIntent.Pause)

        repo.get("tt999", null) shouldBe null
    }

    @Test
    fun `play starts periodic save and pause stops it`() = runTest {
        val repo = FakeProgressRepository()
        val vm = createViewModel(repo)

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = null,
        ))
        vm.onIntent(PlayerIntent.UpdatePosition(60_000, 7_200_000))

        // Play starts periodic save job
        vm.onIntent(PlayerIntent.Play)
        vm.state.value.isPlaying shouldBe true

        // Immediately pause — should save + cancel periodic
        vm.onIntent(PlayerIntent.Pause)
        vm.state.value.isPlaying shouldBe false

        val saved = repo.get("tt999", null)
        saved shouldNotBe null
        saved!!.currentTime shouldBe 60.0
    }

    @Test
    fun `saves on position update when paused`() = runTest {
        val repo = FakeProgressRepository()
        val vm = createViewModel(repo)

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = null,
        ))
        // When not playing, UpdatePosition also triggers save
        vm.onIntent(PlayerIntent.UpdatePosition(120_000, 7_200_000))

        val saved = repo.get("tt999", null)
        saved shouldNotBe null
        saved!!.currentTime shouldBe 120.0
    }
}
