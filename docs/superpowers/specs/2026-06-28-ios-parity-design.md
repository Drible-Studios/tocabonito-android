# iOS Parity Design Spec

**Date:** 2026-06-28
**Status:** Draft
**Effort:** ~4 weeks (2 developers)

---

## 1. Overview & Goals

This effort brings the TocaBonito Android app to feature parity with the fully-working iOS (SwiftUI) reference app. The Android app currently has broken stream resolution (passes magnet links where hoster URLs are expected), missing Torrentio configuration, an incomplete home screen, no subtitle support, and several theme color mismatches. This spec addresses all 20 identified issues (4 Critical, 5 High, 4 Medium, 7 Low) through a Foundation Sprint followed by two parallel tracks (UI and Features).

**Success criteria — "done" looks like:**

- A user can search for a movie/series, see available streams with rich metadata, tap Play, and watch the stream via ExoPlayer on an NVIDIA Shield
- Stream resolution works via the two-tier approach (resolver URL fast path + full RD flow fallback)
- Home screen displays hero banner, trending, popular movies, popular series, and continue watching
- Downloads work end-to-end (resolve, download, play offline)
- API key is entered at runtime in Settings and validated against RD API
- Subtitle search and sideloading works via OpenSubtitles
- Firebase sync preserves favorites, progress, and settings across devices
- All four themes match the iOS color spec exactly

---

## 2. Foundation Sprint (C1-C4 + M1-M4)

### 2.1 Torrentio Client Fix (C1)

**Problem:** `TorrentioClient` calls `$baseUrl/stream/$type/$imdbId.json` without a config path. Without it, Torrentio returns no RD-compatible streams.

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioClient.kt`

**Changes:**

1. Add a `TorrentioConfig` data class:

```kotlin
data class TorrentioConfig(
    val providers: List<String> = listOf(
        "yts", "eztv", "rarbg", "1337x", "thepiratebay", "kickasstorrents", "torrentgalaxy"
    ),
    val language: String = "portuguese",
    val realDebridApiKey: String,
) {
    val configPath: String
        get() {
            val providersPart = "providers=${providers.joinToString(",")}"
            val langPart = "language=$language"
            val rdPart = "realdebrid=$realDebridApiKey"
            return "$providersPart|$langPart|$rdPart"
        }
}
```

2. Modify `TorrentioClient` constructor to accept a config provider (lambda or interface for testability):

```kotlin
class TorrentioClient(
    private val httpClient: HttpClient,
    private val configProvider: () -> TorrentioConfig,
)
```

3. Update URL construction:

```kotlin
val config = configProvider()
val url = "$baseUrl/${config.configPath}/stream/$type/$imdbId.json"
```

4. Episode URL becomes: `$baseUrl/${config.configPath}/stream/$type/$imdbId:$season:$episode.json`

**Testing:** Update `TorrentioClientContractTest` to verify config path is included in requests.

---

### 2.2 RealDebrid Client Expansion (C3)

**Problem:** `RealDebridClient` only has `unrestrict()`. The full RD flow requires 5 additional endpoints.

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/realdebrid/RealDebridClient.kt`

**New endpoints to add:**

| Method | Path | Purpose | Request | Response Model |
|--------|------|---------|---------|----------------|
| `GET` | `/user` | Validate API key | — | `UserResponse` |
| `POST` | `/torrents/addMagnet` | Submit magnet | `magnet` form param | `AddMagnetResponse` (id, uri) |
| `GET` | `/torrents/info/{id}` | Torrent status + files | — | `TorrentInfoResponse` |
| `POST` | `/torrents/selectFiles/{id}` | Select video files | `files` form param (comma-separated IDs) | 204 No Content |
| `GET` | `/streaming/transcode/{id}` | Get transcode URLs | — | `TranscodeResponse` |

**New response models** in `RealDebridResponses.kt`:

```kotlin
@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val premium: Int,  // days remaining
    val type: String,
    val expiration: String,
)

@Serializable
data class AddMagnetResponse(
    val id: String,
    val uri: String,
)

@Serializable
data class TorrentInfoResponse(
    val id: String,
    val filename: String,
    val status: String,  // "downloaded", "magnet_conversion", "waiting_files_selection", etc.
    val links: List<String>,
    val files: List<TorrentFile>,
)

@Serializable
data class TorrentFile(
    val id: Int,
    val path: String,
    val bytes: Long,
    val selected: Int,  // 0 or 1
)

@Serializable
data class TranscodeResponse(
    val apple: TranscodeQuality? = null,
    val dash: TranscodeQuality? = null,
    val liveMP4: TranscodeQuality? = null,
)

@Serializable
data class TranscodeQuality(
    val full: String? = null,  // URL
)
```

**Video file selection logic:**

```kotlin
private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm")

fun selectBestVideoFile(files: List<TorrentFile>): TorrentFile? {
    return files
        .filter { file ->
            val ext = file.path.substringAfterLast('.').lowercase()
            ext in videoExtensions
        }
        .maxByOrNull { it.bytes }
}
```

**Fix existing `unrestrict()`:** The current implementation passes a magnet link, but `/unrestrict/link` expects a hoster URL (the link from `torrentInfoResponse.links[]`). The method signature stays the same but documentation/callers must pass the correct URL type.

**Testing:** Add contract tests for each new endpoint with MockWebServer.

---

### 2.3 Torrentio Response Model Fix (C4)

