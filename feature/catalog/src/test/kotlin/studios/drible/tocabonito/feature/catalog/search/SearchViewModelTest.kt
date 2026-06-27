package studios.drible.tocabonito.feature.catalog.search

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.testing.FakeCatalogRepository
import studios.drible.tocabonito.core.testing.TestFixtures

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

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

    private fun createViewModel() = SearchViewModel(catalogRepo)

    @Test
    fun `initial state is Idle`() = runTest {
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<SearchUiState.Idle>()
            expectNoEvents()
        }
    }

    @Test
    fun `empty query keeps Idle state`() = runTest {
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<SearchUiState.Idle>()
            vm.onIntent(SearchIntent.UpdateQuery(""))
            // Empty query resets to Idle — no state change since already Idle
            expectNoEvents()
        }
    }

    @Test
    fun `query shorter than 2 chars shows Loading but does not trigger search`() = runTest {
        catalogRepo.searchResult = listOf(TestFixtures.mediaItem())
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<SearchUiState.Idle>()
            vm.onIntent(SearchIntent.UpdateQuery("a"))
            awaitItem().shouldBeInstanceOf<SearchUiState.Loading>()
            advanceUntilIdle()
            // debounce fires but filter(length >= 2) blocks it — stays Loading
            expectNoEvents()
        }
    }

    @Test
    fun `query with 2 or more chars triggers search after debounce`() = runTest {
        catalogRepo.searchResult = listOf(TestFixtures.mediaItem())
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<SearchUiState.Idle>()
            vm.onIntent(SearchIntent.UpdateQuery("avengers"))
            awaitItem().shouldBeInstanceOf<SearchUiState.Loading>()
            advanceTimeBy(300)
            advanceUntilIdle()
            val success = awaitItem()
            success.shouldBeInstanceOf<SearchUiState.Success>()
            (success as SearchUiState.Success).results.size shouldBe 1
            success.query shouldBe "avengers"
        }
    }

    @Test
    fun `rapid typing only triggers one search after debounce settles`() = runTest {
        catalogRepo.searchResult = listOf(TestFixtures.mediaItem())
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<SearchUiState.Idle>()
            vm.onIntent(SearchIntent.UpdateQuery("av"))
            awaitItem().shouldBeInstanceOf<SearchUiState.Loading>()
            advanceTimeBy(100)
            vm.onIntent(SearchIntent.UpdateQuery("ave"))
            // No state change since still Loading
            advanceTimeBy(100)
            vm.onIntent(SearchIntent.UpdateQuery("aven"))
            // No state change
            advanceTimeBy(300)
            advanceUntilIdle()
            val success = awaitItem()
            success.shouldBeInstanceOf<SearchUiState.Success>()
            (success as SearchUiState.Success).query shouldBe "aven"
        }
    }

    @Test
    fun `Clear intent resets state to Idle`() = runTest {
        catalogRepo.searchResult = listOf(TestFixtures.mediaItem())
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<SearchUiState.Idle>()
            vm.onIntent(SearchIntent.UpdateQuery("avengers"))
            awaitItem().shouldBeInstanceOf<SearchUiState.Loading>()
            advanceTimeBy(300)
            advanceUntilIdle()
            awaitItem().shouldBeInstanceOf<SearchUiState.Success>()

            vm.onIntent(SearchIntent.Clear)
            awaitItem().shouldBeInstanceOf<SearchUiState.Idle>()
        }
    }

    @Test
    fun `error state on repository failure`() = runTest {
        catalogRepo.shouldThrow = RuntimeException("Network error")
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<SearchUiState.Idle>()
            vm.onIntent(SearchIntent.UpdateQuery("batman"))
            awaitItem().shouldBeInstanceOf<SearchUiState.Loading>()
            advanceTimeBy(300)
            advanceUntilIdle()
            val error = awaitItem()
            error.shouldBeInstanceOf<SearchUiState.Error>()
            (error as SearchUiState.Error).message shouldBe "Network error"
        }
    }
}
