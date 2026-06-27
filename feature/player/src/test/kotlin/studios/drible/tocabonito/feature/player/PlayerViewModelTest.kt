package studios.drible.tocabonito.feature.player

import androidx.lifecycle.SavedStateHandle
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.testing.FakeProgressRepository

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        progressRepository: FakeProgressRepository = FakeProgressRepository(),
    ) = PlayerViewModel(
        savedStateHandle = SavedStateHandle(),
        progressRepository = progressRepository,
    )

    @Test
    fun `saves progress on pause`() = runTest {
        val repo = FakeProgressRepository()
        val vm = createViewModel(repo)

        vm.onIntent(
            PlayerIntent.Initialize(
                mediaId = "tt123",
                streamUrl = "http://example.com/movie.mkv",
                title = "Movie",
                episodeId = null,
            ),
        )
        vm.onIntent(PlayerIntent.UpdatePosition(300_000, 7_200_000))
        vm.onIntent(PlayerIntent.Pause)

        advanceUntilIdle()

        val saved = repo.get("tt123", null)
        saved shouldNotBe null
        saved!!.currentTime shouldBe 300.0
        saved.duration shouldBe 7200.0
    }

    @Test
    fun `skip forward adds 10 seconds`() = runTest {
        val vm = createViewModel()

        vm.onIntent(
            PlayerIntent.Initialize("tt123", "http://example.com/movie.mkv", "Movie", null),
        )
        vm.onIntent(PlayerIntent.UpdatePosition(60_000, 3_600_000))
        vm.onIntent(PlayerIntent.SkipForward)

        vm.state.value.currentPositionMs shouldBe 70_000
    }

    @Test
    fun `skip backward subtracts 10 seconds, floor is 0`() = runTest {
        val vm = createViewModel()

        vm.onIntent(
            PlayerIntent.Initialize("tt123", "http://example.com/movie.mkv", "Movie", null),
        )
        vm.onIntent(PlayerIntent.UpdatePosition(5_000, 3_600_000))
        vm.onIntent(PlayerIntent.SkipBackward)

        vm.state.value.currentPositionMs shouldBe 0
    }

    @Test
    fun `initialize sets resume position from repository`() = runTest {
        val repo = FakeProgressRepository()
        val vm = createViewModel(repo)

        // Pre-seed progress in repository
        vm.onIntent(
            PlayerIntent.Initialize("tt123", "http://example.com/movie.mkv", "Movie", null),
        )
        vm.onIntent(PlayerIntent.UpdatePosition(1_800_000, 7_200_000))
        vm.onIntent(PlayerIntent.Pause)
        advanceUntilIdle()

        // Create fresh VM — it should load resumePositionMs from repo
        val vm2 = createViewModel(repo)
        vm2.onIntent(
            PlayerIntent.Initialize("tt123", "http://example.com/movie.mkv", "Movie", null),
        )
        advanceUntilIdle()

        vm2.state.value.resumePositionMs shouldBe 1_800_000L
    }

    @Test
    fun `toggle controls inverts showControls`() = runTest {
        val vm = createViewModel()
        vm.state.value.showControls shouldBe true

        vm.onIntent(PlayerIntent.ToggleControls)
        vm.state.value.showControls shouldBe false

        vm.onIntent(PlayerIntent.ToggleControls)
        vm.state.value.showControls shouldBe true
    }
}