**Problem:** `TorrentioStream` is missing the `url` field. When Torrentio returns streams with a resolver URL (for RD-configured requests), the `url` field contains the direct resolver URL.

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioResponses.kt`

**Changes:**

```kotlin
@Serializable
data class TorrentioStream(
    val name: String = "",
    val title: String = "",
    val infoHash: String = "",
    val fileIdx: Int = 0,
    val url: String? = null,
    @SerialName("behaviorHints")
    val behaviorHints: BehaviorHints? = null,
) {
    /** Extract infoHash from url query param or behaviorHints.bingeGroup */
    val resolvedInfoHash: String
        get() {
            if (infoHash.isNotEmpty()) return infoHash
            // Try to extract from bingeGroup (format: "torrentio|infoHash|fileIdx")
            behaviorHints?.bingeGroup?.split("|")?.getOrNull(1)?.let { return it }
            return ""
        }

    val resolvedFileIndex: Int
        get() {
            if (fileIdx > 0) return fileIdx
            behaviorHints?.bingeGroup?.split("|")?.getOrNull(2)?.toIntOrNull()?.let { return it }
            return 0
        }
}
```

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/torrentio/TorrentioClient.kt`

Update `toDomain()` mapper:

```kotlin
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
        resolverUrl = url,  // <-- NEW: map url to resolverUrl
    )
}
```

---

### 2.4 Stream Resolution Flow Fix (C2)

