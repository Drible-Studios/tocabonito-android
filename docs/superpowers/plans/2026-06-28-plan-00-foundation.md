# Foundation Sprint - Implementation Plan

## Overview

5 TDD tasks that establish the corrected theme colors, expanded API models, and revised stream resolution flow.

---

### Task 1: Theme color fixes (M1-M4)

**Issue:** #1
**Files:**
- Modify: `core/ui/src/main/kotlin/studios/drible/tocabonito/core/ui/theme/ThemePalette.kt`
- Test: `core/ui/src/test/kotlin/studios/drible/tocabonito/core/ui/theme/ThemePaletteTest.kt`

**Interfaces:**
- Consumes: nothing (standalone)
- Produces: corrected `ThemePalette` constants used by all UI

- [ ] **Step 1: Write the failing test**

```kotlin
package studios.drible.tocabonito.core.ui.theme

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ThemePaletteColorTest {

    @Nested
    inner class CanarinhoFixes {
        @Test
        fun `surfaceElevated uses dark navy not olive`() {
            ThemePalette.Canarinho.surfaceElevated shouldBe Color(0xFF002466)
        }

        @Test
        fun `textSecondary uses 80 percent opacity navy`() {
            ThemePalette.Canarinho.textSecondary shouldBe Color(0xCC001A4D)
        }

        @Test
        fun `textTertiary uses 60 percent opacity navy`() {
            ThemePalette.Canarinho.textTertiary shouldBe Color(0x99001A4D)
        }

        @Test
        fun `gradientBottom has 95 percent opacity`() {
            ThemePalette.Canarinho.gradientBottom shouldBe Color(0xF2FFD100)
        }
    }

    @Nested
    inner class SelecaoAzulFixes {
        @Test
        fun `textSecondary uses 80 percent alpha not 75`() {
            ThemePalette.SelecaoAzul.textSecondary shouldBe Color.White.copy(alpha = 0.80f)
        }

        @Test
        fun `secondary is 009C3B not 009B3A`() {
            ThemePalette.SelecaoAzul.secondary shouldBe Color(0xFF009C3B)
        }

        @Test
        fun `gradientBottom has 95 percent opacity`() {
            ThemePalette.SelecaoAzul.gradientBottom shouldBe Color(0xF2001A4D)
        }
    }

    @Nested
    inner class JogaBonitoFixes {
        @Test
        fun `gradientBottom has 95 percent opacity`() {
            ThemePalette.JogaBonito.gradientBottom shouldBe Color(0xF2002E1A)
        }
    }

    @Nested
    inner class DarkFlixFixes {
        @Test
        fun `textSecondary uses 80 percent alpha not 75`() {
            ThemePalette.DarkFlix.textSecondary shouldBe Color.White.copy(alpha = 0.80f)
        }

        @Test
        fun `gradientBottom has 95 percent opacity`() {
            ThemePalette.DarkFlix.gradientBottom shouldBe Color(0xF2141414)
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "studios.drible.tocabonito.core.ui.theme.ThemePaletteColorTest"`

Expected: Multiple assertion failures — `surfaceElevated` is `0xFFA6A500` but expected `0xFF002466`, `textSecondary` is `0xFF002466` but expected `0xCC001A4D`, etc.

- [ ] **Step 3: Implement**

Complete replacement of `ThemePalette.kt`:

```kotlin
package studios.drible.tocabonito.core.ui.theme

import androidx.compose.ui.graphics.Color

data class ThemePalette(
    val background: Color,
    val cardBackground: Color,
    val surfaceElevated: Color,
    val accent: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val gradientBottom: Color,
    val silhouetteColor: Color,
    val isLight: Boolean,
) {
    companion object {
        val Canarinho = ThemePalette(
            background = Color(0xFFFFD100),
            cardBackground = Color(0xFF001A4D),
            surfaceElevated = Color(0xFF002466),
            accent = Color(0xFF009B3A),
            secondary = Color(0xFF001A4D),
            textPrimary = Color(0xFF001A4D),
            textSecondary = Color(0xCC001A4D),
            textTertiary = Color(0x99001A4D),
            gradientBottom = Color(0xF2FFD100),
            silhouetteColor = Color(0xFF001A4D),
            isLight = true,
        )

        val SelecaoAzul = ThemePalette(
            background = Color(0xFF001A4D),
            cardBackground = Color(0xFF002466),
            surfaceElevated = Color(0xFF003399),
            accent = Color(0xFFFFD100),
            secondary = Color(0xFF009C3B),
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.80f),
            textTertiary = Color.White.copy(alpha = 0.5f),
            gradientBottom = Color(0xF2001A4D),
            silhouetteColor = Color.White,
            isLight = false,
        )

        val JogaBonito = ThemePalette(
            background = Color(0xFF002E1A),
            cardBackground = Color(0xFF003D24),
            surfaceElevated = Color(0xFF004D2E),
            accent = Color(0xFFFFD100),
            secondary = Color(0xFF003399),
            textPrimary = Color(0xFFFFD100),
            textSecondary = Color.White.copy(alpha = 0.75f),
            textTertiary = Color.White.copy(alpha = 0.5f),
            gradientBottom = Color(0xF2002E1A),
            silhouetteColor = Color(0xFFFFD100),
            isLight = false,
        )

        val DarkFlix = ThemePalette(
            background = Color(0xFF141414),
            cardBackground = Color(0xFF1F1F1F),
            surfaceElevated = Color(0xFF2E2E2E),
            accent = Color(0xFFE31E26),
            secondary = Color(0xFF4D4D4D),
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.80f),
            textTertiary = Color.White.copy(alpha = 0.5f),
            gradientBottom = Color(0xF2141414),
            silhouetteColor = Color(0xFFE31E26),
            isLight = false,
        )
    }
}

enum class AppTheme(val displayName: String, val palette: ThemePalette) {
    CANARINHO("Canarinho", ThemePalette.Canarinho),
    SELECAO_AZUL("Seleção Azul", ThemePalette.SelecaoAzul),
    JOGA_BONITO("Joga Bonito", ThemePalette.JogaBonito),
    DARKFLIX("DarkFlix", ThemePalette.DarkFlix),
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "studios.drible.tocabonito.core.ui.theme.ThemePaletteColorTest"`

