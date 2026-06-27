package studios.drible.tocabonito.core.data.sync

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.repository.FavoritesRepository
import studios.drible.tocabonito.core.domain.repository.ProgressRepository

// — Fakes ——————————————————————————————————————————————————————————————————

private class FakeFavoritesRepository(initialItems: List<MediaItem> = emptyList()) : FavoritesRepository {
    val items = initialItems.toMutableList()

    override fun observeAll(): Flow<List<MediaItem>> = flowOf(items.toList())
    override suspend fun add(item: MediaItem) { items.add(item) }
    override suspend fun remove(id: String) { items.removeAll { it.id == id } }
    override fun observeIsFavorite(id: String): Flow<Boolean> = flowOf(items.any { it.id == id })
}

private class FakeProgressRepository(initialProgress: List<WatchProgress> = emptyList()) : ProgressRepository {
    val records = initialProgress.toMutableList()

    override suspend fun save(progress: WatchProgress) { records.add(progress) }
    override suspend fun get(mediaId: String, episodeId: String?): WatchProgress? =
        records.firstOrNull { it.mediaItem.id == mediaId && it.episodeId == episodeId }
    override fun observeContinueWatching(): Flow<List<WatchProgress>> = flowOf(records.toList())
    override suspend fun markFinished(mediaId: String, episodeId: String?) {
        records.removeAll { it.mediaItem.id == mediaId && it.episodeId == episodeId }
    }
}

// — Helpers ————————————————————————————————————————————————————————————————

private fun mediaItem(id: String = "tt123", title: String = "Movie A") = MediaItem(
    id = id,
    title = title,
    overview = "Overview",
    posterPath = null,
    backdropPath = null,
    mediaType = MediaType.MOVIE,
    releaseYear = 2024,
    voteAverage = 7.5,
    genreIds = listOf(28),
)

private fun watchProgress(mediaId: String = "tt123", episodeId: String? = null) = WatchProgress(
    id = "$mediaId:$episodeId",
    mediaItem = mediaItem(id = mediaId),
    currentTime = 300.0,
    duration = 5400.0,
    lastWatched = 1_700_000_000L,
    episodeId = episodeId,
)

// — Tests —————————————————————————————————————————————————————————————————

class DataPortabilityServiceImplTest : FunSpec({

    test("exportAll includes all favorites") {
        val fav1 = mediaItem("tt001", "Film One")
        val fav2 = mediaItem("tt002", "Film Two")
        val favRepo = FakeFavoritesRepository(listOf(fav1, fav2))
        val progressRepo = FakeProgressRepository()
        val service = DataPortabilityServiceImpl(favRepo, progressRepo)

        val backup = service.exportAll()

        backup.favorites.size shouldBe 2
        backup.favorites.map { it.mediaId } shouldBe listOf("tt001", "tt002")
    }

    test("exportAll includes all progress records") {
        val favRepo = FakeFavoritesRepository()
        val p1 = watchProgress("tt001")
        val p2 = watchProgress("tt002", episodeId = "s01e01")
        val progressRepo = FakeProgressRepository(listOf(p1, p2))
        val service = DataPortabilityServiceImpl(favRepo, progressRepo)

        val backup = service.exportAll()

        backup.watchProgress.size shouldBe 2
        backup.watchProgress.map { it.mediaId } shouldBe listOf("tt001", "tt002")
    }

    test("exportAll sets backup version to 1 and sourceProvider") {
        val service = DataPortabilityServiceImpl(FakeFavoritesRepository(), FakeProgressRepository())

        val backup = service.exportAll()

        backup.version shouldBe 1
        backup.sourceProvider shouldBe "tocabonito-android"
    }

    test("importAll adds new favorites and returns correct count") {
        val favRepo = FakeFavoritesRepository()
        val progressRepo = FakeProgressRepository()
        val service = DataPortabilityServiceImpl(favRepo, progressRepo)

        val backup = service.exportAll().copy(
            favorites = listOf(
                mediaItem("tt001").toPortableForTest(),
                mediaItem("tt002").toPortableForTest(),
            ),
        )

        val result = service.importAll(backup)

        result.favoritesImported shouldBe 2
        result.skippedDuplicates shouldBe 0
        favRepo.items.size shouldBe 2
    }

    test("importAll skips duplicate favorites") {
        val existing = mediaItem("tt001")
        val favRepo = FakeFavoritesRepository(listOf(existing))
        val progressRepo = FakeProgressRepository()
        val service = DataPortabilityServiceImpl(favRepo, progressRepo)

        val backup = service.exportAll().copy(
            favorites = listOf(
                mediaItem("tt001").toPortableForTest(), // duplicate
                mediaItem("tt002").toPortableForTest(), // new
            ),
        )

        val result = service.importAll(backup)

        result.favoritesImported shouldBe 1
        result.skippedDuplicates shouldBe 1
    }

    test("importAll adds new progress records and returns correct count") {
        val favRepo = FakeFavoritesRepository()
        val progressRepo = FakeProgressRepository()
        val service = DataPortabilityServiceImpl(favRepo, progressRepo)

        val backup = service.exportAll().copy(
            watchProgress = listOf(
                watchProgress("tt001").toPortableForTest(),
                watchProgress("tt002", "s01e01").toPortableForTest(),
            ),
        )

        val result = service.importAll(backup)

        result.progressRecordsImported shouldBe 2
        progressRepo.records.size shouldBe 2
    }

    test("importAll skips duplicate progress records") {
        val existing = watchProgress("tt001")
        val favRepo = FakeFavoritesRepository()
        val progressRepo = FakeProgressRepository(listOf(existing))
        val service = DataPortabilityServiceImpl(favRepo, progressRepo)

        val backup = service.exportAll().copy(
            watchProgress = listOf(
                watchProgress("tt001").toPortableForTest(),  // duplicate
                watchProgress("tt003").toPortableForTest(),  // new
            ),
        )

        val result = service.importAll(backup)

        result.progressRecordsImported shouldBe 1
        result.skippedDuplicates shouldBe 1
    }

    test("importAll returns settingsImported as zero (not yet implemented)") {
        val service = DataPortabilityServiceImpl(FakeFavoritesRepository(), FakeProgressRepository())
        val backup = service.exportAll()

        val result = service.importAll(backup)

        result.settingsImported shouldBe 0
    }
})

// — Portable converters used only in tests —————————————————————————————————

private fun MediaItem.toPortableForTest() = studios.drible.tocabonito.core.domain.service.PortableFavorite(
    mediaId = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    mediaType = mediaType.name,
    releaseYear = releaseYear,
    voteAverage = voteAverage,
    genreIds = genreIds,
    dateAdded = System.currentTimeMillis(),
)

private fun WatchProgress.toPortableForTest() = studios.drible.tocabonito.core.domain.service.PortableWatchProgress(
    mediaId = mediaItem.id,
    episodeId = episodeId,
    currentTime = currentTime,
    duration = duration,
    lastWatched = lastWatched,
    mediaTitle = mediaItem.title,
    mediaType = mediaItem.mediaType.name,
)
