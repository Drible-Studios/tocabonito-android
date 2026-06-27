# ADR-0001: Shield Deployment Fixes — Image Loading and TMDB Auth

## Status
Accepted

## Date
2026-06-27

## Context

First deployment to Nvidia Shield TV (Android TV, API 34) exposed three bugs that didn't surface in unit tests or emulator builds without a valid TMDB API key:

1. **Poster images not loading** — app showed colored card backgrounds instead of movie posters
2. **TMDB API auth failing** — serialization error on response parsing
3. **API keys not reaching BuildConfig** — `findProperty()` wasn't reading `local.properties`

## Decisions

### 1. Coil: Use OkHttp network fetcher, not Ktor

**Problem:** `coil-network-ktor3` with `KtorNetworkFetcherFactory()` silently failed to load images on the Shield. No errors in logcat, no exceptions — just empty/transparent image results. The TMDB CDN (`image.tmdb.org`) was reachable (verified via ping from device).

**Root cause:** Coil 3's Ktor network fetcher creates its own internal `HttpClient` which, on certain Android TV runtimes, didn't properly resolve the OkHttp engine from the classpath. The default `HttpClient()` constructor picks an engine via service loader which is unreliable on Android TV's restricted ClassLoader.

**Fix:** Switched to `coil-network-okhttp` which uses OkHttp directly (no engine resolution needed). OkHttp is already on the classpath via Ktor's OkHttp engine, so no new dependency is introduced.

```kotlin
// TocaBonitoApp.kt
class TocaBonitoApp : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }
}
```

**Trade-off:** We now have two HTTP stacks — OkHttp (Coil images) and Ktor/OkHttp (API calls). This is fine because Coil manages its own connection pool and cache independently from the API client.

### 2. TMDB: Bearer token in Authorization header, not api_key query param

**Problem:** The TMDB API key provided is a v4 "read access token" (JWT, starts with `eyJ...`). The original implementation passed it as `?api_key=<token>` query parameter, which is the v3 API key pattern. v4 tokens must be sent as `Authorization: Bearer <token>`.

**Symptom on Shield:** The TMDB API returned an error JSON (likely `{"success": false, "status_code": 7}`) which kotlinx.serialization tried to deserialize as `TMDBPageResponse`, failing with "Fields [page, results, total_pages] are required but missing at path: $".

**Fix:** Changed all `TMDBClient` methods from:
```kotlin
parameter("api_key", apiKey)
```
to:
```kotlin
header("Authorization", "Bearer $apiKey")
```

**Why this wasn't caught earlier:** Unit tests use MockEngine with fixture JSONs, so the auth mechanism was never tested against the real TMDB API. The emulator builds on the dev machine couldn't reach TMDB due to the corporate SSL proxy.

### 3. API keys: gradle.properties, not local.properties

**Problem:** `project.findProperty("TMDB_API_KEY")` returns `null` when the key is in `local.properties`. Gradle's `findProperty()` reads from `gradle.properties` (project + user-level) and system properties, NOT from `local.properties`. The `local.properties` file is only auto-parsed by AGP for `sdk.dir`.

**Attempted alternatives:**
- Loading `local.properties` manually with `java.util.Properties()` inside `defaultConfig {}` → fails with Gradle configuration cache errors (can't use `File.inputStream()` in configuration phase)
- Using `providers.gradleProperty()` → same limitation

**Fix:** API keys go in `gradle.properties` (which is gitignored for these values via `.gitignore` pattern). `findProperty()` reads them natively with no configuration cache issues.

```properties
# gradle.properties
TMDB_API_KEY=eyJ...
REAL_DEBRID_API_KEY=XEC...
```

**Security note:** `gradle.properties` is NOT committed to git (the API key lines are developer-local). The committed version only has Gradle JVM args and Android settings.

## Consequences

- Images load reliably on Shield via OkHttp (tested: Trending row shows movie posters)
- TMDB API works with v4 Bearer tokens (tested: 20 trending items returned)
- API keys injected into BuildConfig at compile time via `findProperty()`
- Deep links work (`tocabonito://detail/{id}/{type}` navigates to detail screen)
- No changes needed to unit tests (MockEngine fixtures still work identically)

## Verification

```bash
# From dev Mac
adb connect 192.168.2.109:5555
adb -s 192.168.2.109:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.2.109:5555 shell am start -n studios.drible.tocabonito/.MainActivity
# Wait 10s, take screenshot
adb -s 192.168.2.109:5555 exec-out screencap -p > /tmp/shield-verify.png
# Verify: "Trending" header visible + poster images loaded
```