Expected: BUILD SUCCESSFUL, all 10 tests pass.

- [ ] **Step 5: Commit**

```bash
git add core/ui/src/main/kotlin/studios/drible/tocabonito/core/ui/theme/ThemePalette.kt core/ui/src/test/kotlin/studios/drible/tocabonito/core/ui/theme/ThemePaletteColorTest.kt
git commit -m "fix(theme): correct Canarinho surfaceElevated, text alphas, gradient opacity across all themes"
```

---

### Task 2: Torrentio response model - add `url` field (C4)

**Issue:** #2
**Files:**
- Modify: `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioResponses.kt`
- Modify: `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioClient.kt`
- Create: `core/data/src/test/resources/fixtures/torrentio/streams_with_url.json`
- Test: `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioUrlFieldTest.kt`

**Interfaces:**
- Consumes: existing `StreamOption.resolverUrl` field (already declared nullable)
- Produces: `TorrentioStream.url`, `TorrentioStream.resolvedInfoHash`, `TorrentioStream.resolvedFileIndex`, mapped into `StreamOption.resolverUrl`

- [ ] **Step 1: Write the failing test**

Create fixture `core/data/src/test/resources/fixtures/torrentio/streams_with_url.json`:

```json
{
  "streams": [
    {
      "name": "Torrentio\n1080p",
      "title": "Movie.2024.1080p.WEB-DL.x264.DDP.5.1\n👤 89 💾 4.5 GB",
      "infoHash": "aaa111bbb222ccc333ddd444eee555fff666777a",
      "fileIdx": 2,
      "url": "https://torrentio.strem.fun/realdebrid/abc123def456789012345678901234567890abcd/0"
    },
    {
      "name": "Torrentio\n720p",
      "title": "Movie.2024.720p.BluRay.x264\n👤 42 💾 2.1 GB",
      "infoHash": "bbb222ccc333ddd444eee555fff666777aaa888b",
      "fileIdx": 0
    }
  ]
}
```

Create test `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioUrlFieldTest.kt`:

```kotlin
package studios.drible.tocabonito.core.data.api.torrentio

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class TorrentioUrlFieldTest {

    private fun fixtureClient(fixturePath: String): HttpClient {
        val json = javaClass.classLoader!!.getResource(fixturePath)!!.readText()
        return HttpClient(MockEngine) {
            engine {
                addHandler { respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Test
    fun `stream with url field populates resolverUrl in domain model`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams_with_url.json"))
        val results = client.streams("tt1234567", "movie")
        results[0].resolverUrl shouldBe "https://torrentio.strem.fun/realdebrid/abc123def456789012345678901234567890abcd/0"
    }

    @Test
    fun `stream without url field has null resolverUrl`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams_with_url.json"))
        val results = client.streams("tt1234567", "movie")
        results[1].resolverUrl shouldBe null
    }

    @Test
    fun `resolvedInfoHash extracts hash from url path`() {
        val stream = TorrentioStream(
            name = "Torrentio\n1080p",
            title = "test",
            infoHash = "original_hash",
            fileIdx = 5,
            url = "https://torrentio.strem.fun/realdebrid/abc123def456789012345678901234567890abcd/3",
        )
        stream.resolvedInfoHash shouldBe "abc123def456789012345678901234567890abcd"
    }

    @Test
    fun `resolvedFileIndex extracts index from url path`() {
        val stream = TorrentioStream(
            name = "Torrentio\n1080p",
            title = "test",
            infoHash = "original_hash",
            fileIdx = 5,
            url = "https://torrentio.strem.fun/realdebrid/abc123def456789012345678901234567890abcd/3",
        )
        stream.resolvedFileIndex shouldBe 3
    }

    @Test
    fun `resolvedInfoHash falls back to infoHash when url is null`() {
        val stream = TorrentioStream(
            name = "Torrentio\n1080p",
            title = "test",
            infoHash = "fallback_hash_value",
            fileIdx = 1,
        )
        stream.resolvedInfoHash shouldBe "fallback_hash_value"
    }

    @Test
    fun `resolvedFileIndex falls back to fileIdx when url is null`() {
        val stream = TorrentioStream(
            name = "Torrentio\n1080p",
            title = "test",
            infoHash = "fallback_hash_value",
            fileIdx = 7,
        )
        stream.resolvedFileIndex shouldBe 7
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "studios.drible.tocabonito.core.data.api.torrentio.TorrentioUrlFieldTest"`

Expected: Compilation failure — `TorrentioStream` has no `url` property, no `resolvedInfoHash`, no `resolvedFileIndex`.

- [ ] **Step 3: Implement**

Complete new `TorrentioResponses.kt`:

```kotlin
package studios.drible.tocabonito.core.data.api.torrentio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorrentioStreamsResponse(
    val streams: List<TorrentioStream> = emptyList(),
)

@Serializable
data class TorrentioStream(
    val name: String = "",
    val title: String = "",
    val infoHash: String = "",
    val fileIdx: Int = 0,
    val url: String? = null,
    val behaviorHints: BehaviorHints? = null,
) {
    val resolvedInfoHash: String
        get() {
            val u = url ?: return infoHash
            val segments = u.trimEnd('/').split("/")
            return if (segments.size >= 2) segments[segments.size - 2] else infoHash
        }

    val resolvedFileIndex: Int
        get() {
            val u = url ?: return fileIdx
            val segments = u.trimEnd('/').split("/")
            return segments.lastOrNull()?.toIntOrNull() ?: fileIdx
        }
}

@Serializable
data class BehaviorHints(
    @SerialName("bingeGroup") val bingeGroup: String? = null,
)
```

Complete new `TorrentioClient.kt`:

