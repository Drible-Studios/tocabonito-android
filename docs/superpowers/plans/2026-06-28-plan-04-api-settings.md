# Task 04: ApiKeyStore + Torrentio Preferences + Settings UI

**Issue:** #20
**Branch:** `feat/api-settings`
**Package base:** `studios.drible.tocabonito`

---

## Files Overview

| Action | Path | Purpose |
|--------|------|---------|
| CREATE | `core/data/src/main/kotlin/.../preferences/ApiKeyStore.kt` | DataStore wrapper for RD API key |
| CREATE | `core/data/src/main/kotlin/.../preferences/TorrentioPreferences.kt` | DataStore wrapper for Torrentio config |
| CREATE | `core/data/src/test/kotlin/.../preferences/ApiKeyStoreTest.kt` | Unit tests for ApiKeyStore |
| CREATE | `core/data/src/test/kotlin/.../preferences/TorrentioPreferencesTest.kt` | Unit tests for TorrentioPreferences |
| MODIFY | `feature/settings/src/main/kotlin/.../SettingsViewModel.kt` | Add validation + preferences state |
| MODIFY | `feature/settings/src/main/kotlin/.../SettingsScreen.kt` | Real-Debrid + Torrentio sections |
| MODIFY | `feature/settings/build.gradle.kts` | Add coroutines-test dep for tests |
| MODIFY | `app/src/main/kotlin/.../di/DataModule.kt` | Provide DataStore + bind ApiKeyStore |
| CREATE | `feature/settings/src/test/kotlin/.../SettingsViewModelTest.kt` | ViewModel validation flow tests |

---

## Interfaces

### Consumes
- `RealDebridClient.getUser(): RDUser` (from Task 03)
- `DataStore<Preferences>` (already in `core/data` deps)
- `BuildConfig.RD_API_KEY` (gradle.properties, debug only)

### Produces
- `ApiKeyStore` — injectable source of truth for the RD API key
- `TorrentioPreferences` — injectable source of truth for Torrentio provider/language config
- Updated `SettingsViewModel` with validation and preferences events
- Updated `SettingsScreen` with Real-Debrid + Torrentio Advanced sections

---

## Step 1: ApiKeyStore (RED-GREEN-REFACTOR)

### 1.1 RED — Write failing tests

- [ ] Create test file

```kotlin
// core/data/src/test/kotlin/studios/drible/tocabonito/core/data/preferences/ApiKeyStoreTest.kt
package studios.drible.tocabonito.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File

class ApiKeyStoreTest : FunSpec({

    lateinit var dataStore: DataStore<Preferences>
    lateinit var store: ApiKeyStore

    beforeEach {
        val testDispatcher = UnconfinedTestDispatcher()
        dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(testDispatcher),
            produceFile = { File.createTempFile("test_prefs", ".preferences_pb") },
        )
        store = ApiKeyStore(dataStore)
    }

    test("apiKey emits null when no key stored") {
        store.apiKey.test {
            awaitItem().shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    test("save persists key and apiKey emits it") {
        store.save("my-api-key")
        store.apiKey.test {
            awaitItem() shouldBe "my-api-key"
            cancelAndIgnoreRemainingEvents()
        }
    }

    test("clear removes stored key") {
        store.save("my-api-key")
        store.clear()
        store.apiKey.test {
            awaitItem().shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    test("effectiveKey returns stored key when present") {
        runTest {
            store.save("stored-key")
            store.effectiveKey() shouldBe "stored-key"
        }
    }

    test("effectiveKey returns null when no stored key and not debug") {
        runTest {
            // In test environment, BuildConfig.DEBUG behaviour depends on build variant
            // This test verifies the stored-key path; BuildConfig fallback tested via integration
            store.effectiveKey().shouldBeNull()  // no stored key, BuildConfig.RD_API_KEY likely blank in test
        }
    }
})
```

### 1.2 GREEN — Implement ApiKeyStore

