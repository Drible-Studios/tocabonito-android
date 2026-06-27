package studios.drible.tocabonito.core.data.sync

import kotlinx.coroutines.flow.first
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.repository.FavoritesRepository
import studios.drible.tocabonito.core.domain.repository.ProgressRepository
import studios.drible.tocabonito.core.domain.service.DataPortabilityService
import studios.drible.tocabonito.core.domain.service.ImportResult
import studios.drible.tocabonito.core.domain.service.PortableBackup
import studios.drible.tocabonito.core.domain.service.PortableFavorite
import studios.drible.tocabonito.core.domain.service.PortableWatchProgress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataPortabilityServiceImpl @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val progressRepository: ProgressRepository,
) : DataPortabilityService {

    override suspend fun exportAll(): PortableBackup {
        val favorites = favoritesRepository.observeAll().first()
        val progress = progressRepository.observeContinueWatching().first()

        return PortableBackup(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            sourceProvider = "tocabonito-android",
            favorites = favorites.map { it.toPortable() },
            watchProgress = progress.map { it.toPortable() },
            settings = emptyMap(),
        )
    }

    override suspend fun importAll(backup: PortableBackup): ImportResult {
        val existingFavoriteIds = favoritesRepository.observeAll().first().map { it.id }.toSet()

        var favoritesImported = 0
        var skippedDuplicates = 0

        for (portableFavorite in backup.favorites) {
            if (portableFavorite.mediaId in existingFavoriteIds) {
                skippedDuplicates++
            } else {
                favoritesRepository.add(portableFavorite.toMediaItem())
                favoritesImported++
            }
        }

        var progressImported = 0
        for (portableProgress in backup.watchProgress) {
            val existing = progressRepository.get(portableProgress.mediaId, portableProgress.episodeId)
            if (existing != null) {
                skippedDuplicates++
            } else {
                progressRepository.save(portableProgress.toWatchProgress())
                progressImported++
            }
        }

        return ImportResult(
            favoritesImported = favoritesImported,
            progressRecordsImported = progressImported,
            settingsImported = 0,
            skippedDuplicates = skippedDuplicates,
        )
    }
}

private fun MediaItem.toPortable(): PortableFavorite = PortableFavorite(
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

private fun WatchProgress.toPortable(): PortableWatchProgress = PortableWatchProgress(
    mediaId = mediaItem.id,
    episodeId = episodeId,
    currentTime = currentTime,
    duration = duration,
    lastWatched = lastWatched,
    mediaTitle = mediaItem.title,
    mediaType = mediaItem.mediaType.name,
)

private fun PortableFavorite.toMediaItem(): MediaItem = MediaItem(
    id = mediaId,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    mediaType = MediaType.valueOf(mediaType),
    releaseYear = releaseYear,
    voteAverage = voteAverage,
    genreIds = genreIds,
)

private fun PortableWatchProgress.toWatchProgress(): WatchProgress {
    val mediaItem = MediaItem(
        id = mediaId,
        title = mediaTitle,
        overview = "",
        posterPath = null,
        backdropPath = null,
        mediaType = MediaType.valueOf(mediaType),
        releaseYear = 0,
        voteAverage = 0.0,
        genreIds = emptyList(),
    )
    return WatchProgress(
        id = "$mediaId:$episodeId",
        mediaItem = mediaItem,
        currentTime = currentTime,
        duration = duration,
        lastWatched = lastWatched,
        episodeId = episodeId,
    )
}