**Problem:** `StreamRepositoryImpl.resolveStream()` passes `option.magnetLink` to `unrestrict()`. This is wrong — `/unrestrict/link` expects a hoster URL, not a magnet. Additionally, the two-tier resolution approach is not implemented.

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/repository/StreamRepositoryImpl.kt`

**New implementation:**

```kotlin
class StreamRepositoryImpl @Inject constructor(
    private val torrentioClient: TorrentioClient,
    private val realDebridClient: RealDebridClient,
    private val httpClient: HttpClient,  // for resolver URL following
) : StreamRepository {

    override suspend fun resolveStream(option: StreamOption): StreamLink {
        // Tier 1: Resolver URL (fast path)
        option.resolverUrl?.let { resolverUrl ->
            return resolveViaUrl(resolverUrl)
        }
        // Tier 2: Full RD flow (fallback)
        return resolveViaRealDebrid(option)
    }

    private suspend fun resolveViaUrl(resolverUrl: String): StreamLink {
        // HTTP GET with redirect following — final URL is the direct MP4
        val response = httpClient.get(resolverUrl)
        val finalUrl = response.request.url.toString()
        val fileName = finalUrl.substringAfterLast('/').substringBefore('?')
        return StreamLink(
            id = finalUrl.hashCode().toString(),
            fileName = fileName,
            fileSize = 0,  // unknown from redirect
            hlsUrl = null,
            directUrl = finalUrl,
            quality = StreamQuality.FULL,
        )
    }

    private suspend fun resolveViaRealDebrid(option: StreamOption): StreamLink {
        // Step 1: Add magnet
        val magnetResponse = realDebridClient.addMagnet(option.magnetLink)
        val torrentId = magnetResponse.id

        // Step 2: Poll for torrent info (30s timeout, 2s interval)
        val torrentInfo = pollTorrentInfo(torrentId, timeoutMs = 30_000, intervalMs = 2_000)

        // Step 3: Select best video file
        val bestFile = realDebridClient.selectBestVideoFile(torrentInfo.files)
            ?: throw IllegalStateException("No video file found in torrent")
        realDebridClient.selectFiles(torrentId, listOf(bestFile.id))

        // Step 4: Re-fetch info to get links after file selection
        val updatedInfo = realDebridClient.getTorrentInfo(torrentId)
        val hosterLink = updatedInfo.links.firstOrNull()
            ?: throw IllegalStateException("No download link available")

        // Step 5: Unrestrict the hoster link (NOT the magnet)
        return realDebridClient.unrestrict(hosterLink)
    }

    private suspend fun pollTorrentInfo(
        torrentId: String,
        timeoutMs: Long,
        intervalMs: Long,
    ): TorrentInfoResponse {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val info = realDebridClient.getTorrentInfo(torrentId)
            when (info.status) {
                "downloaded" -> return info
                "waiting_files_selection" -> return info
                "magnet_error", "error", "virus", "dead" ->
                    throw IllegalStateException("Torrent failed: ${info.status}")
            }
            delay(intervalMs)
        }
        throw IllegalStateException("Torrent resolution timed out after ${timeoutMs}ms")
    }
}
```

**Error recovery (ExoPlayer transcode fallback):**

Add to `StreamRepository` interface:

```kotlin
suspend fun resolveTranscode(torrentId: String): StreamLink
```

Implementation:

```kotlin
override suspend fun resolveTranscode(torrentId: String): StreamLink {
    val transcode = realDebridClient.getTranscode(torrentId)
    val url = transcode.apple?.full
        ?: transcode.liveMP4?.full
        ?: throw IllegalStateException("No transcode available")
    return StreamLink(
        id = "transcode-$torrentId",
        fileName = "transcode.mp4",
        fileSize = 0,
        hlsUrl = if (url.contains(".m3u8")) url else null,
        directUrl = url,
        quality = StreamQuality.HIGH,
    )
}
```

**Retry policy:** 3 attempts on network errors with exponential backoff (1s, 2s, 4s).

---

### 2.5 Theme Color Fixes (M1-M4)

**File:** `core/ui/src/main/kotlin/studios/drible/tocabonito/core/ui/theme/ThemePalette.kt`

**Canarinho changes:**
- `surfaceElevated`: `Color(0xFFA6A500)` -> `Color(0xFFCCA600)`
- `textSecondary`: `Color(0xFF002466)` -> `Color(0xFF001A4D).copy(alpha = 0.7f)`
- `textTertiary`: `Color(0xFF003399)` -> `Color(0xFF001A4D).copy(alpha = 0.5f)`

**All themes `gradientBottom`:** append `.copy(alpha = 0.85f)` to each theme's `gradientBottom` value.

**Selecao Azul + DarkFlix `textSecondary`:** change `alpha = 0.75f` to `alpha = 0.7f`.

**Selecao Azul `secondary`:** `Color(0xFF009B3A)` -> `Color(0xFF009C3B)`.

**After changes, the corrected palettes:**

```kotlin
val Canarinho = ThemePalette(
    background = Color(0xFFFFD100),
    cardBackground = Color(0xFF001A4D),
    surfaceElevated = Color(0xFFCCA600),
    accent = Color(0xFF009B3A),
    secondary = Color(0xFF001A4D),
    textPrimary = Color(0xFF001A4D),
    textSecondary = Color(0xFF001A4D).copy(alpha = 0.7f),
    textTertiary = Color(0xFF001A4D).copy(alpha = 0.5f),
    gradientBottom = Color(0xFFFFD100).copy(alpha = 0.85f),
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
    textSecondary = Color.White.copy(alpha = 0.7f),
    textTertiary = Color.White.copy(alpha = 0.5f),
    gradientBottom = Color(0xFF001A4D).copy(alpha = 0.85f),
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
    textSecondary = Color.White.copy(alpha = 0.7f),  // was 0.75f — spec says only SelecaoAzul+DarkFlix
    textTertiary = Color.White.copy(alpha = 0.5f),
    gradientBottom = Color(0xFF002E1A).copy(alpha = 0.85f),
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
    textSecondary = Color.White.copy(alpha = 0.7f),
    textTertiary = Color.White.copy(alpha = 0.5f),
    gradientBottom = Color(0xFF141414).copy(alpha = 0.85f),
    silhouetteColor = Color(0xFFE31E26),
    isLight = false,
)
```

**Testing:** Update `ThemePaletteTest` assertions to match new values.

---

## 3. Track A: UI Parity

### 3.1 Home Screen (H1)

**Problem:** Home screen only shows `trending` and `continueWatching`. iOS shows a hero banner, popular movies, and popular series as well.

**Files:**
- `feature/catalog/src/main/kotlin/studios/drible/tocabonito/feature/catalog/home/HomeUiState.kt`
- `feature/catalog/src/main/kotlin/studios/drible/tocabonito/feature/catalog/home/HomeViewModel.kt`
- `feature/catalog/src/main/kotlin/studios/drible/tocabonito/feature/catalog/home/HomeScreen.kt`

**HomeUiState.Success expansion:**

```kotlin
data class Success(
    val heroItem: MediaItem? = null,
    val trending: List<MediaItem>,
    val popularMovies: List<MediaItem> = emptyList(),
    val popularSeries: List<MediaItem> = emptyList(),
    val continueWatching: List<WatchProgress>,
    val isRefreshing: Boolean = false,
) : HomeUiState()
```

**HomeViewModel.load() changes:**

```kotlin
private fun load() {
    viewModelScope.launch {
        try {
            coroutineScope {
                val trendingDeferred = async { catalogRepository.trending() }
                val popularMoviesDeferred = async { catalogRepository.popular(MediaType.MOVIE) }
                val popularSeriesDeferred = async { catalogRepository.popular(MediaType.SERIES) }

                val trending = trendingDeferred.await()
                val popularMovies = popularMoviesDeferred.await()
                val popularSeries = popularSeriesDeferred.await()

                setState {
                    HomeUiState.Success(
                        heroItem = trending.firstOrNull(),
                        trending = trending,
                        popularMovies = popularMovies,
                        popularSeries = popularSeries,
                        continueWatching = emptyList(),
                    )
                }
            }
        } catch (e: Exception) {
            setState { HomeUiState.Error(e.toUserMessage()) }
        }
    }
}
```

**New UI components (in `core/ui/src/main/kotlin/.../components/`):**

1. **`HeroBanner.kt`** — Full-width backdrop image (Coil `AsyncImage`), gradient overlay from transparent to `palette.gradientBottom`, title text, Play button (accent color, filled), Info button (outlined).

2. **`SilhouetteBackground.kt`** — A `Canvas`-based composable that draws a large blurred ellipse using `palette.silhouetteColor` at ~0.05 alpha. Applied behind the `LazyColumn` content on HomeScreen.

**UI rendering order in HomeScreen LazyColumn:**

```
item { HeroBanner(heroItem) }
if (continueWatching.isNotEmpty()) {
    item { SectionTitle("Continue Watching") }
    item { HorizontalMediaRow(continueWatching) }
}
item { SectionTitle("Trending") }
item { HorizontalMediaRow(trending) }
item { SectionTitle("Popular Movies") }
item { HorizontalMediaRow(popularMovies) }
item { SectionTitle("Popular Series") }
item { HorizontalMediaRow(popularSeries) }
```

---

### 3.2 Stream Filters (H2)

**Problem:** Stream list has no filters. iOS has horizontal filter chips for quality, source, codec, audio, and subtitles.

**Files:**
- `feature/detail/src/main/kotlin/studios/drible/tocabonito/feature/detail/DetailViewModel.kt`
- `feature/detail/src/main/kotlin/studios/drible/tocabonito/feature/detail/DetailUiState.kt`
- New: `feature/detail/src/main/kotlin/studios/drible/tocabonito/feature/detail/components/StreamFilterChips.kt`

**New model in DetailUiState or a separate file:**

```kotlin
data class StreamFilters(
    val quality: String? = null,      // null = All, "4K", "1080p", "720p"
    val source: String? = null,       // null = All, "WEB-DL", "BluRay", "WEBRip", "TS", "CAM"
    val codec: String? = null,        // null = All, "H265", "H264", "AV1"
    val audioLanguage: String? = null,
    val subtitleLanguage: String? = null,
)
```

**DetailUiState.Success adds:**

```kotlin
val filters: StreamFilters = StreamFilters(),
val filteredStreams: List<StreamOption> = streams,
```

**Filter logic (in ViewModel):**

```kotlin
private fun applyFilters(streams: List<StreamOption>, filters: StreamFilters): List<StreamOption> {
    return streams.filter { option ->
        val meta = option.metadata
        (filters.quality == null || option.quality.contains(filters.quality, ignoreCase = true)) &&
        (filters.source == null || meta.source.equals(filters.source, ignoreCase = true)) &&
        (filters.codec == null || meta.codec.equals(filters.codec, ignoreCase = true)) &&
        (filters.audioLanguage == null || meta.languages.any { it.equals(filters.audioLanguage, ignoreCase = true) }) &&
        (filters.subtitleLanguage == null || meta.subtitles.any { it.equals(filters.subtitleLanguage, ignoreCase = true) })
    }
}
```

**New intents:**

```kotlin
is DetailIntent.UpdateFilter -> updateFilter(intent.filters)
```

**StreamFilterChips composable:** Horizontal `LazyRow` with `FilterChip` composables. Each category is a dropdown chip (click toggles a dropdown menu with options). Active filters use `palette.accent` background.

---

### 3.3 Rich Stream Rows (H3)

**Problem:** Stream rows show minimal info. iOS shows quality badges, codec, source, seeders, size, audio/subtitle icons, and action buttons.

**New file:** `feature/detail/src/main/kotlin/studios/drible/tocabonito/feature/detail/components/StreamRow.kt`

**Component structure:**

```
Row {
    Column(weight = 1f) {
        Row { QualityBadge, SourceBadge, HdrBadge, CodecBadge }
        Text(title, style = tertiary, maxLines = 1)
        Row { SeederIndicator, AudioIcons, SubtitleIcons, SizeText }
    }
    Column {
        PlayButton(circle, accent)
        DownloadButton(state-aware icon)
    }
}
```

**Badge colors:**
- Quality: 4K = `Color(0xFF9C27B0)`, 1080p = `Color(0xFF1976D2)`, 720p = `Color(0xFF388E3C)`, SD = `Color(0xFF757575)`
- Seeder count color: green (`Color(0xFF4CAF50)`) if >100, yellow (`Color(0xFFFFC107)`) if >20, gray otherwise

**DownloadButton states:** Renders different icons based on `DownloadState`:
- `null`/idle: download arrow icon
- `RESOLVING`: circular progress (indeterminate)
- `DOWNLOADING`: circular progress with percentage
- `COMPLETED`: checkmark icon
- `FAILED`: retry icon

---

### 3.4 My List Button (H4)

**Problem:** iOS has a full-width "My List" button. Android uses a small heart icon.

**File:** `feature/detail/src/main/kotlin/studios/drible/tocabonito/feature/detail/DetailScreen.kt`

**Changes:**

1. Remove the small heart/favorite icon from the metadata row
2. Add a full-width button below the Play button:

```kotlin
Button(
    onClick = { onIntent(DetailIntent.ToggleFavorite) },
    colors = ButtonDefaults.buttonColors(
        containerColor = palette.surfaceElevated,
        contentColor = palette.textPrimary,
    ),
    modifier = Modifier.fillMaxWidth(),
) {
    Icon(
        imageVector = if (isFavorite) Icons.Default.Check else Icons.Default.Add,
        contentDescription = null,
    )
    Spacer(Modifier.width(8.dp))
    Text(if (isFavorite) "In My List" else "My List")
}
```

---

### 3.5 Download from Detail (H5)

**Problem:** No way to download a stream from the detail screen.

**Files:**
- `feature/detail/src/main/kotlin/studios/drible/tocabonito/feature/detail/DetailViewModel.kt`
- `feature/detail/src/main/kotlin/studios/drible/tocabonito/feature/detail/DetailUiState.kt`

**New intent:**

```kotlin
is DetailIntent.DownloadStream -> downloadStream(intent.option)
```

**New state in DetailUiState.Success:**

```kotlin
val downloadStates: Map<String, DownloadState> = emptyMap(),  // streamOption.id -> state
```

**ViewModel logic:**

```kotlin
private fun downloadStream(option: StreamOption) {
    // Large file warning (check option.size)
    val sizeGb = parseSizeToBytes(option.size)
    if (sizeGb > 2_000_000_000L) {
        updateSuccess { copy(pendingLargeDownload = option) }
        return
    }
    executeDownload(option)
}

