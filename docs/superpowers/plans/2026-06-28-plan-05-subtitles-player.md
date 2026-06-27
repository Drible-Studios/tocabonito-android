# Task 05 - OpenSubtitles + Player Enhancements

**Issues:** #16, #21  
**Branch:** `feat/subtitles-player-enhancements`

---

## Overview

Add OpenSubtitles integration for automatic subtitle fetching, audio/subtitle track selection UI, periodic progress sync, and transcode error recovery. This touches `core/data` (new API client), `core/domain` (repository interface), `feature/player` (UI + ViewModel enhancements).

---

## Files to Create

| # | Path | Purpose |
|---|------|---------|
| 1 | `core/data/src/main/kotlin/.../api/opensubtitles/OpenSubtitlesClient.kt` | HTTP client for OpenSubtitles REST v1 |
| 2 | `core/data/src/main/kotlin/.../api/opensubtitles/OpenSubtitlesResponses.kt` | Serializable response DTOs |
| 3 | `core/data/src/main/kotlin/.../repository/SubtitleRepositoryImpl.kt` | Orchestrates search + download + cache |
| 4 | `core/domain/src/main/kotlin/.../repository/SubtitleRepository.kt` | Domain interface |
| 5 | `core/data/src/test/resources/fixtures/opensubtitles/search.json` | JSON fixture for search endpoint |
| 6 | `core/data/src/test/resources/fixtures/opensubtitles/download.json` | JSON fixture for download endpoint |
| 7 | `core/data/src/test/kotlin/.../api/opensubtitles/OpenSubtitlesClientContractTest.kt` | Contract test with MockEngine |
| 8 | `core/testing/src/main/kotlin/.../FakeSubtitleRepository.kt` | Fake for player tests |
| 9 | `feature/player/src/main/kotlin/.../TrackSelectorSheet.kt` | Bottom sheet for audio/subtitle selection |
| 10 | `feature/player/src/test/kotlin/.../ProgressSyncTimingTest.kt` | Unit test for periodic save logic |
| 11 | `feature/player/src/test/kotlin/.../ErrorRecoveryTest.kt` | Unit test for retry logic |

## Files to Modify

| # | Path | Changes |
|---|------|---------|
| 1 | `feature/player/src/main/kotlin/.../PlayerViewModel.kt` | Add subtitle fetch, periodic progress sync, error recovery |
| 2 | `feature/player/src/main/kotlin/.../PlayerUiState.kt` | Add subtitle/audio track fields, error state |
| 3 | `feature/player/src/main/kotlin/.../PlayerScreen.kt` | Wire SubtitleView, track selector, MergingMediaSource |
| 4 | `feature/player/build.gradle.kts` | Add `:core:testing` testImplementation |
| 5 | `core/domain/src/main/kotlin/.../model/SubtitleTrack.kt` | Add `externalFilePath` field for cached sideloaded subs |
| 6 | `core/data/build.gradle.kts` | (no changes needed -- ktor deps already present) |

---

## Interfaces

### SubtitleRepository (domain)

```kotlin
// core/domain/src/main/kotlin/studios/drible/tocabonito/core/domain/repository/SubtitleRepository.kt
package studios.drible.tocabonito.core.domain.repository

import studios.drible.tocabonito.core.domain.model.SubtitleTrack

interface SubtitleRepository {
    /** Search + download best subtitle for an IMDB ID. Returns cached file path. */
    suspend fun fetchSubtitle(imdbId: String, language: String = "pob"): SubtitleTrack?
}
```

### OpenSubtitlesClient

```kotlin
// core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/opensubtitles/OpenSubtitlesClient.kt
package studios.drible.tocabonito.core.data.api.opensubtitles

import io.ktor.client.HttpClient

class OpenSubtitlesClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    private val baseUrl = "https://api.opensubtitles.com/api/v1"

    suspend fun search(imdbId: String, language: String = "pob"): List<SubtitleData>
    suspend fun download(fileId: Int): SubtitleDownloadResponse
}
```

### PlayerViewModel additions (sketch)

```kotlin
// New intents
sealed class PlayerIntent {
    // ... existing ...
    data class SetSubtitleTrack(val track: SubtitleTrack?) : PlayerIntent()
    data class SetAudioTrack(val track: AudioTrack) : PlayerIntent()
    data object ShowTrackSelector : PlayerIntent()
    data object DismissTrackSelector : PlayerIntent()
    data object RetryAfterError : PlayerIntent()
}

// New state fields
data class PlayerUiState(
    // ... existing ...
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedSubtitle: SubtitleTrack? = null,
    val audioTracks: List<AudioTrack> = emptyList(),
    val selectedAudio: AudioTrack? = null,
    val showTrackSelector: Boolean = false,
    val playerError: PlayerError? = null,
)

data class PlayerError(val message: String, val retryCount: Int, val canRetry: Boolean)
```

---

## TDD Steps

### Step 1: OpenSubtitlesClient contract test (RED)

**File:** `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/api/opensubtitles/OpenSubtitlesClientContractTest.kt`

