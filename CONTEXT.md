# TocaBonito Android — Context

## What This Is

Native Android app for streaming via RealDebrid, built with Kotlin/Jetpack Compose. Port of the iOS TocaBonito app. Targets Nvidia Shield TV as the primary device.

## Architecture

Multi-module Gradle project (11 modules):
- `:app` — Hilt DI, navigation host, entry point
- `:core:domain` — pure Kotlin entities + repository interfaces
- `:core:data` — Ktor clients, Room DB, repository implementations
- `:core:ui` — Compose theme system, shared components
- `:core:testing` — fakes for all interfaces
- `:feature:catalog` — Home, Explore, Search screens
- `:feature:detail` — Movie/series detail with stream resolution
- `:feature:player` — Media3 video player
- `:feature:downloads` — Download management UI
- `:feature:settings` — Theme picker, sync, data export
- `:feature:mylist` — Favorites grid

Pattern: MVI (Model-View-Intent) with StateFlow. Fully reactive pipeline (Room → Flow → Repository → Flow → ViewModel → Compose).

## External Services

| Service | Auth Method | Used For |
|---------|------------|----------|
| TMDB v3 API | Bearer token (v4 read access) in `Authorization` header | Movie/series metadata, posters, search |
| Torrentio | No auth | Stream discovery (magnet links) |
| RealDebrid | Bearer token in `Authorization` header | Stream resolution (magnet → direct URL) |
| OpenSubtitles/Stremio | No auth | External subtitles |

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home gradle :app:assembleDebug
```

JDK 17 required — Android Gradle Plugin's `jlink` step fails on JDK 18+.

## API Keys

Stored in `gradle.properties` (gitignored via the API key lines pattern):
```properties
TMDB_API_KEY=<v4 read access Bearer token>
REAL_DEBRID_API_KEY=<RD API token>
```

Read via `project.findProperty("TMDB_API_KEY")` in `app/build.gradle.kts` → injected as `BuildConfig.TMDB_API_KEY`.

## Deploy to Shield

```bash
adb connect 192.168.2.109:5555
adb -s 192.168.2.109:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.2.109:5555 shell am start -n studios.drible.tocabonito/.MainActivity
```

Shield discovered via network scan on port 5555 (ADB over WiFi must be enabled in Shield developer settings).

## Image Loading

Uses Coil 3 with `coil-network-okhttp` (not ktor). The `TocaBonitoApp` Application class implements `SingletonImageLoader.Factory` with `OkHttpNetworkFetcherFactory()`. See ADR-0001 for why.

## Theme System

4 themes: Canarinho (yellow/light), Seleção Azul (blue/dark), Joga Bonito (green/dark), DarkFlix (black/dark). Persisted to SharedPreferences. Immediate switching via `ThemeProvider.selectTheme()`.