private fun executeDownload(option: StreamOption) {
    updateDownloadState(option.id, DownloadState.RESOLVING)
    viewModelScope.launch {
        try {
            val link = streamRepository.resolveStream(option)
            val current = _state.value as? DetailUiState.Success ?: return@launch
            downloadRepository.save(
                DownloadItem(
                    id = UUID.randomUUID().toString(),
                    mediaId = current.mediaItem.id,
                    // ... fill from current media item + stream option
                    state = DownloadState.QUEUED,
                    // ...
                )
            )
            updateDownloadState(option.id, DownloadState.QUEUED)
            // WorkManager enqueue handled by DownloadRepository.save()
        } catch (e: Exception) {
            updateDownloadState(option.id, DownloadState.FAILED)
        }
    }
}
```

**Large file confirmation dialog:** When `pendingLargeDownload != null`, show AlertDialog:

```
Title: "Large Download"
Message: "This file is ${size}. Download over Wi-Fi?"
Positive: "Download" -> executeDownload(option), dismiss
Negative: "Cancel" -> dismiss
```

---

## 4. Track B: Features

### 4.1 API Key Settings (L6)

**Problem:** No way to enter/validate the RD API key at runtime. App only uses a compile-time BuildConfig value.

**Files:**
- `feature/settings/src/main/kotlin/studios/drible/tocabonito/feature/settings/SettingsScreen.kt`
- `feature/settings/src/main/kotlin/studios/drible/tocabonito/feature/settings/SettingsViewModel.kt`
- New: `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/preferences/ApiKeyStore.kt`

**ApiKeyStore:**

```kotlin
class ApiKeyStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val RD_API_KEY = stringPreferencesKey("rd_api_key")

    val apiKey: Flow<String?> = dataStore.data.map { it[RD_API_KEY] }

    suspend fun save(key: String) {
        dataStore.edit { it[RD_API_KEY] = key }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(RD_API_KEY) }
    }

    /**
     * Returns the effective API key:
     * 1. Runtime-stored key (always preferred)
     * 2. Debug-only: BuildConfig.RD_API_KEY fallback
     */
    suspend fun effectiveKey(): String? {
        val stored = apiKey.first()
        if (stored != null) return stored
        return if (BuildConfig.DEBUG) BuildConfig.RD_API_KEY.takeIf { it.isNotBlank() } else null
    }
}
```

**Settings screen "Real-Debrid" section:**
- `OutlinedTextField` for API key (password visual transformation)
- "Validate" button: calls `realDebridClient.getUser()` with the entered key
- On success: display username, plan type, days remaining in a Card below
- On failure: show error Snackbar
- "Save" stores validated key in DataStore

**Torrentio Advanced section:**
- Multi-select chip group for providers (pre-filled from `TorrentioConfig.providers`)
- Language dropdown (default "portuguese")
- Stored in DataStore, read by `TorrentioClient`'s config provider

---

### 4.2 OpenSubtitles (L2)

**Problem:** No subtitle support. iOS searches OpenSubtitles by IMDB ID and sideloads subtitles.

**New package:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/api/opensubtitles/`