- [ ] Create implementation

```kotlin
// core/data/src/main/kotlin/studios/drible/tocabonito/core/data/preferences/ApiKeyStore.kt
package studios.drible.tocabonito.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import studios.drible.tocabonito.core.data.BuildConfig
import javax.inject.Inject

class ApiKeyStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val RD_KEY = stringPreferencesKey("rd_api_key")

    val apiKey: Flow<String?> = dataStore.data.map { it[RD_KEY] }

    suspend fun save(key: String) {
        dataStore.edit { it[RD_KEY] = key }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(RD_KEY) }
    }

    suspend fun effectiveKey(): String? {
        val stored = apiKey.first()
        if (stored != null) return stored
        return if (BuildConfig.DEBUG) {
            BuildConfig.RD_API_KEY.takeIf { it.isNotBlank() }
        } else {
            null
        }
    }
}
```

### 1.3 REFACTOR

- [ ] Verify tests pass: `./gradlew :core:data:test --tests "*ApiKeyStoreTest*"`
- [ ] Commit: `feat(data): add ApiKeyStore with DataStore persistence`

---

## Step 2: TorrentioPreferences (RED-GREEN-REFACTOR)

### 2.1 RED — Write failing tests

- [ ] Create test file

```kotlin
// core/data/src/test/kotlin/studios/drible/tocabonito/core/data/preferences/TorrentioPreferencesTest.kt
package studios.drible.tocabonito.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.io.File

class TorrentioPreferencesTest : FunSpec({

    lateinit var dataStore: DataStore<Preferences>
    lateinit var prefs: TorrentioPreferences

    beforeEach {
        val testDispatcher = UnconfinedTestDispatcher()
        dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(testDispatcher),
            produceFile = { File.createTempFile("test_torrentio", ".preferences_pb") },
        )
        prefs = TorrentioPreferences(dataStore)
    }

    test("providers emits default list when nothing stored") {
        prefs.providers.test {
            awaitItem() shouldBe TorrentioPreferences.DEFAULT_PROVIDERS
            cancelAndIgnoreRemainingEvents()
        }
    }

    test("language emits default 'portuguese' when nothing stored") {
        prefs.language.test {
            awaitItem() shouldBe "portuguese"
            cancelAndIgnoreRemainingEvents()
        }
    }

    test("saveProviders persists custom list") {
        val custom = listOf("yts", "eztv")
        prefs.saveProviders(custom)
        prefs.providers.test {
            awaitItem() shouldBe custom
            cancelAndIgnoreRemainingEvents()
        }
    }

    test("saveLanguage persists custom language") {
        prefs.saveLanguage("english")
        prefs.language.test {
            awaitItem() shouldBe "english"
            cancelAndIgnoreRemainingEvents()
        }
    }

    test("saveProviders with empty list emits empty") {
        prefs.saveProviders(emptyList())
        prefs.providers.test {
            awaitItem() shouldBe emptyList()
            cancelAndIgnoreRemainingEvents()
        }
    }
})
```

### 2.2 GREEN — Implement TorrentioPreferences

- [ ] Create implementation

```kotlin
// core/data/src/main/kotlin/studios/drible/tocabonito/core/data/preferences/TorrentioPreferences.kt
package studios.drible.tocabonito.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TorrentioPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val PROVIDERS_KEY = stringPreferencesKey("torrentio_providers")
    private val LANGUAGE_KEY = stringPreferencesKey("torrentio_language")

    val providers: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[PROVIDERS_KEY]?.split(",")?.filter { it.isNotBlank() }
            ?: DEFAULT_PROVIDERS
    }

    val language: Flow<String> = dataStore.data.map { prefs ->
        prefs[LANGUAGE_KEY] ?: DEFAULT_LANGUAGE
    }

    suspend fun saveProviders(list: List<String>) {
        dataStore.edit { it[PROVIDERS_KEY] = list.joinToString(",") }
    }

    suspend fun saveLanguage(lang: String) {
        dataStore.edit { it[LANGUAGE_KEY] = lang }
    }

    companion object {
        const val DEFAULT_LANGUAGE = "portuguese"
        val DEFAULT_PROVIDERS = listOf(
            "yts", "eztv", "rarbg", "1337x", "thepiratebay", "kickasstorrents", "torrentgalaxy",
        )
        val AVAILABLE_LANGUAGES = listOf(
            "portuguese", "english", "spanish", "french", "german", "italian",
        )
    }
}
```