```kotlin
package studios.drible.tocabonito.core.data.api.opensubtitles

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class OpenSubtitlesClientContractTest {

    private fun fixtureClient(fixturePath: String, assertRequest: ((io.ktor.client.engine.mock.MockRequestHandleScope, io.ktor.http.HttpRequestData) -> Unit)? = null): HttpClient {
        val json = javaClass.classLoader!!.getResource(fixturePath)!!.readText()
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    assertRequest?.invoke(this, request)
                    respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Test
    fun `search sends correct API key header and imdb query`() = runTest {
        var capturedHeaders: io.ktor.http.Headers? = null
        var capturedUrl: String? = null
        val client = OpenSubtitlesClient(
            fixtureClient("fixtures/opensubtitles/search.json") { _, request ->
                capturedHeaders = request.headers
                capturedUrl = request.url.toString()
            },
            apiKey = "test-api-key",
        )

        client.search("tt1234567")

        capturedHeaders!!["Api-Key"] shouldBe "test-api-key"
        capturedUrl!! shouldContain "imdb_id=tt1234567"
        capturedUrl!! shouldContain "languages=pob"
    }

    @Test
    fun `search parses response into SubtitleData list`() = runTest {
        val client = OpenSubtitlesClient(
            fixtureClient("fixtures/opensubtitles/search.json"),
            apiKey = "test-api-key",
        )

        val results = client.search("tt1234567")

        results shouldHaveSize 2
        results[0].id shouldBe "123456"
        results[0].attributes.language shouldBe "pt-BR"
        results[0].attributes.files[0].file_id shouldBe 99001
        results[0].attributes.files[0].file_name shouldBe "Movie.2024.1080p.pob.srt"
    }

    @Test
    fun `download sends POST with file_id and returns link`() = runTest {
        var capturedMethod: HttpMethod? = null
        val client = OpenSubtitlesClient(
            fixtureClient("fixtures/opensubtitles/download.json") { _, request ->
                capturedMethod = request.method
            },
            apiKey = "test-api-key",
        )

        val result = client.download(99001)

        capturedMethod shouldBe HttpMethod.Post
        result.link shouldBe "https://dl.opensubtitles.org/en/download/sub/99001"
        result.file_name shouldBe "Movie.2024.1080p.pob.srt"
    }
}
```

**Commit:** `test(data): add OpenSubtitlesClient contract test with JSON fixtures (RED)`

---

### Step 2: JSON fixtures

**File:** `core/data/src/test/resources/fixtures/opensubtitles/search.json`

```json
{
  "total_pages": 1,
  "total_count": 2,
  "page": 1,
  "data": [
    {
      "id": "123456",
      "type": "subtitle",
      "attributes": {
        "language": "pt-BR",
        "download_count": 50000,
        "hearing_impaired": false,
        "fps": 23.976,
        "files": [
          { "file_id": 99001, "file_name": "Movie.2024.1080p.pob.srt" }
        ],
        "feature_details": { "imdb_id": 1234567 }
      }
    },
    {
      "id": "123457",
      "type": "subtitle",
      "attributes": {
        "language": "pt-BR",
        "download_count": 12000,
        "hearing_impaired": true,
        "fps": 23.976,
        "files": [
          { "file_id": 99002, "file_name": "Movie.2024.1080p.pob.HI.srt" }
        ],
        "feature_details": { "imdb_id": 1234567 }
      }
    }
  ]
}
```

**File:** `core/data/src/test/resources/fixtures/opensubtitles/download.json`

```json
{
  "link": "https://dl.opensubtitles.org/en/download/sub/99001",
  "file_name": "Movie.2024.1080p.pob.srt",
  "requests": 98,
  "remaining": 2,
  "message": "Your quota will be renewed in 21 hours and 13 minutes (2024-01-01 12:00:00 UTC)",
  "reset_time": "2024-01-01T12:00:00.000Z",
  "reset_time_utc": "2024-01-01T12:00:00.000Z"
}
```

**Commit:** `test(data): add OpenSubtitles JSON fixtures`

---

### Step 3: OpenSubtitlesResponses (GREEN for DTOs)

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/opensubtitles/OpenSubtitlesResponses.kt`

```kotlin
package studios.drible.tocabonito.core.data.api.opensubtitles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubtitleSearchResponse(
    val data: List<SubtitleData>,
    @SerialName("total_count") val totalCount: Int = 0,
)

@Serializable
data class SubtitleData(
    val id: String,
    val attributes: SubtitleAttributes,
)

@Serializable
data class SubtitleAttributes(
    val language: String,
    val files: List<SubtitleFile>,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("hearing_impaired") val hearingImpaired: Boolean = false,
)

@Serializable
data class SubtitleFile(
    @SerialName("file_id") val file_id: Int,
    @SerialName("file_name") val file_name: String,
)

@Serializable
data class SubtitleDownloadResponse(
    val link: String,
    @SerialName("file_name") val file_name: String,
    val remaining: Int = 0,
)
```

**Commit:** `feat(data): add OpenSubtitles serializable response models`

---

### Step 4: OpenSubtitlesClient implementation (GREEN)

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/opensubtitles/OpenSubtitlesClient.kt`