**Files:**
- `OpenSubtitlesClient.kt`
- `OpenSubtitlesResponses.kt`

**Endpoints:**

| Method | Path | Headers | Purpose |
|--------|------|---------|---------|
| `POST` | `/login` | `Api-Key` | Optional user login for higher rate limits |
| `GET` | `/subtitles?imdb_id={id}&languages={lang}` | `Api-Key`, optional `Authorization` | Search subtitles |
| `POST` | `/download` | `Api-Key`, optional `Authorization` | Get download link for subtitle file |

**Configuration:**
- App-level API key in `BuildConfig.OPENSUBTITLES_API_KEY` (registered with OpenSubtitles for Movies & TV)
- Optional user credentials stored encrypted in DataStore (Settings screen section)
- Base URL: `https://api.opensubtitles.com/api/v1`

**Player integration:**

In `feature/player`, when playback starts:
1. Query OpenSubtitles by IMDB ID + preferred language (from settings, default "pob" for Brazilian Portuguese)
2. Download best-matching SRT/VTT file to cache directory
3. Create `MergingMediaSource` combining the video source with a `SingleSampleMediaSource` for the subtitle file
4. Expose subtitle tracks in ExoPlayer's `TrackSelectionDialog`

**Domain model:**

```kotlin
// In core/domain/model/SubtitleTrack.kt (already exists)
data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val url: String?,          // download URL
    val localPath: String?,    // after download
    val isForced: Boolean = false,
)
```

---

### 4.3 Firebase Sync (L3)

**Problem:** Sync services are stubs (`StubFavoritesSyncService`, `StubProgressSyncService`, `StubSettingsSyncService`). No actual cloud sync.

**New module or package:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/firebase/`

**Dependencies (add to app `build.gradle.kts`):**
- `com.google.firebase:firebase-auth-ktx`
- `com.google.firebase:firebase-firestore-ktx`
- `com.google.android.gms:play-services-auth`

**Implementation plan:**

1. **Google Sign-In:** Use Firebase Auth with Google provider. Add sign-in button in Settings. On success, store UID.

2. **Firestore schema:**

```
users/{uid}/
  favorites/{mediaId}  -> { mediaId, mediaType, title, posterPath, addedAt }
  progress/{mediaId}   -> { mediaId, positionMs, durationMs, updatedAt, episodeId? }
  settings/app         -> { theme, language, torrentioProviders[], openSubsCredentials? }