### 2.3 REFACTOR

- [ ] Verify tests pass: `./gradlew :core:data:test --tests "*TorrentioPreferencesTest*"`
- [ ] Commit: `feat(data): add TorrentioPreferences for provider/language config`

---

## Step 3: SettingsViewModel Validation (RED-GREEN-REFACTOR)

### 3.1 RED — Write failing ViewModel tests

- [ ] Create test file

```kotlin
// feature/settings/src/test/kotlin/studios/drible/tocabonito/feature/settings/SettingsViewModelTest.kt
package studios.drible.tocabonito.feature.settings

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import studios.drible.tocabonito.core.data.api.realdebrid.RDUser
import studios.drible.tocabonito.core.data.api.realdebrid.RealDebridClient
import studios.drible.tocabonito.core.data.preferences.ApiKeyStore
import studios.drible.tocabonito.core.data.preferences.TorrentioPreferences
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.DataPortabilityService
import studios.drible.tocabonito.core.domain.service.ImportResult
import studios.drible.tocabonito.core.domain.service.PortableBackup
import studios.drible.tocabonito.core.domain.service.SyncStatus
import studios.drible.tocabonito.core.domain.service.SyncStatusProvider
import studios.drible.tocabonito.core.ui.theme.ThemeProvider

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeEach { Dispatchers.setMain(testDispatcher) }
    afterEach { Dispatchers.resetMain() }

    // --- Fakes ---

    class FakeApiKeyStore : ApiKeyStore {
        private val _apiKey = MutableStateFlow<String?>(null)
        override val apiKey: Flow<String?> = _apiKey
        override suspend fun save(key: String) { _apiKey.value = key }
        override suspend fun clear() { _apiKey.value = null }
        override suspend fun effectiveKey(): String? = _apiKey.value
    }

    class FakeRealDebridClient(
        private val shouldFail: Boolean = false,
    ) {
        suspend fun getUser(apiKey: String): RDUser {
            if (shouldFail) throw RuntimeException("Invalid API key")
            return RDUser(
                id = 1,
                username = "testuser",
                email = "test@example.com",
                premium = 1,
                type = "premium",
                expiration = "2026-12-31T00:00:00.000Z",
            )
        }
    }

    // --- Tests ---

    test("validateApiKey sets ValidationSuccess on valid key") {
        val fakeApiKeyStore = FakeApiKeyStore()
        val fakeClient = FakeRealDebridClient(shouldFail = false)
        val viewModel = createViewModel(
            apiKeyStore = fakeApiKeyStore,
            realDebridClient = fakeClient,
        )

        viewModel.apiValidationState.test {
            awaitItem() shouldBe ApiValidationState.Idle
            viewModel.validateApiKey("valid-key")
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem().shouldBeInstanceOf<ApiValidationState.Loading>()
            awaitItem().shouldBeInstanceOf<ApiValidationState.Success>()
            val success = viewModel.apiValidationState.value as ApiValidationState.Success
            success.user.username shouldBe "testuser"
            cancelAndIgnoreRemainingEvents()
        }
    }

    test("validateApiKey sets ValidationError on invalid key") {
        val fakeApiKeyStore = FakeApiKeyStore()
        val fakeClient = FakeRealDebridClient(shouldFail = true)
        val viewModel = createViewModel(
            apiKeyStore = fakeApiKeyStore,
            realDebridClient = fakeClient,
        )

        viewModel.apiValidationState.test {
            awaitItem() shouldBe ApiValidationState.Idle
            viewModel.validateApiKey("bad-key")
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem().shouldBeInstanceOf<ApiValidationState.Loading>()
            awaitItem().shouldBeInstanceOf<ApiValidationState.Error>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    test("saveApiKey persists to ApiKeyStore") {
        val fakeApiKeyStore = FakeApiKeyStore()
        val viewModel = createViewModel(apiKeyStore = fakeApiKeyStore)

        viewModel.saveApiKey("persisted-key")
        testDispatcher.scheduler.advanceUntilIdle()

        fakeApiKeyStore.effectiveKey() shouldBe "persisted-key"
    }

    test("clearApiKey removes from ApiKeyStore and resets validation") {
        val fakeApiKeyStore = FakeApiKeyStore()
        fakeApiKeyStore.save("existing-key")
        val viewModel = createViewModel(apiKeyStore = fakeApiKeyStore)

        viewModel.clearApiKey()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeApiKeyStore.effectiveKey() shouldBe null
        viewModel.apiValidationState.value shouldBe ApiValidationState.Idle
    }
})

// Helper to construct SettingsViewModel with fakes
private fun createViewModel(
    apiKeyStore: ApiKeyStore = object : ApiKeyStore {
        override val apiKey: Flow<String?> = flowOf(null)
        override suspend fun save(key: String) {}
        override suspend fun clear() {}
        override suspend fun effectiveKey(): String? = null
    },
    realDebridClient: Any? = null, // FakeRealDebridClient
    // ... other deps use stubs
): SettingsViewModel {
    // Implementation depends on final SettingsViewModel constructor
    TODO("Wire fakes into SettingsViewModel constructor")
}
```