```kotlin
package studios.drible.tocabonito.core.data.api.opensubtitles

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class OpenSubtitlesClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    private val baseUrl = "https://api.opensubtitles.com/api/v1"

    suspend fun search(imdbId: String, language: String = "pob"): List<SubtitleData> {
        val response: SubtitleSearchResponse = httpClient.get("$baseUrl/subtitles") {
            header("Api-Key", apiKey)
            parameter("imdb_id", imdbId)
            parameter("languages", language)
        }.body()
        return response.data
    }

    suspend fun download(fileId: Int): SubtitleDownloadResponse {
        return httpClient.post("$baseUrl/download") {
            header("Api-Key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(DownloadRequest(fileId))
        }.body()
    }
}

@Serializable
private data class DownloadRequest(
    @kotlinx.serialization.SerialName("file_id") val fileId: Int,
)
```

**Run tests:** `./gradlew :core:data:test --tests "*.OpenSubtitlesClientContractTest"`

**Commit:** `feat(data): implement OpenSubtitlesClient with search and download`

---

### Step 5: SubtitleRepository interface + fake (RED for integration)

**File:** `core/domain/src/main/kotlin/studios/drible/tocabonito/core/domain/repository/SubtitleRepository.kt`

```kotlin
package studios.drible.tocabonito.core.domain.repository

import studios.drible.tocabonito.core.domain.model.SubtitleTrack

interface SubtitleRepository {
    /**
     * Searches OpenSubtitles for the given IMDB ID, downloads the best match,
     * caches it locally, and returns a SubtitleTrack pointing to the cached file.
     * Returns null if no subtitles found or download fails.
     */
    suspend fun fetchSubtitle(imdbId: String, language: String = "pob"): SubtitleTrack?
}
```

**File:** `core/testing/src/main/kotlin/studios/drible/tocabonito/core/testing/FakeSubtitleRepository.kt`

```kotlin
package studios.drible.tocabonito.core.testing

import studios.drible.tocabonito.core.domain.model.SubtitleTrack
import studios.drible.tocabonito.core.domain.repository.SubtitleRepository

class FakeSubtitleRepository : SubtitleRepository {
    var subtitleToReturn: SubtitleTrack? = null
    var fetchCallCount = 0
    var lastImdbId: String? = null

    override suspend fun fetchSubtitle(imdbId: String, language: String): SubtitleTrack? {
        fetchCallCount++
        lastImdbId = imdbId
        return subtitleToReturn
    }
}
```

**Commit:** `feat(domain): add SubtitleRepository interface and fake for testing`

---

### Step 6: SubtitleRepositoryImpl (GREEN)

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/repository/SubtitleRepositoryImpl.kt`

```kotlin
package studios.drible.tocabonito.core.data.repository

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import studios.drible.tocabonito.core.data.api.opensubtitles.OpenSubtitlesClient
import studios.drible.tocabonito.core.domain.model.SubtitleTrack
import studios.drible.tocabonito.core.domain.repository.SubtitleRepository
import java.io.File
import javax.inject.Inject

