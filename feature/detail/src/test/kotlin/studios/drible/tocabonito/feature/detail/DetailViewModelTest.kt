package studios.drible.tocabonito.feature.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.testing.FakeCatalogRepository
import studios.drible.tocabonito.core.testing.FakeFavoritesRepository
import studios.drible.tocabonito.core.testing.FakeStreamRepository
import studios.drible.tocabonito.core.testing.TestFixtures
import studios.drible.tocabonito.feature.detail.model.StreamFilters

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val catalogRepo = FakeCatalogRepository()
    private val streamRepo = FakeStreamRepository()
    private val favoritesRepo = FakeFavoritesRepository()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        mediaId: String = "tt123",
        mediaType: String = "movie",
    ) = DetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("mediaId" to mediaId, "mediaType" to mediaType)),
        catalogRepository = catalogRepo,
        streamRepository = streamRepo,
        favoritesRepository = favoritesRepo,
    )

    @Test
    fun `loads details and streams on init - Success state`() = runTest {
        catalogRepo.detailsResult = TestFixtures.mediaItem(id = "tt123")
        streamRepo.streamsResult = listOf(TestFixtures.streamOption())

        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<DetailUiState.Loading>()
            // Intermediate: details loaded, streams loading
            val intermediate = awaitItem()
            intermediate.shouldBeInstanceOf<DetailUiState.Success>()
            (intermediate as DetailUiState.Success).isLoadingStreams shouldBe true

            // Final: streams loaded
            val success = awaitItem()
            success.shouldBeInstanceOf<DetailUiState.Success>()
            (success as DetailUiState.Success).apply {
                mediaItem.id shouldBe "tt123"
                streams.size shouldBe 1
                isLoadingStreams shouldBe false
            }
        }
    }

    @Test
    fun `error state when details load fails`() = runTest {
        catalogRepo.shouldThrow = RuntimeException("Network error")

        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<DetailUiState.Loading>()
            val error = awaitItem()
            error.shouldBeInstanceOf<DetailUiState.Error>()
            (error as DetailUiState.Error).message shouldBe "Network error"
        }
    }

    @Test
    fun `toggle favorite adds item when not favorite`() = runTest {
        val item = TestFixtures.mediaItem(id = "tt123")
        catalogRepo.detailsResult = item
        streamRepo.streamsResult = emptyList()

        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<DetailUiState.Loading>()
            // skip intermediate states until stable success
            var lastSuccess: DetailUiState.Success? = null
            while (lastSuccess == null || lastSuccess.isLoadingStreams) {
                val s = awaitItem()
                if (s is DetailUiState.Success) lastSuccess = s
            }
            lastSuccess!!.isFavorite shouldBe false

            vm.onIntent(DetailIntent.ToggleFavorite)
            val afterToggle = awaitItem()
            afterToggle.shouldBeInstanceOf<DetailUiState.Success>()
            (afterToggle as DetailUiState.Success).isFavorite shouldBe true
        }
    }

    @Test
    fun `toggle favorite removes item when already favorite`() = runTest {
        val item = TestFixtures.mediaItem(id = "tt123")
        catalogRepo.detailsResult = item
        streamRepo.streamsResult = emptyList()
        favoritesRepo.add(item)

        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<DetailUiState.Loading>()
            // skip intermediate states until stable success with isFavorite = true
            var lastSuccess: DetailUiState.Success? = null
            while (lastSuccess == null || lastSuccess.isLoadingStreams || !lastSuccess.isFavorite) {
                val s = awaitItem()
                if (s is DetailUiState.Success) lastSuccess = s
            }
            lastSuccess!!.isFavorite shouldBe true

            vm.onIntent(DetailIntent.ToggleFavorite)
            val afterToggle = awaitItem()
            afterToggle.shouldBeInstanceOf<DetailUiState.Success>()
            (afterToggle as DetailUiState.Success).isFavorite shouldBe false
        }
    }

    @Test
    fun `resolve stream transitions isResolvingStream then populates resolvedLink`() = runTest {
        val item = TestFixtures.mediaItem(id = "tt123")
        val option = TestFixtures.streamOption()
        val link = TestFixtures.streamLink()
        catalogRepo.detailsResult = item
        streamRepo.streamsResult = listOf(option)
        streamRepo.resolveResult = link

        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<DetailUiState.Loading>()
            // consume states until streams loaded
            var lastSuccess: DetailUiState.Success? = null
            while (lastSuccess == null || lastSuccess.isLoadingStreams) {
                val s = awaitItem()
                if (s is DetailUiState.Success) lastSuccess = s
            }

            vm.onIntent(DetailIntent.ResolveStream(option))

            val resolving = awaitItem()
            resolving.shouldBeInstanceOf<DetailUiState.Success>()
            (resolving as DetailUiState.Success).isResolvingStream shouldBe true

            val resolved = awaitItem()
            resolved.shouldBeInstanceOf<DetailUiState.Success>()
            (resolved as DetailUiState.Success).apply {
                isResolvingStream shouldBe false
                resolvedLink shouldBe link
            }
        }
    }

    @Test
    fun `dismiss resolved link clears resolvedLink`() = runTest {
        val item = TestFixtures.mediaItem(id = "tt123")
        val option = TestFixtures.streamOption()
        val link = TestFixtures.streamLink()
        catalogRepo.detailsResult = item
        streamRepo.streamsResult = listOf(option)
        streamRepo.resolveResult = link

        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<DetailUiState.Loading>()
            var lastSuccess: DetailUiState.Success? = null
            while (lastSuccess == null || lastSuccess.isLoadingStreams) {
                val s = awaitItem()
                if (s is DetailUiState.Success) lastSuccess = s
            }

            vm.onIntent(DetailIntent.ResolveStream(option))
            awaitItem() // resolving
            awaitItem() // resolved

            vm.onIntent(DetailIntent.DismissResolvedLink)
            val dismissed = awaitItem()
            dismissed.shouldBeInstanceOf<DetailUiState.Success>()
            (dismissed as DetailUiState.Success).resolvedLink shouldBe null
        }
    }

    @Test
    fun `select episode re-fetches streams for that season and episode`() = runTest {
        val item = TestFixtures.mediaItem(id = "tt123", mediaType = studios.drible.tocabonito.core.domain.model.MediaType.SERIES)
        catalogRepo.detailsResult = item
        streamRepo.streamsResult = listOf(TestFixtures.streamOption(quality = "720p"))

        val vm = createViewModel(mediaType = "series")
        vm.state.test {
            awaitItem().shouldBeInstanceOf<DetailUiState.Loading>()
            var lastSuccess: DetailUiState.Success? = null
            while (lastSuccess == null || lastSuccess.isLoadingStreams) {
                val s = awaitItem()
                if (s is DetailUiState.Success) lastSuccess = s
            }

            streamRepo.streamsResult = listOf(TestFixtures.streamOption(quality = "1080p"))
            vm.onIntent(DetailIntent.SelectEpisode(season = 2, episode = 3))

            val loading = awaitItem()
            loading.shouldBeInstanceOf<DetailUiState.Success>()
            (loading as DetailUiState.Success).isLoadingStreams shouldBe true

            val updated = awaitItem()
            updated.shouldBeInstanceOf<DetailUiState.Success>()
            (updated as DetailUiState.Success).apply {
                isLoadingStreams shouldBe false
                streams.first().quality shouldBe "1080p"
            }
        }
    }

    @Test
    fun `apply quality filter narrows stream list`() = runTest {
        val item = TestFixtures.mediaItem(id = "tt123")
        catalogRepo.detailsResult = item
        streamRepo.streamsResult = listOf(
            TestFixtures.streamOption(quality = "1080p", source = "BluRay"),
            TestFixtures.streamOption(quality = "720p", source = "WebDL"),
            TestFixtures.streamOption(quality = "4K", source = "BluRay"),
        )

        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<DetailUiState.Loading>()
            var lastSuccess: DetailUiState.Success? = null
            while (lastSuccess == null || lastSuccess.isLoadingStreams) {
                val s = awaitItem()
                if (s is DetailUiState.Success) lastSuccess = s
            }
            lastSuccess!!.filteredStreams.size shouldBe 3

            vm.onIntent(DetailIntent.UpdateFilters(StreamFilters(quality = "1080p")))
            val filtered = awaitItem()
            filtered.shouldBeInstanceOf<DetailUiState.Success>()
            (filtered as DetailUiState.Success).apply {
                filteredStreams.size shouldBe 1
                filteredStreams.first().quality shouldBe "1080p"
            }
        }
    }

    @Test
    fun `apply source filter narrows stream list`() = runTest {
        val item = TestFixtures.mediaItem(id = "tt123")
        catalogRepo.detailsResult = item
        streamRepo.streamsResult = listOf(
            TestFixtures.streamOption(quality = "1080p", source = "BluRay"),
            TestFixtures.streamOption(quality = "720p", source = "WebDL"),
            TestFixtures.streamOption(quality = "4K", source = "BluRay"),
        )

        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<DetailUiState.Loading>()
            var lastSuccess: DetailUiState.Success? = null
            while (lastSuccess == null || lastSuccess.isLoadingStreams) {
                val s = awaitItem()
                if (s is DetailUiState.Success) lastSuccess = s
            }

            vm.onIntent(DetailIntent.UpdateFilters(StreamFilters(source = "WebDL")))
            val filtered = awaitItem()
            filtered.shouldBeInstanceOf<DetailUiState.Success>()
            (filtered as DetailUiState.Success).apply {
                filteredStreams.size shouldBe 1
                filteredStreams.first().quality shouldBe "720p"
            }
        }
    }

    @Test
    fun `clear filters restores full stream list`() = runTest {
        val item = TestFixtures.mediaItem(id = "tt123")
        catalogRepo.detailsResult = item
        streamRepo.streamsResult = listOf(
            TestFixtures.streamOption(quality = "1080p", source = "BluRay"),
            TestFixtures.streamOption(quality = "720p", source = "WebDL"),
        )

        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<DetailUiState.Loading>()
            var lastSuccess: DetailUiState.Success? = null
            while (lastSuccess == null || lastSuccess.isLoadingStreams) {
                val s = awaitItem()
                if (s is DetailUiState.Success) lastSuccess = s
            }

            vm.onIntent(DetailIntent.UpdateFilters(StreamFilters(quality = "1080p")))
            val filtered = awaitItem()
            (filtered as DetailUiState.Success).filteredStreams.size shouldBe 1

            vm.onIntent(DetailIntent.UpdateFilters(StreamFilters.EMPTY))
            val cleared = awaitItem()
            cleared.shouldBeInstanceOf<DetailUiState.Success>()
            (cleared as DetailUiState.Success).filteredStreams.size shouldBe 2
        }
    }
}
