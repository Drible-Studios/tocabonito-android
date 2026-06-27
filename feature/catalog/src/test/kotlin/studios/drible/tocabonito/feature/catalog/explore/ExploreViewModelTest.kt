package studios.drible.tocabonito.feature.catalog.explore

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
import studios.drible.tocabonito.core.domain.model.Genre
import studios.drible.tocabonito.core.domain.model.YearFilter
import studios.drible.tocabonito.core.testing.FakeCatalogRepository
import studios.drible.tocabonito.core.testing.TestFixtures

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val catalogRepo = FakeCatalogRepository()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ExploreViewModel(catalogRepo)

    @Test
    fun `initial state is loading then success with genres`() = runTest {
        catalogRepo.genresResult = listOf(Genre(28, "Action"), Genre(35, "Comedy"))
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<ExploreUiState.Loading>()
            val success = awaitItem()
            success.shouldBeInstanceOf<ExploreUiState.Success>()
            (success as ExploreUiState.Success).genres.size shouldBe 2
            (success).selectedGenre shouldBe null
        }
    }

    @Test
    fun `error state on repository failure`() = runTest {
        catalogRepo.shouldThrow = RuntimeException("Network error")
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<ExploreUiState.Loading>()
            awaitItem().shouldBeInstanceOf<ExploreUiState.Error>()
        }
    }

    @Test
    fun `selecting genre triggers discover and updates items`() = runTest {
        val genre = Genre(28, "Action")
        catalogRepo.genresResult = listOf(genre)
        catalogRepo.discoverResult = listOf(TestFixtures.mediaItem())
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<ExploreUiState.Loading>()
            val afterGenres = awaitItem()
            afterGenres.shouldBeInstanceOf<ExploreUiState.Success>()

            vm.onIntent(ExploreIntent.SelectGenre(genre))

            val afterSelect = awaitItem()
            afterSelect.shouldBeInstanceOf<ExploreUiState.Success>()
            (afterSelect as ExploreUiState.Success).selectedGenre shouldBe genre
            (afterSelect).items shouldBe emptyList()

            val afterDiscover = awaitItem()
            afterDiscover.shouldBeInstanceOf<ExploreUiState.Success>()
            (afterDiscover as ExploreUiState.Success).items.size shouldBe 1
        }
    }

    @Test
    fun `selecting year filter re-fetches from page 1`() = runTest {
        val genre = Genre(28, "Action")
        catalogRepo.genresResult = listOf(genre)
        catalogRepo.discoverResult = listOf(TestFixtures.mediaItem())
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<ExploreUiState.Loading>()
            awaitItem().shouldBeInstanceOf<ExploreUiState.Success>()

            vm.onIntent(ExploreIntent.SelectGenre(genre))
            awaitItem() // selectedGenre set, items cleared
            awaitItem() // discover results loaded

            catalogRepo.discoverResult = listOf(TestFixtures.mediaItem(id = "tt9999999"))
            vm.onIntent(ExploreIntent.SelectYear(YearFilter.Y2025))

            val afterYear = awaitItem()
            afterYear.shouldBeInstanceOf<ExploreUiState.Success>()
            (afterYear as ExploreUiState.Success).selectedYear shouldBe YearFilter.Y2025
            (afterYear).currentPage shouldBe 1

            val afterRefetch = awaitItem()
            afterRefetch.shouldBeInstanceOf<ExploreUiState.Success>()
            (afterRefetch as ExploreUiState.Success).items.size shouldBe 1
            (afterRefetch).items.first().id shouldBe "tt9999999"
        }
    }

    @Test
    fun `load more appends results and increments page`() = runTest {
        val genre = Genre(28, "Action")
        catalogRepo.genresResult = listOf(genre)
        catalogRepo.discoverResult = listOf(TestFixtures.mediaItem(id = "tt0000001"))
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<ExploreUiState.Loading>()
            awaitItem().shouldBeInstanceOf<ExploreUiState.Success>()

            vm.onIntent(ExploreIntent.SelectGenre(genre))
            awaitItem()
            awaitItem() // page 1 loaded with 1 item

            catalogRepo.discoverResult = listOf(TestFixtures.mediaItem(id = "tt0000002"))
            vm.onIntent(ExploreIntent.LoadMore)

            val loadingMore = awaitItem()
            loadingMore.shouldBeInstanceOf<ExploreUiState.Success>()
            (loadingMore as ExploreUiState.Success).isLoadingMore shouldBe true

            val afterMore = awaitItem()
            afterMore.shouldBeInstanceOf<ExploreUiState.Success>()
            (afterMore as ExploreUiState.Success).items.size shouldBe 2
            (afterMore).currentPage shouldBe 2
            (afterMore).isLoadingMore shouldBe false
        }
    }

    @Test
    fun `load more is ignored when no more pages`() = runTest {
        val genre = Genre(28, "Action")
        catalogRepo.genresResult = listOf(genre)
        catalogRepo.discoverResult = emptyList()
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<ExploreUiState.Loading>()
            awaitItem().shouldBeInstanceOf<ExploreUiState.Success>()

            vm.onIntent(ExploreIntent.SelectGenre(genre))
            awaitItem()
            val afterDiscover = awaitItem()
            afterDiscover.shouldBeInstanceOf<ExploreUiState.Success>()
            (afterDiscover as ExploreUiState.Success).hasMorePages shouldBe false

            vm.onIntent(ExploreIntent.LoadMore)
            // No new state emitted since hasMorePages is false
            expectNoEvents()
        }
    }
}