### 3.2 GREEN — Extend SettingsViewModel

- [ ] Modify `feature/settings/src/main/kotlin/.../SettingsViewModel.kt`

```kotlin
// Add to SettingsViewModel.kt

sealed class ApiValidationState {
    data object Idle : ApiValidationState()
    data object Loading : ApiValidationState()
    data class Success(val user: RDUser) : ApiValidationState()
    data class Error(val message: String) : ApiValidationState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val themeProvider: ThemeProvider,
    private val syncStatusProvider: SyncStatusProvider,
    private val cloudAccountProvider: CloudAccountProvider,
    private val dataPortabilityService: DataPortabilityService,
    private val apiKeyStore: ApiKeyStore,
    private val realDebridClient: RealDebridClient,
    private val torrentioPreferences: TorrentioPreferences,
) : ViewModel() {

    // --- Existing fields unchanged ---

    // --- New: API key validation ---
    private val _apiValidationState = MutableStateFlow<ApiValidationState>(ApiValidationState.Idle)
    val apiValidationState: StateFlow<ApiValidationState> = _apiValidationState

    val storedApiKey: StateFlow<String?> = apiKeyStore.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // --- New: Torrentio preferences ---
    val torrentioProviders: StateFlow<List<String>> = torrentioPreferences.providers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TorrentioPreferences.DEFAULT_PROVIDERS)

    val torrentioLanguage: StateFlow<String> = torrentioPreferences.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TorrentioPreferences.DEFAULT_LANGUAGE)

    fun validateApiKey(key: String) {
        viewModelScope.launch {
            _apiValidationState.update { ApiValidationState.Loading }
            runCatching { realDebridClient.getUser(key) }
                .onSuccess { user -> _apiValidationState.update { ApiValidationState.Success(user) } }
                .onFailure { t -> _apiValidationState.update { ApiValidationState.Error(t.message ?: "Validation failed") } }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch { apiKeyStore.save(key) }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            apiKeyStore.clear()
            _apiValidationState.update { ApiValidationState.Idle }
        }
    }

    fun updateProviders(providers: List<String>) {
        viewModelScope.launch { torrentioPreferences.saveProviders(providers) }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch { torrentioPreferences.saveLanguage(language) }
    }
}
```