```

3. **Sync implementations** (replace stubs):

```kotlin
class FirebaseFavoritesSyncService(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : FavoritesSyncService {
    override suspend fun push(favorites: List<FavoriteEntity>) { ... }
    override suspend fun pull(): List<FavoriteEntity> { ... }
    override suspend fun sync() { /* merge with last-write-wins */ }
}
```

4. **Conflict resolution:** Last-write-wins using `updatedAt` timestamp field on each document.

5. **Sync triggers:**
   - App launch (pull)
   - After favorite toggle (push)
   - After playback pause/stop (push progress)
   - After settings change (push settings)

6. **Offline:** Firestore SDK handles local caching. No custom offline logic needed.

7. **Settings toggle:** "Sync to Cloud" boolean in Settings — when off, sync services behave as no-op.

---

### 4.4 Splash Screen (L1)

**Problem:** App shows a blank yellow screen on cold start. No branded splash.

**Files:**
- `app/src/main/kotlin/studios/drible/tocabonito/MainActivity.kt`
- New: `app/src/main/res/values/splash.xml`

**Implementation:**

1. Add dependency: `androidx.core:core-splashscreen`

2. In `MainActivity.onCreate()`:

```kotlin
val splashScreen = installSplashScreen()
splashScreen.setKeepOnScreenCondition {
    // Keep splash until first data loads
    homeViewModel.state.value is HomeUiState.Loading
}
```

3. Theme configuration in `res/values/themes.xml`:

```xml
<style name="Theme.TocaBonito.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/splash_background</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
    <item name="postSplashScreenTheme">@style/Theme.TocaBonito</item>
</style>
```

4. In `AndroidManifest.xml`, set the splash theme on the launcher activity.

---

### 4.5 SilhouetteBackground (L4)

**Problem:** iOS has a subtle decorative layer behind content using the theme's silhouette color.

**New file:** `core/ui/src/main/kotlin/studios/drible/tocabonito/core/ui/components/SilhouetteBackground.kt`

**Implementation:**

```kotlin
@Composable
fun SilhouetteBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalThemePalette.current
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = palette.silhouetteColor.copy(alpha = 0.04f),
                radius = size.minDimension * 0.8f,
                center = Offset(size.width * 0.7f, size.height * 0.15f),
            )
            drawCircle(
                color = palette.silhouetteColor.copy(alpha = 0.03f),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * 0.2f, size.height * 0.6f),
            )
        }
        content()
    }
}
```

**Usage:** Wrap the `LazyColumn` in `HomeScreen` and `DetailScreen` with `SilhouetteBackground`.

---

### 4.6 Format Guide (L5)

**Problem:** Users may not understand stream quality tiers, codecs, or source types.

**New files:**
- `feature/detail/src/main/kotlin/studios/drible/tocabonito/feature/detail/FormatGuideScreen.kt`
- Navigation route added to `DetailNavigation.kt`

**Content (static, no API calls):**

Sections:
1. **Quality Tiers** — 4K (2160p), 1080p (Full HD), 720p (HD), SD (480p/360p)
2. **Codecs** — H.265/HEVC (better compression, newer devices), H.264/AVC (universal), AV1 (bleeding edge)
3. **Sources** — WEB-DL (streaming rip, best), BluRay (disc rip), WEBRip (screen capture), TS/CAM (theater recording, avoid)
4. **Audio** — Dual Audio, multi-language flags
5. **HDR** — HDR10, Dolby Vision, HDR10+

**Access:** Info icon button (`Icons.Outlined.Info`) next to "Available Streams" section title in DetailScreen.

---

### 4.7 Player Enhancements (L7)

**Problem:** Player lacks audio track selection, subtitle selection, progress sync, and error recovery.

**File:** `feature/player/src/main/kotlin/studios/drible/tocabonito/feature/player/` (exact files TBD based on current player implementation)

**Additions:**

1. **Audio track selection:** Use ExoPlayer's built-in `TrackSelectionDialog` or a custom bottom sheet listing available audio tracks from `player.currentTracks`.

2. **Subtitle track selection:** After OpenSubtitles integration loads sideloaded tracks, present them alongside embedded tracks in the same dialog.

3. **Playback progress sync:**
   - Save to Room DB: every 10 seconds during playback + on pause/stop
   - Save to Firestore: on pause/stop (debounced, not every 10s)
   - Data: `{ mediaId, episodeId?, positionMs, durationMs, updatedAt }`

4. **Transcode error recovery:**

```kotlin
player.addListener(object : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
        if (retryCount < MAX_RETRIES && torrentId != null) {
            retryCount++
            viewModelScope.launch {
                val transcodeLink = streamRepository.resolveTranscode(torrentId)
                val newSource = buildMediaSource(transcodeLink)
                player.setMediaSource(newSource)
                player.prepare()
                player.play()
            }
        } else {
            // Show error to user
        }
    }
})
```

---

## 5. Module Impact

### Modified modules:

| Module | Changes |
|--------|---------|
| `core/data` | `TorrentioClient` (config path), `TorrentioResponses` (url field), `RealDebridClient` (5 new endpoints + response models), `StreamRepositoryImpl` (two-tier resolution), new `ApiKeyStore`, new `opensubtitles/` package, new `sync/firebase/` package |
| `core/domain` | `StreamRepository` interface (add `resolveTranscode`), `SubtitleTrack` (already exists, verify fields) |
| `core/ui` | `ThemePalette` (color fixes), new `HeroBanner`, new `StreamFilterChips`, new `StreamRow`, new `SilhouetteBackground` |
| `feature/catalog` | `HomeViewModel` (parallel loads), `HomeUiState` (hero, popular), `HomeScreen` (new layout) |
| `feature/detail` | `DetailViewModel` (filters, downloads, large file dialog), `DetailUiState` (filters, downloadStates), `DetailScreen` (My List button, filter chips, rich rows), new `FormatGuideScreen` |
| `feature/player` | Subtitle sideloading, progress sync, audio/subtitle track selection, transcode error recovery |
| `feature/settings` | API key entry/validation, Torrentio provider config, OpenSubtitles credentials, sync toggle, Google Sign-In |
| `feature/downloads` | Wire download triggers from detail, progress UI updates |
| `app` | Splash screen setup, Firebase initialization, new Hilt modules for Firebase/OpenSubtitles |

### New packages (not new Gradle modules):

- `core/data/.../api/opensubtitles/` — OpenSubtitles API client
- `core/data/.../sync/firebase/` — Firebase implementations replacing stubs
- `core/data/.../preferences/ApiKeyStore.kt` — Encrypted key storage

### New Gradle dependencies:

- `androidx.core:core-splashscreen:1.0.1`
- `com.google.firebase:firebase-bom` (Auth, Firestore)
- `com.google.android.gms:play-services-auth`
- No new Gradle modules needed (keep it flat)

---

## 6. Data Flow Diagrams

### Stream Resolution Flow

```
User taps Play on StreamRow
  -> DetailViewModel.onIntent(ResolveStream(option))
    -> updateSuccess { copy(isResolvingStream = true) }
    -> streamRepository.resolveStream(option)

      [Tier 1: Resolver URL — fast path]
      if option.resolverUrl != null:
        HTTP GET option.resolverUrl (Ktor followRedirects = true)
        -> final response URL = direct MP4 link
        -> return StreamLink(directUrl = finalUrl)

      [Tier 2: Full RD flow — fallback]
      else:
        realDebridClient.addMagnet(option.magnetLink)
          -> AddMagnetResponse { id = "TORRENT_ID" }
        loop (2s interval, 30s timeout):
          realDebridClient.getTorrentInfo("TORRENT_ID")
          if status == "downloaded" || "waiting_files_selection" -> break
          if status in error states -> throw
        selectBestVideoFile(info.files)
          -> filter by video extensions, pick largest
        realDebridClient.selectFiles("TORRENT_ID", [fileId])
        realDebridClient.getTorrentInfo("TORRENT_ID")
          -> links[0] = hoster URL
        realDebridClient.unrestrict(links[0])
          -> UnrestrictResponse { link = direct MP4 URL }
        return StreamLink(directUrl = link)

    -> updateSuccess { copy(isResolvingStream = false, resolvedLink = link) }
    -> navigate to Player with StreamLink

      [Error recovery: ExoPlayer fails]
      Player.Listener.onPlayerError:
        realDebridClient.getTranscode("TORRENT_ID")
        -> prefer apple.full (HLS), fallback liveMP4.full
        -> retry playback with transcode URL