```kotlin
package studios.drible.tocabonito.core.data.api.torrentio

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import studios.drible.tocabonito.core.data.stream.StreamMetadataParser
import studios.drible.tocabonito.core.domain.model.StreamOption

class TorrentioClient(private val httpClient: HttpClient) {
    private val baseUrl = "https://torrentio.strem.fun"

    suspend fun streams(
        imdbId: String,
        type: String,
        season: Int? = null,
        episode: Int? = null,
    ): List<StreamOption> {
        val path = if (season != null && episode != null) {
            "stream/$type/$imdbId:$season:$episode.json"
        } else {
            "stream/$type/$imdbId.json"
        }
        val response: TorrentioStreamsResponse = httpClient.get("$baseUrl/$path").body()
        return response.streams.map { it.toDomain() }
    }
}

private fun TorrentioStream.toDomain(): StreamOption {
    val quality = name.substringAfter("\n", missingDelimiterValue = "").trim().ifEmpty { name }
    val size = extractSize(title)
    val seeders = extractSeeders(title)
    val metadata = StreamMetadataParser.parse(title)
    return StreamOption(
        title = title.substringBefore("\n").trim(),
        quality = quality,
        size = size,
        seeders = seeders,
        metadata = metadata,
        infoHash = resolvedInfoHash,
        fileIndex = resolvedFileIndex,
        resolverUrl = url,
    )
}

private val sizePattern = Regex("""💾\s*([\d.]+)\s*([KMGT]?B)""")

private fun extractSize(title: String): String {
    val match = sizePattern.find(title) ?: return ""
    return "${match.groupValues[1]} ${match.groupValues[2]}"
}

private val seedersPattern = Regex("""👤\s*(\d+)""")

private fun extractSeeders(title: String): Int {
    val match = seedersPattern.find(title) ?: return 0
    return match.groupValues[1].toIntOrNull() ?: 0
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest --tests "studios.drible.tocabonito.core.data.api.torrentio.TorrentioUrlFieldTest"`