### 3.3 REFACTOR

- [ ] Extract `ApiKeyStore` interface for testability (the concrete class implements it)
- [ ] Verify tests pass: `./gradlew :feature:settings:test --tests "*SettingsViewModelTest*"`
- [ ] Commit: `feat(settings): add API key validation and Torrentio preferences to SettingsViewModel`

---

## Step 4: Add `getUser()` to RealDebridClient

> Note: This step assumes Task 03 already added `getUser()`. If not yet merged, add it here.

- [ ] Add response model

```kotlin
// core/data/src/main/kotlin/.../api/realdebrid/RealDebridResponses.kt (append)

@Serializable
data class RDUserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val premium: Int,
    val type: String,
    val expiration: String,
)
```

- [ ] Add domain model

```kotlin
// core/domain/src/main/kotlin/.../model/RDUser.kt
package studios.drible.tocabonito.core.domain.model

data class RDUser(
    val id: Int,
    val username: String,
    val email: String,
    val premium: Int,
    val type: String,
    val expiration: String,
)
```

- [ ] Add method to `RealDebridClient`

```kotlin
suspend fun getUser(apiKey: String? = null): RDUser {
    val token = apiKey ?: apiToken
    val response: RDUserResponse = httpClient.get("$baseUrl/user") {
        header("Authorization", "Bearer $token")
    }.body()
    return RDUser(
        id = response.id,
        username = response.username,
        email = response.email,
        premium = response.premium,
        type = response.type,
        expiration = response.expiration,
    )
}
```

- [ ] Commit: `feat(realdebrid): add getUser endpoint for API key validation`

---

## Step 5: DI Wiring

- [ ] Modify `app/src/main/kotlin/.../di/DataModule.kt`

```kotlin
// Add to DataModule.kt

@Provides
@Singleton
fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("tocabonito_settings")
    }
}

@Provides
@Singleton
fun provideApiKeyStore(dataStore: DataStore<Preferences>): ApiKeyStore =
    ApiKeyStore(dataStore)

@Provides
@Singleton
fun provideTorrentioPreferences(dataStore: DataStore<Preferences>): TorrentioPreferences =
    TorrentioPreferences(dataStore)
```

- [ ] Verify DI graph compiles: `./gradlew :app:assembleDebug`
- [ ] Commit: `feat(di): wire ApiKeyStore and TorrentioPreferences into Hilt graph`

---

## Step 6: Settings Screen UI (Real-Debrid + Torrentio Sections)

- [ ] Modify `feature/settings/src/main/kotlin/.../SettingsScreen.kt`

Add between the "Account" and "Data" sections:

```kotlin
// --- Real-Debrid section ---
Spacer(Modifier.height(16.dp))
HorizontalDivider(color = palette.textTertiary.copy(alpha = 0.2f))
Spacer(Modifier.height(16.dp))

SettingsSectionHeader(title = "Real-Debrid", palette = palette)
RealDebridSection(
    storedKey = storedApiKey,
    validationState = apiValidationState,
    onValidate = { viewModel.validateApiKey(it) },
    onSave = { viewModel.saveApiKey(it) },
    onClear = { viewModel.clearApiKey() },
    palette = palette,
)

// --- Torrentio Advanced section ---
Spacer(Modifier.height(16.dp))
HorizontalDivider(color = palette.textTertiary.copy(alpha = 0.2f))
Spacer(Modifier.height(16.dp))

SettingsSectionHeader(title = "Torrentio Advanced", palette = palette)
TorrentioSection(
    providers = torrentioProviders,
    language = torrentioLanguage,
    onProvidersChange = { viewModel.updateProviders(it) },
    onLanguageChange = { viewModel.updateLanguage(it) },
    palette = palette,
)
```

### RealDebridSection Composable