```

### Download Flow

```
User taps Download icon on StreamRow
  -> DetailViewModel.onIntent(DownloadStream(option))
    -> if option.size > 2GB:
        show confirmation dialog
        -> user confirms -> continue
        -> user cancels -> abort
    -> updateDownloadState(option.id, RESOLVING)
    -> streamRepository.resolveStream(option)  [same two-tier as above]
      -> StreamLink { directUrl, fileName, fileSize }
    -> downloadRepository.save(DownloadItem {
        id = UUID,
        mediaId = currentMedia.id,
        state = QUEUED,
        totalBytes = link.fileSize,
        ...
      })
    -> updateDownloadState(option.id, QUEUED)

    [Background — WorkManager]
    DownloadWorker.doWork():
      -> setForeground(ForegroundInfo(notification))
      -> downloadRepository.updateState(id, DOWNLOADING)
      -> HTTP GET directUrl with Range header support
        -> stream bytes to app-internal file
        -> every 1MB or 5s: update progress in Room + notification
      -> on complete:
        downloadRepository.updateState(id, VERIFYING)
        IntegrityVerifier.verify(file)  [already exists]
        downloadRepository.updateState(id, COMPLETED)
        downloadRepository.updateLocalPath(id, filePath)
      -> on failure:
        downloadRepository.updateState(id, FAILED)
        downloadRepository.updateError(id, failureCount++, error.message)
        if failureCount < 3: Result.retry()
        else: Result.failure()
```

### Firebase Sync Flow

```
App Launch:
  -> FirebaseAuth.currentUser != null?
    -> yes: pull favorites, progress, settings from Firestore
           merge with local Room DB (last-write-wins by timestamp)
    -> no: skip sync, use local only

User toggles favorite:
  -> FavoritesRepositoryImpl.add/remove(mediaItem)
    -> Room: insert/delete
    -> if syncEnabled && user signed in:
        FirebaseFavoritesSyncService.push([updated favorite])
        -> Firestore: set/delete document

Player pauses/stops:
  -> ProgressRepositoryImpl.save(progress)
    -> Room: upsert WatchProgressEntity
    -> if syncEnabled && user signed in:
        FirebaseProgressSyncService.push(progress)
        -> Firestore: set document (merge)