class SubtitleRepositoryImpl @Inject constructor(
    private val openSubtitlesClient: OpenSubtitlesClient,
    private val downloadHttpClient: HttpClient,
    private val context: Context,
) : SubtitleRepository {

    private val cacheDir: File
        get() = File(context.cacheDir, "subtitles").also { it.mkdirs() }

    override suspend fun fetchSubtitle(imdbId: String, language: String): SubtitleTrack? {
        // Check cache first
        val cachedFile = File(cacheDir, "${imdbId}_$language.srt")
        if (cachedFile.exists()) {
            return cachedFile.toSubtitleTrack(language)
        }

        // Search
        val results = openSubtitlesClient.search(imdbId, language)
        if (results.isEmpty()) return null

        // Pick best: highest download count, non-HI preferred
        val best = results
            .sortedWith(compareBy({ it.attributes.hearingImpaired }, { -it.attributes.downloadCount }))
            .first()

        val fileId = best.attributes.files.firstOrNull()?.file_id ?: return null

        // Download link
        val downloadResponse = openSubtitlesClient.download(fileId)

        // Fetch actual subtitle file
        val channel = downloadHttpClient.get(downloadResponse.link).bodyAsChannel()
        cachedFile.outputStream().use { out ->
            channel.toInputStream().use { input -> input.copyTo(out) }
        }

        return cachedFile.toSubtitleTrack(language)
    }

    private fun File.toSubtitleTrack(language: String) = SubtitleTrack(
        index = 100, // sideloaded tracks get high index to avoid collision
        name = "Portuguese (BR)",
        languageCode = language,
        codec = "text/x-ssa", // SRT mapped to subrip by ExoPlayer
        isExternal = true,
        externalUrl = this.absolutePath,
    )
}
```

**Commit:** `feat(data): implement SubtitleRepositoryImpl with cache and download`

---

### Step 7: Periodic progress sync test (RED)

**File:** `feature/player/src/test/kotlin/studios/drible/tocabonito/feature/player/ProgressSyncTimingTest.kt`

```kotlin
package studios.drible.tocabonito.feature.player

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.testing.FakeProgressRepository
import studios.drible.tocabonito.core.testing.FakeSubtitleRepository
import androidx.lifecycle.SavedStateHandle

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressSyncTimingTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel(
        progressRepository: FakeProgressRepository = FakeProgressRepository(),
    ) = PlayerViewModel(
        savedStateHandle = SavedStateHandle(),
        progressRepository = progressRepository,
        subtitleRepository = FakeSubtitleRepository(),
        streamRepository = studios.drible.tocabonito.core.testing.FakeStreamRepository(),
    )

    @Test
    fun `saves progress every 10 seconds during playback`() = runTest {
        val repo = FakeProgressRepository()
        val vm = createViewModel(repo)

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = "tt999",
        ))
        vm.onIntent(PlayerIntent.UpdatePosition(60_000, 7_200_000))
        vm.onIntent(PlayerIntent.Play)

        // After 9 seconds -- no periodic save yet
        advanceTimeBy(9_000)
        repo.get("tt999", null)?.currentTime shouldBe null

        // After 10 seconds -- first periodic save
        advanceTimeBy(1_001)
        advanceUntilIdle()
        val saved = repo.get("tt999", null)
        saved shouldBe saved // not null assertion implied by next line
        saved!!.currentTime shouldBe 60.0
    }

    @Test
    fun `stops periodic save when paused`() = runTest {
        val repo = FakeProgressRepository()
        val vm = createViewModel(repo)

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = "tt999",
        ))
        vm.onIntent(PlayerIntent.UpdatePosition(60_000, 7_200_000))
        vm.onIntent(PlayerIntent.Play)
        advanceTimeBy(10_001)
        advanceUntilIdle()

        // Pause should save immediately
        vm.onIntent(PlayerIntent.Pause)
        advanceUntilIdle()

        // Count saves: initial periodic (1) + pause (1) = 2 total
        // After pausing, advancing time should NOT trigger more saves
        val savedBefore = repo.get("tt999", null)!!.currentTime
        advanceTimeBy(20_000)
        advanceUntilIdle()
        repo.get("tt999", null)!!.currentTime shouldBe savedBefore
    }

    @Test
    fun `saves on stop via player listener event`() = runTest {
        val repo = FakeProgressRepository()
        val vm = createViewModel(repo)

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = "tt999",
        ))
        vm.onIntent(PlayerIntent.UpdatePosition(120_000, 7_200_000))
        vm.onIntent(PlayerIntent.Pause) // simulates stop
        advanceUntilIdle()

        repo.get("tt999", null)!!.currentTime shouldBe 120.0
    }
}
```

**Commit:** `test(player): add periodic progress sync timing tests (RED)`

---

### Step 8: Error recovery test (RED)

**File:** `feature/player/src/test/kotlin/studios/drible/tocabonito/feature/player/ErrorRecoveryTest.kt`

```kotlin
package studios.drible.tocabonito.feature.player

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.testing.FakeProgressRepository
import studios.drible.tocabonito.core.testing.FakeStreamRepository
import studios.drible.tocabonito.core.testing.FakeSubtitleRepository
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamQuality
import androidx.lifecycle.SavedStateHandle

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorRecoveryTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `on player error with torrentId, retries resolveTranscode up to 3 times`() = runTest {
        val streamRepo = FakeStreamRepository()
        streamRepo.transcodeResult = StreamLink(
            id = "new-id",
            fileName = "movie.mkv",
            fileSize = 4_500_000,
            hlsUrl = null,
            directUrl = "https://rd.com/new-stream",
            quality = StreamQuality.FULL,
        )

        val vm = PlayerViewModel(
            savedStateHandle = SavedStateHandle(),
            progressRepository = FakeProgressRepository(),
            subtitleRepository = FakeSubtitleRepository(),
            streamRepository = streamRepo,
        )

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = "tt999",
            torrentId = "torrent-abc",
        ))
        advanceUntilIdle()

        // First error -> retry
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        advanceUntilIdle()

        vm.state.value.streamUrl shouldBe "https://rd.com/new-stream"
        vm.state.value.playerError shouldBe null // recovered successfully

        // Second error -> retry
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        advanceUntilIdle()
        vm.state.value.playerError shouldBe null

        // Third error -> retry
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        advanceUntilIdle()
        vm.state.value.playerError shouldBe null

        // Fourth error -> give up, show error
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        advanceUntilIdle()
        vm.state.value.playerError shouldNotBe null
        vm.state.value.playerError!!.canRetry shouldBe false
    }

    @Test
    fun `on player error without torrentId, shows error immediately`() = runTest {
        val vm = PlayerViewModel(
            savedStateHandle = SavedStateHandle(),
            progressRepository = FakeProgressRepository(),
            subtitleRepository = FakeSubtitleRepository(),
            streamRepository = FakeStreamRepository(),
        )

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = "tt999",
            torrentId = null, // no torrent ID
        ))
        advanceUntilIdle()

        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        advanceUntilIdle()

        vm.state.value.playerError shouldNotBe null
        vm.state.value.playerError!!.canRetry shouldBe false
    }

    @Test
    fun `successful recovery resets retry counter`() = runTest {
        val streamRepo = FakeStreamRepository()
        streamRepo.transcodeResult = StreamLink(
            id = "new-id",
            fileName = "movie.mkv",
            fileSize = 4_500_000,
            hlsUrl = null,
            directUrl = "https://rd.com/new-stream",
            quality = StreamQuality.FULL,
        )

        val vm = PlayerViewModel(
            savedStateHandle = SavedStateHandle(),
            progressRepository = FakeProgressRepository(),
            subtitleRepository = FakeSubtitleRepository(),
            streamRepository = streamRepo,
        )

        vm.onIntent(PlayerIntent.Initialize(
            mediaId = "tt999",
            streamUrl = "http://example.com/movie.mkv",
            title = "Test Movie",
            episodeId = null,
            imdbId = "tt999",
            torrentId = "torrent-abc",
        ))
        advanceUntilIdle()

        // Error + recovery
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        advanceUntilIdle()
        vm.state.value.playerError shouldBe null

        // Simulate successful playback resumed (play intent resets counter)
        vm.onIntent(PlayerIntent.Play)

        // Now retry count is reset, so we get 3 more tries
        vm.onIntent(PlayerIntent.OnPlayerError("Source error"))
        advanceUntilIdle()
        vm.state.value.playerError shouldBe null // still retrying
    }
}
```

**Commit:** `test(player): add error recovery tests for transcode retry logic (RED)`

---

### Step 9: Update PlayerUiState + PlayerIntent (GREEN infrastructure)

**File:** `feature/player/src/main/kotlin/studios/drible/tocabonito/feature/player/PlayerUiState.kt`

```kotlin
package studios.drible.tocabonito.feature.player

