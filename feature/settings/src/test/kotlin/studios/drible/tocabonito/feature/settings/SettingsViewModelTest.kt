package studios.drible.tocabonito.feature.settings

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.data.api.realdebrid.RealDebridClient
import studios.drible.tocabonito.core.data.preferences.ApiKeyStore
import studios.drible.tocabonito.core.data.preferences.TorrentioPreferences
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.CloudAccountStatus
import studios.drible.tocabonito.core.domain.service.DataPortabilityService
import studios.drible.tocabonito.core.domain.service.ImportResult
import studios.drible.tocabonito.core.domain.service.PortableBackup
import studios.drible.tocabonito.core.domain.service.SyncStatus
import studios.drible.tocabonito.core.domain.service.SyncStatusProvider
import studios.drible.tocabonito.core.ui.theme.ThemeProvider
import studios.drible.tocabonito.core.ui.theme.ThemeStore

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockHttpClient(response: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(response, status, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    private fun createViewModel(
        httpClient: HttpClient = mockHttpClient("{}"),
    ): SettingsViewModel {
        return SettingsViewModel(
            themeProvider = ThemeProvider(FakeThemeStore()),
            syncStatusProvider = FakeSyncStatusProvider(),
            cloudAccountProvider = FakeCloudAccountProvider(),
            dataPortabilityService = FakeDataPortabilityService(),
            apiKeyStore = ApiKeyStore(FakeDataStore()),
            torrentioPreferences = TorrentioPreferences(FakeDataStore()),
            realDebridClient = RealDebridClient(httpClient, "test-token"),
        )
    }

    @Test
    fun `apiValidationState starts as Idle`() = runTest {
        val vm = createViewModel()
        vm.apiValidationState.test {
            awaitItem().shouldBeInstanceOf<ApiValidationState.Idle>()
        }
    }

    @Test
    fun `validateApiKey sets Success on valid response`() = runTest {
        val json = """{"id":1,"username":"testuser","email":"test@test.com","points":100,"locale":"en","avatar":"","type":"premium","premium":1234567890,"expiration":"2025-12-31"}"""
        val vm = createViewModel(httpClient = mockHttpClient(json))

        vm.validateApiKey("valid-key")
        advanceUntilIdle()

        vm.apiValidationState.test {
            var state = awaitItem()
            if (state is ApiValidationState.Idle || state is ApiValidationState.Loading) {
                state = awaitItem()
            }
            if (state is ApiValidationState.Loading) {
                state = awaitItem()
            }
            state.shouldBeInstanceOf<ApiValidationState.Success>()
            state.user.username shouldBe "testuser"
        }
    }

    @Test
    fun `validateApiKey sets Error on failure`() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { error("Network error") }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val vm = createViewModel(httpClient = client)

        vm.validateApiKey("bad-key")
        advanceUntilIdle()

        vm.apiValidationState.test {
            var state = awaitItem()
            if (state is ApiValidationState.Idle || state is ApiValidationState.Loading) {
                state = awaitItem()
            }
            if (state is ApiValidationState.Loading) {
                state = awaitItem()
            }
            state.shouldBeInstanceOf<ApiValidationState.Error>()
        }
    }

    @Test
    fun `clearApiKey resets validation state to Idle`() = runTest {
        val vm = createViewModel()
        vm.clearApiKey()
        advanceUntilIdle()

        vm.apiValidationState.test {
            awaitItem().shouldBeInstanceOf<ApiValidationState.Idle>()
        }
    }
}

private class FakeThemeStore : ThemeStore {
    private var value: String? = null
    override fun load(): String? = value
    override fun save(value: String) { this.value = value }
}

private class FakeSyncStatusProvider : SyncStatusProvider {
    override fun observeStatus() = flowOf(SyncStatus.Disabled)
}

private class FakeCloudAccountProvider : CloudAccountProvider {
    override suspend fun accountStatus() = CloudAccountStatus.NO_ACCOUNT
}

private class FakeDataPortabilityService : DataPortabilityService {
    override suspend fun exportAll() = PortableBackup(
        version = 1,
        exportedAt = 0L,
        sourceProvider = "test",
        favorites = emptyList(),
        watchProgress = emptyList(),
        settings = emptyMap(),
    )
    override suspend fun importAll(backup: PortableBackup) = ImportResult(0, 0, 0, 0)
}
