package studios.drible.tocabonito.feature.mylist

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
import studios.drible.tocabonito.core.testing.FakeFavoritesRepository
import studios.drible.tocabonito.core.testing.TestFixtures

@OptIn(ExperimentalCoroutinesApi::class)
class MyListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val favoritesRepo = FakeFavoritesRepository()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = MyListViewModel(favoritesRepository = favoritesRepo)

    @Test
    fun `empty state when no favorites`() = runTest {
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<MyListUiState.Empty>()
        }
    }

    @Test
    fun `content state when favorites exist`() = runTest {
        val item = TestFixtures.mediaItem(id = "tt001")
        favoritesRepo.add(item)

        val vm = createViewModel()
        vm.state.test {
            // stateIn emits the initialValue (Empty) first, then the upstream value
            val first = awaitItem()
            val resolved = if (first is MyListUiState.Empty) awaitItem() else first
            resolved.shouldBeInstanceOf<MyListUiState.Content>()
            (resolved as MyListUiState.Content).items.size shouldBe 1
            resolved.items.first().id shouldBe "tt001"
        }
    }

    @Test
    fun `adding a favorite updates state immediately`() = runTest {
        val vm = createViewModel()
        vm.state.test {
            awaitItem().shouldBeInstanceOf<MyListUiState.Empty>()

            val item = TestFixtures.mediaItem(id = "tt002")
            favoritesRepo.add(item)

            val updated = awaitItem()
            updated.shouldBeInstanceOf<MyListUiState.Content>()
            (updated as MyListUiState.Content).items.size shouldBe 1
            updated.items.first().id shouldBe "tt002"
        }
    }
}