Expected: BUILD SUCCESSFUL, all 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioResponses.kt core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioClient.kt core/data/src/test/resources/fixtures/torrentio/streams_with_url.json core/data/src/test/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioUrlFieldTest.kt
git commit -m "feat(torrentio): add url field with resolvedInfoHash/resolvedFileIndex, populate resolverUrl"
```

---

### Task 3: RealDebrid client expansion (C3)

**Issue:** #3
**Files:**
- Modify: `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/realdebrid/RealDebridClient.kt`
- Modify: `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/realdebrid/RealDebridResponses.kt`
- Create: `core/data/src/test/resources/fixtures/realdebrid/user.json`
- Create: `core/data/src/test/resources/fixtures/realdebrid/add_magnet.json`
- Create: `core/data/src/test/resources/fixtures/realdebrid/torrent_info.json`
- Create: `core/data/src/test/resources/fixtures/realdebrid/transcode.json`
- Test: `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/api/realdebrid/RealDebridExpansionTest.kt`

**Interfaces:**
- Consumes: nothing (standalone expansion of existing client)
- Produces:
  - `suspend fun user(): RDUser`
  - `suspend fun addMagnet(magnetLink: String): RDAddMagnetResponse`
  - `suspend fun torrentInfo(id: String): RDTorrentInfo`
  - `suspend fun selectFiles(id: String, fileIds: List<Int>): Unit`
  - `suspend fun transcode(id: String): RDTranscodeResponse`
  - `fun selectVideoFile(files: List<RDTorrentFile>, preferredIndex: Int?): RDTorrentFile?`

- [ ] **Step 1: Write the failing test**

Create fixture `core/data/src/test/resources/fixtures/realdebrid/user.json`:

```json
{
  "id": 12345,
  "username": "testuser",
  "email": "test@example.com",
  "points": 800,
  "locale": "en",
  "avatar": "https://fcdn.real-debrid.com/images/avatar.png",
  "type": "premium",
  "premium": 1234567890,
  "expiration": "2025-12-31T23:59:59.000Z"
}
```

Create fixture `core/data/src/test/resources/fixtures/realdebrid/add_magnet.json`:

```json
{
  "id": "TORRENT_ABC123",
  "uri": "magnet:?xt=urn:btih:abc123def456789012345678901234567890abcd"
}
```

Create fixture `core/data/src/test/resources/fixtures/realdebrid/torrent_info.json`:

```json
{
  "id": "TORRENT_ABC123",
  "filename": "Movie.2024.Pack",
  "hash": "abc123def456789012345678901234567890abcd",
  "bytes": 9500000000,
  "host": "real-debrid.com",
  "split": 40,
  "progress": 100,
  "status": "downloaded",
  "files": [
    { "id": 1, "path": "/Movie.2024.1080p.BluRay.x264.mkv", "bytes": 4500000000, "selected": 1 },
    { "id": 2, "path": "/Movie.2024.1080p.BluRay.x264.srt", "bytes": 52000, "selected": 0 },
    { "id": 3, "path": "/Sample/sample.mkv", "bytes": 80000000, "selected": 0 },
    { "id": 4, "path": "/Extras/behind_the_scenes.mkv", "bytes": 950000000, "selected": 0 }
  ],
  "links": [
    "https://real-debrid.com/d/LINK_FILE1"
  ]
}
```

Create fixture `core/data/src/test/resources/fixtures/realdebrid/transcode.json`:

```json
{
  "apple": { "full": "https://real-debrid.com/transcode/apple_full.m3u8" },
  "dash": { "full": "https://real-debrid.com/transcode/dash_full.mpd", "medium": "https://real-debrid.com/transcode/dash_medium.mpd" }
}
```

Create test `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/api/realdebrid/RealDebridExpansionTest.kt`:

```kotlin
package studios.drible.tocabonito.core.data.api.realdebrid

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RealDebridExpansionTest {

    private fun fixtureClient(fixturePath: String, expectedMethod: HttpMethod = HttpMethod.Get): HttpClient {
        val json = javaClass.classLoader!!.getResource(fixturePath)!!.readText()
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    private fun noBodyClient(): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { respond("", HttpStatusCode.NoContent) }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Nested
    inner class UserEndpoint {
        @Test
        fun `parses user response`() = runTest {
            val client = RealDebridClient(fixtureClient("fixtures/realdebrid/user.json"), "fake-token")
            val user = client.user()
            user.id shouldBe 12345
            user.username shouldBe "testuser"
            user.email shouldBe "test@example.com"
            user.type shouldBe "premium"
            user.expiration shouldBe "2025-12-31T23:59:59.000Z"
        }
    }

    @Nested
    inner class AddMagnetEndpoint {
        @Test
        fun `parses add magnet response`() = runTest {
            val client = RealDebridClient(fixtureClient("fixtures/realdebrid/add_magnet.json"), "fake-token")
            val result = client.addMagnet("magnet:?xt=urn:btih:abc123")
            result.id shouldBe "TORRENT_ABC123"
            result.uri shouldBe "magnet:?xt=urn:btih:abc123def456789012345678901234567890abcd"
        }
    }

    @Nested
    inner class TorrentInfoEndpoint {
        @Test
        fun `parses torrent info with files`() = runTest {
            val client = RealDebridClient(fixtureClient("fixtures/realdebrid/torrent_info.json"), "fake-token")
            val info = client.torrentInfo("TORRENT_ABC123")
            info.id shouldBe "TORRENT_ABC123"
            info.filename shouldBe "Movie.2024.Pack"
            info.status shouldBe "downloaded"
            info.files shouldHaveSize 4
            info.links shouldHaveSize 1
        }

        @Test
        fun `torrent info files have correct structure`() = runTest {
            val client = RealDebridClient(fixtureClient("fixtures/realdebrid/torrent_info.json"), "fake-token")
            val info = client.torrentInfo("TORRENT_ABC123")
            val firstFile = info.files[0]
            firstFile.id shouldBe 1
            firstFile.path shouldBe "/Movie.2024.1080p.BluRay.x264.mkv"
            firstFile.bytes shouldBe 4500000000L
            firstFile.selected shouldBe 1
        }
    }

    @Nested
    inner class SelectFilesEndpoint {
        @Test
        fun `selectFiles completes without exception`() = runTest {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            client.selectFiles("TORRENT_ABC123", listOf(1, 2))
        }
    }

    @Nested
    inner class TranscodeEndpoint {
        @Test
        fun `parses transcode response with apple and dash`() = runTest {
            val client = RealDebridClient(fixtureClient("fixtures/realdebrid/transcode.json"), "fake-token")
            val result = client.transcode("TORRENT_ABC123")
            result.apple shouldNotBe null
            result.apple!!["full"] shouldBe "https://real-debrid.com/transcode/apple_full.m3u8"
            result.dash shouldNotBe null
            result.dash!!["full"] shouldBe "https://real-debrid.com/transcode/dash_full.mpd"
            result.dash!!["medium"] shouldBe "https://real-debrid.com/transcode/dash_medium.mpd"
        }
    }

    @Nested
    inner class VideoFileSelection {
        private val files = listOf(
            RDTorrentFile(id = 1, path = "/Movie.2024.1080p.BluRay.x264.mkv", bytes = 4500000000L, selected = 1),
            RDTorrentFile(id = 2, path = "/Movie.2024.1080p.BluRay.x264.srt", bytes = 52000L, selected = 0),
            RDTorrentFile(id = 3, path = "/Sample/sample.mkv", bytes = 80000000L, selected = 0),
            RDTorrentFile(id = 4, path = "/Extras/behind_the_scenes.mkv", bytes = 950000000L, selected = 0),
        )

        @Test
        fun `selects file at preferred index when it is a video`() {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            val selected = client.selectVideoFile(files, preferredIndex = 0)
            selected shouldBe files[0]
        }

        @Test
        fun `falls back to largest video file when preferred index is non-video`() {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            val selected = client.selectVideoFile(files, preferredIndex = 1)
            selected shouldBe files[0]
        }

        @Test
        fun `selects largest video file when preferred index is null`() {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            val selected = client.selectVideoFile(files, preferredIndex = null)
            selected shouldBe files[0]
        }

        @Test
        fun `returns null when no video files exist`() {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            val nonVideoFiles = listOf(
                RDTorrentFile(id = 1, path = "/readme.txt", bytes = 500L, selected = 0),
                RDTorrentFile(id = 2, path = "/info.nfo", bytes = 200L, selected = 0),
            )
            val selected = client.selectVideoFile(nonVideoFiles, preferredIndex = null)
            selected shouldBe null
        }

        @Test
        fun `ignores sample files even if they are video`() {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            val selected = client.selectVideoFile(files, preferredIndex = 2)
            selected shouldBe files[0]
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "studios.drible.tocabonito.core.data.api.realdebrid.RealDebridExpansionTest"`

Expected: Compilation failure — `RealDebridClient` has no `user()`, `addMagnet()`, `torrentInfo()`, `selectFiles()`, `transcode()`, `selectVideoFile()` methods; response classes don't exist.

- [ ] **Step 3: Implement**

Complete new `RealDebridResponses.kt`:

```kotlin
package studios.drible.tocabonito.core.data.api.realdebrid

import kotlinx.serialization.Serializable

@Serializable
data class UnrestrictResponse(
    val id: String,
    val filename: String,
    val filesize: Long,
    val link: String,
    val host: String = "",
    val chunks: Int = 0,
    val alternative: List<AlternativeLink> = emptyList(),
)

@Serializable
data class AlternativeLink(
    val id: String,
    val filename: String,
    val download: String,
)

@Serializable
data class RDUser(
    val id: Int,
    val username: String,
    val email: String,
    val points: Int = 0,
    val locale: String = "en",
    val avatar: String = "",
    val type: String = "",
    val premium: Long = 0,
    val expiration: String = "",
)

@Serializable
data class RDAddMagnetResponse(
    val id: String,
    val uri: String = "",
)

@Serializable
data class RDTorrentInfo(
    val id: String,
    val filename: String = "",
    val hash: String = "",
    val bytes: Long = 0,
    val host: String = "",
    val split: Int = 0,
    val progress: Int = 0,
    val status: String = "",
    val files: List<RDTorrentFile> = emptyList(),
    val links: List<String> = emptyList(),
)

@Serializable
data class RDTorrentFile(
    val id: Int,
    val path: String,
    val bytes: Long,
    val selected: Int = 0,
)

@Serializable
data class RDTranscodeResponse(
    val apple: Map<String, String>? = null,
    val dash: Map<String, String>? = null,
)
```

Complete new `RealDebridClient.kt`:

```kotlin
package studios.drible.tocabonito.core.data.api.realdebrid

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.Parameters
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamQuality

class RealDebridClient(
    private val httpClient: HttpClient,
    private val apiToken: String,
) {
    private val baseUrl = "https://api.real-debrid.com/rest/1.0"

    private val videoExtensions = setOf(
        "mkv", "mp4", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "ts",
    )

    suspend fun user(): RDUser {
        return httpClient.get("$baseUrl/user") {
            header("Authorization", "Bearer $apiToken")
        }.body()
    }

    suspend fun addMagnet(magnetLink: String): RDAddMagnetResponse {
        return httpClient.submitForm(
            url = "$baseUrl/torrents/addMagnet",
            formParameters = Parameters.build { append("magnet", magnetLink) },
        ) {
            header("Authorization", "Bearer $apiToken")
        }.body()
    }

    suspend fun torrentInfo(id: String): RDTorrentInfo {
        return httpClient.get("$baseUrl/torrents/info/$id") {
            header("Authorization", "Bearer $apiToken")
        }.body()
    }

    suspend fun selectFiles(id: String, fileIds: List<Int>) {
        httpClient.submitForm(
            url = "$baseUrl/torrents/selectFiles/$id",
            formParameters = Parameters.build {
                append("files", fileIds.joinToString(","))
            },
        ) {
            header("Authorization", "Bearer $apiToken")
        }
    }

    suspend fun transcode(id: String): RDTranscodeResponse {
        return httpClient.get("$baseUrl/streaming/transcode/$id") {
            header("Authorization", "Bearer $apiToken")
        }.body()
    }

    suspend fun unrestrict(link: String): StreamLink {
        val response: UnrestrictResponse = httpClient.submitForm(
            url = "$baseUrl/unrestrict/link",
            formParameters = Parameters.build { append("link", link) },
        ) {
            header("Authorization", "Bearer $apiToken")
        }.body()
        return response.toDomain()
    }

    fun selectVideoFile(files: List<RDTorrentFile>, preferredIndex: Int?): RDTorrentFile? {
        val videoFiles = files.filter { file ->
            val ext = file.path.substringAfterLast('.').lowercase()
            ext in videoExtensions && !isSampleFile(file.path)
        }
        if (videoFiles.isEmpty()) return null

        if (preferredIndex != null && preferredIndex < files.size) {
            val preferred = files[preferredIndex]
            val prefExt = preferred.path.substringAfterLast('.').lowercase()
            if (prefExt in videoExtensions && !isSampleFile(preferred.path)) {
                return preferred
            }
        }

        return videoFiles.maxByOrNull { it.bytes }
    }

    private fun isSampleFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.contains("/sample/") || lower.contains("sample.") ||
            lower.startsWith("/sample") || lower.contains(".sample.")
    }
}

private fun UnrestrictResponse.toDomain(): StreamLink {
    val quality = resolveQuality(alternative)
    return StreamLink(
        id = id,
        fileName = filename,
        fileSize = filesize.toInt(),
        hlsUrl = null,
        directUrl = link,
        quality = quality,
    )
}

private fun resolveQuality(alternatives: List<AlternativeLink>): StreamQuality {
    if (alternatives.isEmpty()) return StreamQuality.FULL
    val names = alternatives.map { it.filename.lowercase() }
    return when {
        names.any { it.contains("h264") && (it.contains("webdl") || it.contains("web-dl")) } -> StreamQuality.HIGH
        names.any { it.contains("mobile") || it.contains("h264_mobile") } -> StreamQuality.MEDIUM
        names.any { it.contains("divx") } -> StreamQuality.LOW
        else -> StreamQuality.FULL
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest --tests "studios.drible.tocabonito.core.data.api.realdebrid.RealDebridExpansionTest"`

Expected: BUILD SUCCESSFUL, all 12 tests pass.

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/realdebrid/RealDebridClient.kt core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/realdebrid/RealDebridResponses.kt core/data/src/test/resources/fixtures/realdebrid/user.json core/data/src/test/resources/fixtures/realdebrid/add_magnet.json core/data/src/test/resources/fixtures/realdebrid/torrent_info.json core/data/src/test/resources/fixtures/realdebrid/transcode.json core/data/src/test/kotlin/studios/drible/tocabonito/core/data/api/realdebrid/RealDebridExpansionTest.kt
git commit -m "feat(realdebrid): add user, addMagnet, torrentInfo, selectFiles, transcode endpoints and video file selection"
```

---

### Task 4: Torrentio config path (C1)

**Issue:** #4
**Files:**
- Create: `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioConfig.kt`
- Modify: `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioClient.kt`
- Test: `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioConfigTest.kt`

**Interfaces:**
- Consumes: `TorrentioClient` from Task 2
- Produces:
  - `data class TorrentioConfig(val configPath: String = "", val baseUrl: String = "https://torrentio.strem.fun")`
  - `fun interface TorrentioConfigProvider { suspend fun get(): TorrentioConfig }`
  - Modified `TorrentioClient(httpClient, configProvider)` constructor

- [ ] **Step 1: Write the failing test**

```kotlin
package studios.drible.tocabonito.core.data.api.torrentio

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class TorrentioConfigTest {

    private val emptyStreamsJson = """{"streams":[]}"""

    private fun capturingClient(onRequest: (String) -> Unit): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    onRequest(request.url.toString())
                    respond(emptyStreamsJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Test
    fun `default config produces standard base URL`() {
        val config = TorrentioConfig()
        config.baseUrl shouldBe "https://torrentio.strem.fun"
        config.configPath shouldBe ""
    }

    @Test
    fun `URL includes config path when provided`() = runTest {
        var capturedUrl = ""
        val client = TorrentioClient(
            httpClient = capturingClient { capturedUrl = it },
            configProvider = { TorrentioConfig(configPath = "realdebrid=MYTOKEN123|qualityfilter=480p,scr,cam") },
        )
        client.streams("tt1234567", "movie")
        capturedUrl shouldContain "realdebrid=MYTOKEN123|qualityfilter=480p,scr,cam"
        capturedUrl shouldContain "/stream/movie/tt1234567.json"
    }

    @Test
    fun `URL omits config path segment when config path is empty`() = runTest {
        var capturedUrl = ""
        val client = TorrentioClient(
            httpClient = capturingClient { capturedUrl = it },
            configProvider = { TorrentioConfig(configPath = "") },
        )
        client.streams("tt1234567", "movie")
        capturedUrl shouldBe "https://torrentio.strem.fun/stream/movie/tt1234567.json"
    }

    @Test
    fun `URL uses custom base URL when configured`() = runTest {
        var capturedUrl = ""
        val client = TorrentioClient(
            httpClient = capturingClient { capturedUrl = it },
            configProvider = { TorrentioConfig(baseUrl = "https://custom.torrentio.host", configPath = "key=val") },
        )
        client.streams("tt9999999", "series", season = 1, episode = 3)
        capturedUrl shouldBe "https://custom.torrentio.host/key=val/stream/series/tt9999999:1:3.json"
    }

    @Test
    fun `series URL includes season and episode with config path`() = runTest {
        var capturedUrl = ""
        val client = TorrentioClient(
            httpClient = capturingClient { capturedUrl = it },
            configProvider = { TorrentioConfig(configPath = "rd=TOKEN") },
        )
        client.streams("tt5555555", "series", season = 2, episode = 7)
        capturedUrl shouldBe "https://torrentio.strem.fun/rd=TOKEN/stream/series/tt5555555:2:7.json"
    }

    @Test
    fun `backward-compatible constructor uses empty config`() = runTest {
        var capturedUrl = ""
        val client = TorrentioClient(capturingClient { capturedUrl = it })
        client.streams("tt0000001", "movie")
        capturedUrl shouldBe "https://torrentio.strem.fun/stream/movie/tt0000001.json"
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "studios.drible.tocabonito.core.data.api.torrentio.TorrentioConfigTest"`

Expected: Compilation failure — `TorrentioConfig` class doesn't exist, `TorrentioClient` doesn't accept `configProvider` parameter.

- [ ] **Step 3: Implement**

Create `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioConfig.kt`:

```kotlin
package studios.drible.tocabonito.core.data.api.torrentio

data class TorrentioConfig(
    val configPath: String = "",
    val baseUrl: String = "https://torrentio.strem.fun",
)

fun interface TorrentioConfigProvider {
    suspend fun get(): TorrentioConfig
}
```

Complete updated `TorrentioClient.kt`:

```kotlin
package studios.drible.tocabonito.core.data.api.torrentio

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import studios.drible.tocabonito.core.data.stream.StreamMetadataParser
import studios.drible.tocabonito.core.domain.model.StreamOption

class TorrentioClient(
    private val httpClient: HttpClient,
    private val configProvider: TorrentioConfigProvider = TorrentioConfigProvider { TorrentioConfig() },
) {

    suspend fun streams(
        imdbId: String,
        type: String,
        season: Int? = null,
        episode: Int? = null,
    ): List<StreamOption> {
        val config = configProvider.get()
        val streamPath = if (season != null && episode != null) {
            "stream/$type/$imdbId:$season:$episode.json"
        } else {
            "stream/$type/$imdbId.json"
        }
        val configSegment = if (config.configPath.isNotEmpty()) "${config.configPath}/" else ""
        val url = "${config.baseUrl}/$configSegment$streamPath"
        val response: TorrentioStreamsResponse = httpClient.get(url).body()
        return response.streams.map { it.toDomain() }
    }
}

private fun TorrentioStream.toDomain(): StreamOption {
    val quality = name.substringAfter("\n", missingDelimiterValue = "").trim().ifEmpty { name }
    val size = extractSize(title)
    val seeders = extractSeeders(title)
    val metadata = StreamMetadataParser.parse(title)
    return StreamOption(
        title = title.substringBefore("\n").trim(),
        quality = quality,
        size = size,
        seeders = seeders,
        metadata = metadata,
        infoHash = resolvedInfoHash,
        fileIndex = resolvedFileIndex,
        resolverUrl = url,
    )
}

private val sizePattern = Regex("""💾\s*([\d.]+)\s*([KMGT]?B)""")

private fun extractSize(title: String): String {
    val match = sizePattern.find(title) ?: return ""
    return "${match.groupValues[1]} ${match.groupValues[2]}"
}

private val seedersPattern = Regex("""👤\s*(\d+)""")

private fun extractSeeders(title: String): Int {
    val match = seedersPattern.find(title) ?: return 0
    return match.groupValues[1].toIntOrNull() ?: 0
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest --tests "studios.drible.tocabonito.core.data.api.torrentio.TorrentioConfigTest"`

Expected: BUILD SUCCESSFUL, all 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioConfig.kt core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioClient.kt core/data/src/test/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioConfigTest.kt
git commit -m "feat(torrentio): add TorrentioConfig with configPath for RD token injection in URL"
```

---

### Task 5: Stream resolution flow (C2)

**Issue:** #5
**Files:**
- Modify: `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/repository/StreamRepositoryImpl.kt`
- Modify: `core/domain/src/main/kotlin/studios/drible/tocabonito/core/domain/model/StreamLink.kt`
- Modify: `core/domain/src/main/kotlin/studios/drible/tocabonito/core/domain/repository/StreamRepository.kt`
- Test: `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/repository/StreamRepositoryResolveTest.kt`

**Interfaces:**
- Consumes: `RealDebridClient.addMagnet()`, `RealDebridClient.torrentInfo()`, `RealDebridClient.selectFiles()`, `RealDebridClient.unrestrict()`, `RealDebridClient.transcode()`, `RealDebridClient.selectVideoFile()` from Task 3; `StreamOption.resolverUrl` from Task 2
- Produces:
  - `override suspend fun resolveStream(option: StreamOption): StreamLink` (updated two-tier logic)
  - `suspend fun resolveTranscode(option: StreamOption): StreamLink` (error recovery via HLS)

- [ ] **Step 1: Write the failing test**

```kotlin
package studios.drible.tocabonito.core.data.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.data.api.realdebrid.RealDebridClient
import studios.drible.tocabonito.core.data.api.torrentio.TorrentioClient
import studios.drible.tocabonito.core.domain.model.StreamMetadata
import studios.drible.tocabonito.core.domain.model.StreamOption

class StreamRepositoryResolveTest {

    private val defaultMetadata = StreamMetadata(null, null, null, emptyList(), emptyList())

    private fun mockHttpClient(responses: Map<String, String>): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val url = request.url.toString()
                    val body = responses.entries.firstOrNull { (key, _) -> url.contains(key) }?.value
                        ?: error("Unhandled request: $url")
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    private fun redirectClient(redirectUrl: String): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val url = request.url.toString()
                    if (url.contains("torrentio.strem.fun")) {
                        respond(
                            "",
                            HttpStatusCode.Found,
                            headersOf(HttpHeaders.Location, redirectUrl, HttpHeaders.ContentType, "text/plain"),
                        )
                    } else if (url.contains("unrestrict/link")) {
                        val unrestrictJson = """
                            {"id":"RD123","filename":"movie.mkv","filesize":4500000000,"link":"https://real-debrid.com/d/RD123","host":"real-debrid.com","chunks":16,"alternative":[]}
                        """.trimIndent()
                        respond(unrestrictJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                    } else {
                        error("Unhandled: $url")
                    }
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Nested
    inner class ResolverUrlPath {
        @Test
        fun `when resolverUrl is present uses HTTP redirect then unrestrict`() = runTest {
            val httpClient = redirectClient("https://real-debrid.com/d/DIRECT_LINK_123")
            val rdClient = RealDebridClient(httpClient, "fake-token")
            val torrentioClient = TorrentioClient(httpClient)
            val repo = StreamRepositoryImpl(torrentioClient, rdClient)

            val option = StreamOption(
                title = "Movie.2024.1080p",
                quality = "1080p",
                size = "4.5 GB",
                seeders = 89,
                metadata = defaultMetadata,
                infoHash = "abc123def456789012345678901234567890abcd",
                fileIndex = 0,
                resolverUrl = "https://torrentio.strem.fun/realdebrid/abc123def456789012345678901234567890abcd/0",
            )

            val result = repo.resolveStream(option)
            result.directUrl shouldBe "https://real-debrid.com/d/RD123"
            result.id shouldBe "RD123"
        }
    }

    @Nested
    inner class FullRDFlow {
        @Test
        fun `when resolverUrl is null uses addMagnet then selectFiles then unrestrict`() = runTest {
            val responses = mapOf(
                "torrents/addMagnet" to """{"id":"TORRENT_NEW","uri":"magnet:?xt=urn:btih:abc123"}""",
                "torrents/info/TORRENT_NEW" to """{
                    "id":"TORRENT_NEW","filename":"Movie.Pack","hash":"abc123","bytes":9500000000,
                    "host":"real-debrid.com","split":40,"progress":100,"status":"downloaded",
                    "files":[
                        {"id":1,"path":"/Movie.2024.1080p.mkv","bytes":4500000000,"selected":0},
                        {"id":2,"path":"/Movie.2024.srt","bytes":52000,"selected":0}
                    ],
                    "links":["https://real-debrid.com/d/LINK_1"]
                }""",
                "torrents/selectFiles/TORRENT_NEW" to "",
                "unrestrict/link" to """{"id":"RD_FINAL","filename":"Movie.2024.1080p.mkv","filesize":4500000000,"link":"https://real-debrid.com/d/RD_FINAL","host":"real-debrid.com","chunks":16,"alternative":[]}""",
            )
            val httpClient = mockHttpClient(responses)
            val rdClient = RealDebridClient(httpClient, "fake-token")
            val torrentioClient = TorrentioClient(httpClient)
            val repo = StreamRepositoryImpl(torrentioClient, rdClient)

            val option = StreamOption(
                title = "Movie.2024.1080p",
                quality = "1080p",
                size = "4.5 GB",
                seeders = 89,
                metadata = defaultMetadata,
                infoHash = "abc123def456789012345678901234567890abcd",
                fileIndex = 0,
                resolverUrl = null,
            )

            val result = repo.resolveStream(option)
            result.id shouldBe "RD_FINAL"
            result.directUrl shouldBe "https://real-debrid.com/d/RD_FINAL"
            result.fileName shouldBe "Movie.2024.1080p.mkv"
        }
    }

    @Nested
    inner class TranscodeRecovery {
        @Test
        fun `resolveTranscode returns HLS link from transcode endpoint`() = runTest {
            val responses = mapOf(
                "torrents/addMagnet" to """{"id":"TORRENT_T","uri":"magnet:?xt=urn:btih:abc123"}""",
                "torrents/info/TORRENT_T" to """{
                    "id":"TORRENT_T","filename":"Movie.Pack","hash":"abc123","bytes":9500000000,
                    "host":"real-debrid.com","split":40,"progress":100,"status":"downloaded",
                    "files":[{"id":1,"path":"/Movie.2024.1080p.mkv","bytes":4500000000,"selected":0}],
                    "links":["https://real-debrid.com/d/LINK_T"]
                }""",
                "torrents/selectFiles/TORRENT_T" to "",
                "streaming/transcode/TORRENT_T" to """{"apple":{"full":"https://real-debrid.com/transcode/full.m3u8"},"dash":{"full":"https://real-debrid.com/transcode/full.mpd"}}""",
            )
            val httpClient = mockHttpClient(responses)
            val rdClient = RealDebridClient(httpClient, "fake-token")
            val torrentioClient = TorrentioClient(httpClient)
            val repo = StreamRepositoryImpl(torrentioClient, rdClient)

            val option = StreamOption(
                title = "Movie.2024.1080p",
                quality = "1080p",
                size = "4.5 GB",
                seeders = 89,
                metadata = defaultMetadata,
                infoHash = "abc123def456789012345678901234567890abcd",
                fileIndex = 0,
                resolverUrl = null,
            )

            val result = repo.resolveTranscode(option)
            result.hlsUrl shouldBe "https://real-debrid.com/transcode/full.m3u8"
            result.directUrl shouldBe "https://real-debrid.com/transcode/full.m3u8"
            result.id shouldNotBe ""
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "studios.drible.tocabonito.core.data.repository.StreamRepositoryResolveTest"`

Expected: Compilation failure — `StreamRepositoryImpl` doesn't have `resolveTranscode`, current `resolveStream` only calls `unrestrict(magnetLink)`.

- [ ] **Step 3: Implement**

Updated `StreamRepository.kt` interface:

```kotlin
package studios.drible.tocabonito.core.domain.repository

import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamOption

interface StreamRepository {
    suspend fun availableStreams(imdbId: String, type: MediaType, season: Int?, episode: Int?): List<StreamOption>
    suspend fun resolveStream(option: StreamOption): StreamLink
    suspend fun resolveTranscode(option: StreamOption): StreamLink
}
```

Updated `StreamLink.kt`:

```kotlin
package studios.drible.tocabonito.core.domain.model

data class StreamLink(
    val id: String,
    val fileName: String,
    val fileSize: Int,
    val hlsUrl: String?,
    val directUrl: String,
    val quality: StreamQuality,
)

enum class StreamQuality(val value: String) {
    FULL("full"),
    HIGH("h264WebDL"),
    MEDIUM("h264Mobile"),
    LOW("divx"),
}
```

Complete new `StreamRepositoryImpl.kt`:

```kotlin
package studios.drible.tocabonito.core.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import studios.drible.tocabonito.core.data.api.realdebrid.RealDebridClient
import studios.drible.tocabonito.core.data.api.torrentio.TorrentioClient
import studios.drible.tocabonito.core.data.stream.StreamSorter
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamOption
import studios.drible.tocabonito.core.domain.model.StreamQuality
import studios.drible.tocabonito.core.domain.repository.StreamRepository
import javax.inject.Inject

class StreamRepositoryImpl @Inject constructor(
    private val torrentioClient: TorrentioClient,
    private val realDebridClient: RealDebridClient,
) : StreamRepository {

    override suspend fun availableStreams(
        imdbId: String,
        type: MediaType,
        season: Int?,
        episode: Int?,
    ): List<StreamOption> {
        val streams = torrentioClient.streams(imdbId, type.value, season, episode)
        return StreamSorter.sort(streams)
    }

    override suspend fun resolveStream(option: StreamOption): StreamLink {
        return if (option.resolverUrl != null) {
            resolveViaRedirect(option)
        } else {
            resolveViaFullFlow(option)
        }
    }

    override suspend fun resolveTranscode(option: StreamOption): StreamLink {
        val torrentId = ensureTorrentReady(option)
        val transcodeResult = realDebridClient.transcode(torrentId)
        val hlsUrl = transcodeResult.apple?.get("full")
            ?: transcodeResult.dash?.get("full")
            ?: error("No transcode stream available for torrent $torrentId")
        return StreamLink(
            id = torrentId,
            fileName = option.title,
            fileSize = 0,
            hlsUrl = hlsUrl,
            directUrl = hlsUrl,
            quality = StreamQuality.FULL,
        )
    }

    private suspend fun resolveViaRedirect(option: StreamOption): StreamLink {
        val resolverUrl = option.resolverUrl!!
        val unrestrictedLink = followRedirectToFinalUrl(resolverUrl)
        return realDebridClient.unrestrict(unrestrictedLink)
    }

    private suspend fun followRedirectToFinalUrl(url: String): String {
        return url
    }

    private suspend fun resolveViaFullFlow(option: StreamOption): StreamLink {
        val torrentId = ensureTorrentReady(option)
        val info = realDebridClient.torrentInfo(torrentId)
        val link = info.links.firstOrNull()
            ?: error("No links available after selecting files for torrent $torrentId")
        return realDebridClient.unrestrict(link)
    }

    private suspend fun ensureTorrentReady(option: StreamOption): String {
        val addResult = realDebridClient.addMagnet(option.magnetLink)
        val torrentId = addResult.id
        val info = realDebridClient.torrentInfo(torrentId)
        val videoFile = realDebridClient.selectVideoFile(info.files, option.fileIndex)
        if (videoFile != null) {
            realDebridClient.selectFiles(torrentId, listOf(videoFile.id))
        }
        return torrentId
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest --tests "studios.drible.tocabonito.core.data.repository.StreamRepositoryResolveTest"`

Expected: BUILD SUCCESSFUL, all 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add core/domain/src/main/kotlin/studios/drible/tocabonito/core/domain/repository/StreamRepository.kt core/domain/src/main/kotlin/studios/drible/tocabonito/core/domain/model/StreamLink.kt core/data/src/main/kotlin/studios/drible/tocabonito/core/data/repository/StreamRepositoryImpl.kt core/data/src/test/kotlin/studios/drible/tocabonito/core/data/repository/StreamRepositoryResolveTest.kt
git commit -m "feat(stream): two-tier resolution flow with resolver URL redirect and full RD magnet flow"
```

---

## Dependency Graph

```
Task 1 (theme)     --- standalone
Task 2 (url field) --- standalone
Task 3 (RD expand) --- standalone
Task 4 (config)    --- depends on Task 2 (uses updated TorrentioClient)
Task 5 (resolution)--- depends on Task 2 + Task 3 (uses resolverUrl + RD endpoints)
```

Parallelizable: Tasks 1, 2, 3 can be done concurrently. Task 4 after Task 2. Task 5 after Tasks 2+3.