```kotlin
@Composable
private fun RealDebridSection(
    storedKey: String?,
    validationState: ApiValidationState,
    onValidate: (String) -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    palette: ThemePalette,
) {
    var keyInput by remember { mutableStateOf(storedKey ?: "") }

    OutlinedTextField(
        value = keyInput,
        onValueChange = { keyInput = it },
        label = { Text("API Key") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.textTertiary,
            focusedLabelColor = palette.accent,
            cursorColor = palette.accent,
        ),
    )

    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onValidate(keyInput) },
            enabled = keyInput.isNotBlank() && validationState !is ApiValidationState.Loading,
        ) {
            Text("Validate")
        }
        Button(
            onClick = { onSave(keyInput) },
            enabled = keyInput.isNotBlank(),
        ) {
            Text("Save")
        }
        if (storedKey != null) {
            OutlinedButton(onClick = onClear) {
                Text("Clear")
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    when (validationState) {
        is ApiValidationState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
        is ApiValidationState.Success -> {
            val user = validationState.user
            val daysRemaining = calculateDaysRemaining(user.expiration)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Username: ${user.username}", color = palette.textPrimary)
                    Text("Plan: ${user.type}", color = palette.textSecondary)
                    Text("Days remaining: $daysRemaining", color = palette.textSecondary)
                }
            }
        }
        is ApiValidationState.Error -> {
            Text(
                text = "Validation failed: ${validationState.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        else -> {}
    }
}
```

### TorrentioSection Composable

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TorrentioSection(
    providers: List<String>,
    language: String,
    onProvidersChange: (List<String>) -> Unit,
    onLanguageChange: (String) -> Unit,
    palette: ThemePalette,
) {
    Text("Providers", color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TorrentioPreferences.DEFAULT_PROVIDERS.forEach { provider ->
            val selected = provider in providers
            FilterChip(
                selected = selected,
                onClick = {
                    val updated = if (selected) providers - provider else providers + provider
                    onProvidersChange(updated)
                },
                label = { Text(provider) },
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Text("Language", color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = language.replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TorrentioPreferences.AVAILABLE_LANGUAGES.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onLanguageChange(lang)
                        expanded = false
                    },
                )
            }
        }
    }
}
```

- [ ] Commit: `feat(settings): add Real-Debrid and Torrentio Advanced UI sections`

---

## Step 7: Build Verification + Feature Flag Guard

- [ ] Modify `core/data/build.gradle.kts` — add `BuildConfig` generation:

```kotlin
android {
    namespace = "studios.drible.tocabonito.core.data"
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "RD_API_KEY", "\"${project.findProperty("RD_API_KEY") ?: ""}\"")
    }
}
```

- [ ] Full build: `./gradlew assembleDebug`
- [ ] Full test suite: `./gradlew test`
- [ ] Commit: `build(data): expose RD_API_KEY via BuildConfig for debug fallback`

---

## Verification Checklist

- [ ] `./gradlew :core:data:test` — all preferences tests green
- [ ] `./gradlew :feature:settings:test` — ViewModel tests green
- [ ] `./gradlew assembleDebug` — clean build
- [ ] Manual: Settings screen shows Real-Debrid section with masked input
- [ ] Manual: Validate button calls API and shows user card on success
- [ ] Manual: Torrentio section shows chips pre-selected with defaults
- [ ] Manual: Language dropdown works

---

## Commit Sequence

1. `feat(data): add ApiKeyStore with DataStore persistence`
2. `feat(data): add TorrentioPreferences for provider/language config`
3. `feat(realdebrid): add getUser endpoint for API key validation`
4. `feat(di): wire ApiKeyStore and TorrentioPreferences into Hilt graph`
5. `feat(settings): add API key validation and Torrentio preferences to SettingsViewModel`
6. `feat(settings): add Real-Debrid and Torrentio Advanced UI sections`
7. `build(data): expose RD_API_KEY via BuildConfig for debug fallback`
