package studios.drible.tocabonito.core.data.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import studios.drible.tocabonito.core.data.db.entity.DownloadEntity
import studios.drible.tocabonito.core.data.db.entity.FavoriteEntity
import studios.drible.tocabonito.core.data.db.entity.WatchProgressEntity
import studios.drible.tocabonito.core.domain.model.DownloadItem
import studios.drible.tocabonito.core.domain.model.DownloadPriority
import studios.drible.tocabonito.core.domain.model.DownloadState
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.WatchProgress

// FavoriteEntity <-> MediaItem

fun FavoriteEntity.toDomain(): MediaItem = MediaItem(
    id = mediaId,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    mediaType = if (mediaType == "series") MediaType.SERIES else MediaType.MOVIE,
    releaseYear = releaseYear,
    voteAverage = voteAverage,
    genreIds = Json.decodeFromString<List<Int>>(genreIds),
)

fun MediaItem.toEntity(): FavoriteEntity = FavoriteEntity(
    mediaId = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    mediaType = mediaType.value,
    releaseYear = releaseYear,
    voteAverage = voteAverage,
    genreIds = Json.encodeToString(genreIds),
    addedAt = System.currentTimeMillis(),
)

// WatchProgressEntity <-> WatchProgress

fun WatchProgressEntity.toDomain(): WatchProgress {
    val mediaItem = MediaItem(
        id = mediaId,
        title = mediaTitle,
        overview = "",
        posterPath = mediaPosterPath,
        backdropPath = mediaBackdropPath,
        mediaType = if (mediaType == "series") MediaType.SERIES else MediaType.MOVIE,
        releaseYear = mediaReleaseYear,
        voteAverage = mediaVoteAverage,
        genreIds = Json.decodeFromString<List<Int>>(mediaGenreIds),
    )
    return WatchProgress(
        id = id,
        mediaItem = mediaItem,
        currentTime = currentTime,
        duration = duration,
        lastWatched = lastWatched,
        episodeId = episodeId,
    )
}

fun WatchProgress.toEntity(): WatchProgressEntity = WatchProgressEntity(
    id = id,
    mediaId = mediaItem.id,
    mediaTitle = mediaItem.title,
    mediaPosterPath = mediaItem.posterPath,
    mediaBackdropPath = mediaItem.backdropPath,
    mediaType = mediaItem.mediaType.value,
    mediaReleaseYear = mediaItem.releaseYear,
    mediaVoteAverage = mediaItem.voteAverage,
    mediaGenreIds = Json.encodeToString(mediaItem.genreIds),
    episodeId = episodeId,
    currentTime = currentTime,
    duration = duration,
    lastWatched = lastWatched,
)

// DownloadEntity <-> DownloadItem

fun DownloadEntity.toDomain(): DownloadItem = DownloadItem(
    id = id,
    mediaId = mediaId,
    episodeId = episodeId,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    mediaType = if (mediaType == "series") MediaType.SERIES else MediaType.MOVIE,
    quality = quality,
    source = source,
    codec = codec,
    state = DownloadState.valueOf(state),
    progress = progress,
    bytesWritten = bytesWritten,
    totalBytes = totalBytes,
    estimatedBytes = estimatedBytes,
    localFilePath = localFilePath,
    fileExtension = fileExtension,
    dateQueued = dateQueued,
    dateCompleted = dateCompleted,
    failureCount = failureCount,
    lastError = lastError,
    priority = DownloadPriority.entries.firstOrNull { it.value == priority } ?: DownloadPriority.USER_INITIATED,
    allowedOnCellular = allowedOnCellular,
    speedBytesPerSecond = speedBytesPerSecond,
)

fun DownloadItem.toEntity(): DownloadEntity = DownloadEntity(
    id = id,
    mediaId = mediaId,
    episodeId = episodeId,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    mediaType = mediaType.value,
    quality = quality,
    source = source,
    codec = codec,
    state = state.name,
    progress = progress,
    bytesWritten = bytesWritten,
    totalBytes = totalBytes,
    estimatedBytes = estimatedBytes,
    localFilePath = localFilePath,
    fileExtension = fileExtension,
    dateQueued = dateQueued,
    dateCompleted = dateCompleted,
    failureCount = failureCount,
    lastError = lastError,
    priority = priority.value,
    allowedOnCellular = allowedOnCellular,
    speedBytesPerSecond = speedBytesPerSecond,
)