import studios.drible.tocabonito.core.domain.model.AudioTrack
import studios.drible.tocabonito.core.domain.model.SubtitleTrack

data class PlayerUiState(
    val streamUrl: String = "",
    val mediaTitle: String = "",
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val resumePositionMs: Long? = null,
    val showControls: Boolean = true,
    val isBuffering: Boolean = false,
    // Subtitle/Audio tracks
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedSubtitle: SubtitleTrack? = null,
    val audioTracks: List<AudioTrack> = emptyList(),
    val selectedAudio: AudioTrack? = null,
    val showTrackSelector: Boolean = false,
    // Error recovery
    val playerError: PlayerError? = null,
)

data class PlayerError(
    val message: String,
    val retryCount: Int,
    val canRetry: Boolean,
)

sealed class PlayerIntent {
    data class Initialize(
        val mediaId: String,
        val streamUrl: String,
        val title: String,
        val episodeId: String?,
        val imdbId: String? = null,
        val torrentId: String? = null,
    ) : PlayerIntent()

    data object Play : PlayerIntent()
    data object Pause : PlayerIntent()
    data class Seek(val positionMs: Long) : PlayerIntent()
    data object SkipForward : PlayerIntent()
    data object SkipBackward : PlayerIntent()
    data class UpdatePosition(val positionMs: Long, val durationMs: Long) : PlayerIntent()
    data object ToggleControls : PlayerIntent()
    // Track selection
    data class SetSubtitleTrack(val track: SubtitleTrack?) : PlayerIntent()
    data class SetAudioTrack(val track: AudioTrack) : PlayerIntent()
    data object ShowTrackSelector : PlayerIntent()
    data object DismissTrackSelector : PlayerIntent()
    // Error handling
    data class OnPlayerError(val message: String) : PlayerIntent()
    // Track discovery from ExoPlayer
    data class UpdateTracks(
        val audioTracks: List<AudioTrack>,
        val subtitleTracks: List<SubtitleTrack>,
    ) : PlayerIntent()
}
```

**Commit:** `feat(player): extend PlayerUiState and PlayerIntent for tracks and error recovery`

---

### Step 10: Update PlayerViewModel (GREEN - all tests pass)

**File:** `feature/player/src/main/kotlin/studios/drible/tocabonito/feature/player/PlayerViewModel.kt`

```kotlin
package studios.drible.tocabonito.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import studios.drible.tocabonito.core.domain.model.AudioTrack
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.SubtitleTrack
import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.repository.ProgressRepository
import studios.drible.tocabonito.core.domain.repository.StreamRepository
import studios.drible.tocabonito.core.domain.repository.SubtitleRepository
import studios.drible.tocabonito.core.ui.mvi.MviViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val progressRepository: ProgressRepository,
    private val subtitleRepository: SubtitleRepository,
    private val streamRepository: StreamRepository,
) : MviViewModel<PlayerUiState, PlayerIntent>(PlayerUiState()) {

    private var currentMediaId: String = ""
    private var currentEpisodeId: String? = null
    private var currentImdbId: String? = null
    private var currentTorrentId: String? = null
    private var retryCount: Int = 0
    private var periodicSaveJob: Job? = null

    companion object {
        private const val SKIP_AMOUNT_MS = 10_000L
        private const val PERIODIC_SAVE_INTERVAL_MS = 10_000L
        private const val MAX_RETRY_COUNT = 3
    }

    init {
        val mediaId: String? = savedStateHandle["mediaId"]
        val rawStreamUrl: String? = savedStateHandle["streamUrl"]
        if (mediaId != null && rawStreamUrl != null) {
            val streamUrl = android.net.Uri.decode(rawStreamUrl)
            onIntent(PlayerIntent.Initialize(mediaId, streamUrl, mediaId, null))
        }
    }

    override fun onIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.Initialize -> handleInitialize(intent)
            is PlayerIntent.Play -> handlePlay()
            is PlayerIntent.Pause -> handlePause()
            is PlayerIntent.Seek -> setState { copy(currentPositionMs = intent.positionMs) }
            is PlayerIntent.SkipForward -> setState {
                copy(currentPositionMs = (currentPositionMs + SKIP_AMOUNT_MS).coerceAtMost(durationMs))
            }
            is PlayerIntent.SkipBackward -> setState {
                copy(currentPositionMs = (currentPositionMs - SKIP_AMOUNT_MS).coerceAtLeast(0))
            }
            is PlayerIntent.UpdatePosition -> handleUpdatePosition(intent)
            is PlayerIntent.ToggleControls -> setState { copy(showControls = !showControls) }
            is PlayerIntent.SetSubtitleTrack -> setState { copy(selectedSubtitle = intent.track) }
            is PlayerIntent.SetAudioTrack -> setState { copy(selectedAudio = intent.track) }
            is PlayerIntent.ShowTrackSelector -> setState { copy(showTrackSelector = true) }
            is PlayerIntent.DismissTrackSelector -> setState { copy(showTrackSelector = false) }
            is PlayerIntent.OnPlayerError -> handlePlayerError(intent)
            is PlayerIntent.UpdateTracks -> handleUpdateTracks(intent)
        }
    }

    private fun handleInitialize(intent: PlayerIntent.Initialize) {
        currentMediaId = intent.mediaId
        currentEpisodeId = intent.episodeId
        currentImdbId = intent.imdbId
        currentTorrentId = intent.torrentId
        retryCount = 0

        setState {
            copy(
                streamUrl = intent.streamUrl,
                mediaTitle = intent.title,
                playerError = null,
            )
        }

        // Load resume position
        viewModelScope.launch {
            val existing = progressRepository.get(intent.mediaId, intent.episodeId)
            if (existing != null) {
                val resumeMs = (existing.currentTime * 1000).toLong()
                setState { copy(resumePositionMs = resumeMs) }
            }
        }

        // Fetch subtitles from OpenSubtitles
        intent.imdbId?.let { imdbId ->
            viewModelScope.launch {
                val track = subtitleRepository.fetchSubtitle(imdbId)
                if (track != null) {
                    setState {
                        copy(
                            subtitleTracks = subtitleTracks + track,
                            selectedSubtitle = track, // auto-select
                        )
                    }
                }
            }
        }
    }

    private fun handlePlay() {
        setState { copy(isPlaying = true, playerError = null) }
        retryCount = 0 // successful playback resets retry counter
        startPeriodicSave()
    }

    private fun handlePause() {
        setState { copy(isPlaying = false) }
        stopPeriodicSave()
        saveProgress()
    }

    private fun handleUpdatePosition(intent: PlayerIntent.UpdatePosition) {
        setState {
            copy(
                currentPositionMs = intent.positionMs,
                durationMs = intent.durationMs,
            )
        }
        if (!currentState.isPlaying) {
            saveProgress()
        }
    }

    private fun handleUpdateTracks(intent: PlayerIntent.UpdateTracks) {
        setState {
            copy(
                audioTracks = intent.audioTracks,
                // Merge: keep sideloaded subs + add embedded ones
                subtitleTracks = intent.subtitleTracks + subtitleTracks.filter { it.isExternal },
            )
        }
    }

    private fun handlePlayerError(intent: PlayerIntent.OnPlayerError) {
        val torrentId = currentTorrentId
        if (torrentId != null && retryCount < MAX_RETRY_COUNT) {
            retryCount++
            viewModelScope.launch {
                try {
                    val newLink = streamRepository.resolveTranscode(torrentId)
                    setState {
                        copy(
                            streamUrl = newLink.directUrl,
                            playerError = null,
                        )
                    }
                } catch (e: Exception) {
                    setState {
                        copy(playerError = PlayerError(
                            message = intent.message,
                            retryCount = retryCount,
                            canRetry = false,
                        ))
                    }
                }
            }
        } else {
            setState {
                copy(playerError = PlayerError(
                    message = intent.message,
                    retryCount = retryCount,
                    canRetry = false,
                ))
            }
        }
    }

    private fun startPeriodicSave() {
        periodicSaveJob?.cancel()
        periodicSaveJob = viewModelScope.launch {
            while (true) {
                delay(PERIODIC_SAVE_INTERVAL_MS)
                if (currentState.isPlaying) {
                    saveProgress()
                }
            }
        }
    }

    private fun stopPeriodicSave() {
        periodicSaveJob?.cancel()
        periodicSaveJob = null
    }

    private fun saveProgress() {
        val s = currentState
        if (currentMediaId.isBlank() || s.durationMs <= 0) return
        viewModelScope.launch {
            val mediaItem = MediaItem(
                id = currentMediaId,
                title = s.mediaTitle,
                overview = "",
                posterPath = null,
                backdropPath = null,
                mediaType = MediaType.MOVIE,
                releaseYear = 0,
                voteAverage = 0.0,
                genreIds = emptyList(),
            )
            val progress = WatchProgress(
                id = "wp_${currentMediaId}_${currentEpisodeId ?: ""}",
                mediaItem = mediaItem,
                currentTime = s.currentPositionMs / 1000.0,
                duration = s.durationMs / 1000.0,
                lastWatched = System.currentTimeMillis(),
                episodeId = currentEpisodeId,
            )
            progressRepository.save(progress)
        }
    }
}
```

**Commit:** `feat(player): implement periodic save, subtitle fetch, and error recovery in PlayerViewModel`

---

### Step 11: Update StreamRepository interface + fake for `resolveTranscode`

**File:** `core/domain/src/main/kotlin/studios/drible/tocabonito/core/domain/repository/StreamRepository.kt` (modify)

```kotlin
package studios.drible.tocabonito.core.domain.repository

