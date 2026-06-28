package studios.drible.tocabonito.feature.catalog.home

import app.cash.turbine.test
import io.kotest.matchers.collections.shouldNotBeEmpty
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
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.testing.FakeCatalogRepository
import studios.drible.tocabonito.core.testing.FakeProgressRepository
import studios.drible.tocabonito.core.testing.TestFixtures

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val catalogRepo = FakeCatalogRepository()
    private val progressRepo = FakeProgressRepository()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(catalogRepo, progressRepo)

    @Test
    fun `initial state is loading then success`() = runTest {
        catalogRepo.trendingResult = listOf(TestFixtures.mediaItem())
        catalogRepo.popularResult = listOf(TestFixtures.mediaItem(id = "pop1"))
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<HomeUiState.Loading>()
            val success = awaitItem()
            success.shouldBeInstanceOf<HomeUiState.Success>()
            (success as HomeUiState.Success).trending.size shouldBe 1
        }
    }

    @Test
    fun `load populates heroItem from first trending item`() = runTest {
        val hero = TestFixtures.mediaItem(id = "hero1", title = "Hero Movie")
        catalogRepo.trendingResult = listOf(hero, TestFixtures.mediaItem(id = "other"))
        catalogRepo.popularResult = emptyList()
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<HomeUiState.Loading>()
            val success = awaitItem() as HomeUiState.Success
            success.heroItem?.id shouldBe "hero1"
        }
    }

    @Test
    fun `load populates popularMovies and popularSeries in parallel`() = runTest {
        catalogRepo.trendingResult = listOf(TestFixtures.mediaItem())
        catalogRepo.popularResult = listOf(
            TestFixtures.mediaItem(id = "mov1", mediaType = MediaType.MOVIE),
            TestFixtures.mediaItem(id = "ser1", mediaType = MediaType.SERIES),
        )
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<HomeUiState.Loading>()
            val success = awaitItem() as HomeUiState.Success
            success.popularMovies.shouldNotBeEmpty()
            success.popularSeries.shouldNotBeEmpty()
        }
    }

    @Test
    fun `error state on repository failure`() = runTest {
        catalogRepo.shouldThrow = RuntimeException("Network error")
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<HomeUiState.Loading>()
            awaitItem().shouldBeInstanceOf<HomeUiState.Error>()
        }
    }
}
