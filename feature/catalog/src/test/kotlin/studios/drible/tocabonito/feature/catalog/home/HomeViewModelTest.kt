package studios.drible.tocabonito.feature.catalog.home

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
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<HomeUiState.Loading>()
            val success = awaitItem()
            success.shouldBeInstanceOf<HomeUiState.Success>()
            (success as HomeUiState.Success).trending.size shouldBe 1
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