```

---

## 7. Testing Strategy

> Aligned with ADR-0008 (Drible-Studios/tocabonito-specs). No mock-heavy unit tests. Prefer fakes (`:core:testing`) over mocks. Property-based tests for data transformations.

### Testing Environments

| Environment | What runs there | Why |
|-------------|----------------|-----|
| **NVIDIA Shield** (192.168.2.109:5555) | Real endpoint tests (Torrentio, Real-Debrid), E2E smoke tests, playback validation | Real API keys, real network, real hardware decoders — only place to validate streaming pipeline end-to-end |
| **Android TV emulator** (local Mac, Android TV image) | Everything else: UI tests, integration tests with MockWebServer, Compose tests, screenshot tests, database tests | Fast iteration, no network dependency, deterministic |

### Layer-by-Layer (per ADR-0008)

| Layer | Test type | Tools | Runs on |
|-------|-----------|-------|---------|
| `:core:domain` | Property-based | Kotest property testing, generators | JVM (no device) |
| `:core:data` (network) | Contract tests | JSON fixtures from real API responses, kotlinx.serialization | JVM (no device) |
| `:core:data` (database) | Integration | Room in-memory + Turbine for Flow | Android TV emulator |
| `:core:data` (repos) | Integration | Real Room + fake network (fakes from `:core:testing`) | Android TV emulator |
| `:feature:*` | UI (screenshot) | Paparazzi (zero-device) | JVM (no device) |
| `:feature:*` | Compose UI tests | Compose test rules | Android TV emulator |
| `:app` | E2E smoke tests | Maestro (3-5 critical user journeys) | Android TV emulator |
| `:app` | Real endpoint E2E | Maestro + real API keys | **NVIDIA Shield** |

### Contract Tests (JVM — no device needed)

| Area | Test Class | What to verify |
|------|-----------|----------------|
| RealDebrid endpoints | `RealDebridClientContractTest` | Each endpoint serializes/deserializes correctly against real JSON fixtures, video file selection picks largest |
| Torrentio responses | `TorrentioClientContractTest` | URL includes config path, `url` field parsed, maps to `resolverUrl` |
| Stream resolution logic | `StreamResolutionTest` | Tier 1 (resolver URL) returns direct link, Tier 2 (RD flow) handles polling + unrestrict, timeout throws |
| Stream filters | `StreamFilterTest` | Each filter type works independently and combined, property-based with Kotest |
| Stream metadata parser | `StreamMetadataParserTest` | Property-based: random title strings → parsed metadata is consistent |
| OpenSubtitles | `OpenSubtitlesClientTest` | Search by IMDB ID, download link extraction |

### Integration Tests (Android TV emulator)

| Test | Setup | Verification |
|------|-------|--------------|
| Full stream resolution | MockWebServer for Torrentio + RD | End-to-end: search → resolve → get direct URL (both tiers) |
| Download end-to-end | MockWebServer + in-memory Room | Enqueue → download → verify file exists + Room state transitions |
| Firebase sync round-trip | Firebase emulator suite | Push favorites → pull on fresh instance → verify match |
| Progress persistence | Room in-memory + Turbine | Save progress → observe continue watching → emits correct items |

### UI Tests (Android TV emulator — Compose test rules)

| Screen | Tests |
|--------|-------|
| HomeScreen | Hero banner renders, all 5 sections visible, pull-to-refresh works |
| DetailScreen | Filter chips toggle streams, My List button state changes, download button states |
| SettingsScreen | API key field validates, provider chips toggle, Google Sign-In button present |
| DownloadsScreen | Progress bar updates, completed items show play button |
| FormatGuideScreen | All sections render, navigation back works |

### Screenshot Tests (JVM — Paparazzi, zero-device)

| Component | Variants |
|-----------|----------|
| HeroBanner | All 4 themes |
| StreamRow | Quality badges (4K/1080p/720p/SD), download states (idle/downloading/complete/failed) |
| StreamFilterChips | Active/inactive states |
| ThemePalette | Full-screen color swatch for each theme (regression guard for M1-M4 fixes) |

### Real Endpoint Tests (NVIDIA Shield ONLY)

These tests hit **real Torrentio and Real-Debrid APIs** with real API keys. They validate the actual streaming pipeline works end-to-end on production infrastructure.

| Test | Steps | Validates |
|------|-------|-----------|
| Torrentio config path | Search known IMDB ID → verify response has `url` field populated | C1 fix: config path includes RD key |
| Resolver URL fast path | Get stream with `url` → HTTP GET → verify 302 redirect to RD domain | C2 Tier 1: resolver URL resolution |
| Full RD flow | Get stream without `url` → addMagnet → poll → selectFiles → unrestrict → verify direct URL responds 200 | C2 Tier 2: full client-side resolution |
| Playback | Resolve stream → feed to ExoPlayer → verify `STATE_READY` within 10s | End-to-end playback works on Shield hardware |
| Transcode fallback | Feed known-bad URL to ExoPlayer → verify transcode retry triggers → new URL plays | Error recovery path |
| Download pipeline | Resolve → enqueue download → verify file appears + is playable offline | H5 download E2E |

**Run with:** `adb -s 192.168.2.109:5555 shell am instrument -w studios.drible.tocabonito.test/...`

### Maestro E2E Smoke Tests (3-5 critical journeys)

| Journey | Environment | Steps |
|---------|-------------|-------|
| Browse → Detail → Play | Shield | Launch → tap trending item → tap Play → verify player visible |
| Search → Stream → Download | Shield | Search "Interstellar" → tap result → tap Download on stream → verify notification |
| Settings → API Key | Android TV emulator | Open Settings → enter key → Validate → verify account info shown |
| Theme switching | Android TV emulator | Settings → change theme → verify Home screen colors update |
| Offline playback | Shield | Downloads tab → tap completed item → verify player starts |

### Test Data & Fixtures

- **Known IMDB IDs for Shield tests:** Use well-cached popular titles (e.g., `tt0816692` Interstellar) that are reliably available on RD
- **JSON fixtures** (for contract tests): Captured from real API responses, stored in `core/testing/src/main/resources/fixtures/`
- **Fakes** (`:core:testing`): `FakeCatalogRepository`, `FakeStreamRepository`, `FakeRealDebridClient`, `FakeTorrentioClient`, etc.

---

## 8. Out of Scope

- **Multi-user / account switching** — Single Google account per device is sufficient
- **Chromecast support** — Shield HDMI output covers the TV use case
- **TV-specific D-pad navigation optimization** — Shield remote works with standard Compose focus handling; no leanback library needed
- **Torrent health monitoring** — Display seeder count is enough; no real-time tracking
- **Parental controls** — Not in iOS app either
- **In-app rating/review prompts** — Not a parity issue
- **VLC player fallback** — Decided against; transcode handles codec edge cases
- **Anonymous Firebase Auth** — Only Google Sign-In (simpler, no orphan accounts)
- **OpenSubtitles user login requirement** — App-level API key is sufficient for basic use; user login is optional for higher limits