import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamOption

interface StreamRepository {
    suspend fun availableStreams(imdbId: String, type: MediaType, season: Int?, episode: Int?): List<StreamOption>
    suspend fun resolveStream(option: StreamOption): StreamLink
    /** Re-resolve a torrent's transcode link (e.g., after playback error). */
    suspend fun resolveTranscode(torrentId: String): StreamLink
}
```

**File:** `core/testing/src/main/kotlin/studios/drible/tocabonito/core/testing/FakeStreamRepository.kt` (modify - add field)

Add to existing FakeStreamRepository:

```kotlin
var transcodeResult: StreamLink? = null
var resolveTranscodeCallCount = 0

override suspend fun resolveTranscode(torrentId: String): StreamLink {
    resolveTranscodeCallCount++
    return transcodeResult ?: throw IllegalStateException("No transcode result configured")
}
```

**Commit:** `feat(domain): add resolveTranscode to StreamRepository interface`

---

### Step 12: StreamRepositoryImpl - add resolveTranscode (GREEN)

Add to `StreamRepositoryImpl`:

```kotlin
override suspend fun resolveTranscode(torrentId: String): StreamLink {
    // Re-unrestrict using the torrent ID as the link identifier
    return realDebridClient.unrestrict(torrentId)
}
```

**Commit:** `feat(data): implement resolveTranscode in StreamRepositoryImpl`

---

### Step 13: TrackSelectorSheet composable

**File:** `feature/player/src/main/kotlin/studios/drible/tocabonito/feature/player/TrackSelectorSheet.kt`

```kotlin
package studios.drible.tocabonito.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import studios.drible.tocabonito.core.domain.model.AudioTrack
import studios.drible.tocabonito.core.domain.model.SubtitleTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorSheet(
    state: PlayerUiState,
    onSelectAudio: (AudioTrack) -> Unit,
    onSelectSubtitle: (SubtitleTrack?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Audio section
            if (state.audioTracks.isNotEmpty()) {
                Text(
                    text = "Audio",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                state.audioTracks.forEach { track ->
                    TrackRow(
                        label = track.displayName,
                        isSelected = track == state.selectedAudio,
                        onClick = { onSelectAudio(track) },
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // Subtitle section
            Text(
                text = "Subtitles",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            // "Off" option
            TrackRow(
                label = "Off",
                isSelected = state.selectedSubtitle == null,
                onClick = { onSelectSubtitle(null) },
            )
            state.subtitleTracks.forEach { track ->
                TrackRow(
                    label = track.displayName,
                    isSelected = track == state.selectedSubtitle,
                    onClick = { onSelectSubtitle(track) },
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
```

**Commit:** `feat(player): add TrackSelectorSheet bottom sheet for audio/subtitle selection`

---

### Step 14: Wire PlayerScreen with subtitle + tracks + error overlay

Update `PlayerScreen.kt` to:

1. Build `MergingMediaSource` when `selectedSubtitle` has an external file path
2. Show `TrackSelectorSheet` when `state.showTrackSelector` is true
3. Forward `Player.Listener.onTracksChanged` to `PlayerIntent.UpdateTracks`
4. Forward `Player.Listener.onPlayerError` to `PlayerIntent.OnPlayerError`
5. Apply `trackSelectionParameters` when audio/subtitle selection changes
6. Show error overlay from `state.playerError`

Key changes (conceptual diff):

```kotlin
// In DisposableEffect(player) listener:
override fun onPlayerError(error: PlaybackException) {
    viewModel.onIntent(PlayerIntent.OnPlayerError(error.localizedMessage ?: "Playback error"))
}

override fun onTracksChanged(tracks: Tracks) {
    val audioTracks = tracks.groups
        .filter { it.type == C.TRACK_TYPE_AUDIO }
        .flatMapIndexed { groupIdx, group ->
            (0 until group.length).map { trackIdx ->
                val format = group.getTrackFormat(trackIdx)
                AudioTrack(
                    index = groupIdx * 100 + trackIdx,
                    name = format.label ?: format.language ?: "Track ${trackIdx + 1}",
                    languageCode = format.language,
                )
            }
        }
    val subtitleTracks = tracks.groups
        .filter { it.type == C.TRACK_TYPE_TEXT }
        .flatMapIndexed { groupIdx, group ->
            (0 until group.length).map { trackIdx ->
                val format = group.getTrackFormat(trackIdx)
                SubtitleTrack(
                    index = groupIdx * 100 + trackIdx,
                    name = format.label ?: format.language ?: "Subtitle ${trackIdx + 1}",
                    languageCode = format.language,
                    codec = format.codecs,
                )
            }
        }
    viewModel.onIntent(PlayerIntent.UpdateTracks(audioTracks, subtitleTracks))
}

// MergingMediaSource for sideloaded subtitle:
LaunchedEffect(state.selectedSubtitle, state.streamUrl) {
    val subtitle = state.selectedSubtitle
    if (subtitle != null && subtitle.isExternal && subtitle.externalUrl != null) {
        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(
            Uri.parse(subtitle.externalUrl)
        )
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage(subtitle.languageCode ?: "pt")
            .build()
        val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
            .createMediaSource(MediaItem.fromUri(state.streamUrl))
        val subtitleSource = SingleSampleMediaSource.Factory(DefaultDataSource.Factory(context))
            .createMediaSource(subtitleConfig, C.TIME_UNSET)
        player.setMediaSource(MergingMediaSource(mediaSource, subtitleSource))
        player.prepare()
    }
}
```

**Commit:** `feat(player): wire subtitle rendering, track discovery, and error recovery in PlayerScreen`

---

### Step 15: Update feature/player/build.gradle.kts

```kotlin
dependencies {
    implementation(project(":core:data"))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource) // for DefaultDataSource
    implementation(libs.compose.icons.extended)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.launcher)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.coroutines.test)
}
```

**Commit:** `build(player): add core:testing and media3-datasource dependencies`

---

### Step 16: Modify SubtitleTrack model (add externalFilePath convenience)

The existing `SubtitleTrack` already has `externalUrl: String?` which we use for the local file path of cached subtitles. No model change needed -- the `externalUrl` field serves double duty (remote URL or local file path, distinguished by `isExternal = true`).

---

### Step 17: Run all tests (VERIFY GREEN)

```bash
./gradlew :core:data:test --tests "*.OpenSubtitlesClientContractTest"
./gradlew :feature:player:test --tests "*.ProgressSyncTimingTest"
./gradlew :feature:player:test --tests "*.ErrorRecoveryTest"
./gradlew :feature:player:test --tests "*.PlayerViewModelTest"
```

**Commit:** `test: verify all subtitles + player enhancement tests pass (GREEN)`

---

## Commit Sequence

| # | Message | Type |
|---|---------|------|
| 1 | `test(data): add OpenSubtitlesClient contract test with JSON fixtures (RED)` | test |
| 2 | `test(data): add OpenSubtitles JSON fixtures` | test |
| 3 | `feat(data): add OpenSubtitles serializable response models` | feat |
| 4 | `feat(data): implement OpenSubtitlesClient with search and download` | feat |
| 5 | `feat(domain): add SubtitleRepository interface and fake for testing` | feat |
| 6 | `feat(data): implement SubtitleRepositoryImpl with cache and download` | feat |
| 7 | `test(player): add periodic progress sync timing tests (RED)` | test |
| 8 | `test(player): add error recovery tests for transcode retry logic (RED)` | test |
| 9 | `feat(player): extend PlayerUiState and PlayerIntent for tracks and error recovery` | feat |
| 10 | `feat(player): implement periodic save, subtitle fetch, and error recovery in PlayerViewModel` | feat |
| 11 | `feat(domain): add resolveTranscode to StreamRepository interface` | feat |
| 12 | `feat(data): implement resolveTranscode in StreamRepositoryImpl` | feat |
| 13 | `feat(player): add TrackSelectorSheet bottom sheet for audio/subtitle selection` | feat |
| 14 | `feat(player): wire subtitle rendering, track discovery, and error recovery in PlayerScreen` | feat |
| 15 | `build(player): add core:testing and media3-datasource dependencies` | build |
| 16 | `test: verify all subtitles + player enhancement tests pass (GREEN)` | test |

---

## Key Design Decisions

1. **SubtitleRepository as domain interface** -- keeps ViewModel testable with fakes, hides OpenSubtitles + caching details.
2. **Periodic save via coroutine delay loop** -- simpler than WorkManager for in-session saves; 10s interval balances battery vs. data loss.
3. **Retry counter resets on successful Play** -- prevents stale retry state after transient network issues resolve.
4. **MergingMediaSource for sideloaded SRT** -- standard Media3 approach; embedded subs are discovered via `onTracksChanged`.
5. **Cache-first subtitle fetch** -- avoids hitting OpenSubtitles API quota (20 downloads/day) on repeated plays.
6. **`externalUrl` reuse** -- the existing `SubtitleTrack.externalUrl` field holds the local file path for cached subs (no model migration needed).
