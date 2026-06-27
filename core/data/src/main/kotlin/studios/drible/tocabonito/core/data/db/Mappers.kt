package studios.drible.tocabonito.core.data.db

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import studios.drible.tocabonito.core.data.db.entity.FavoriteEntity
import studios.drible.tocabonito.core.data.db.entity.WatchProgressEntity
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
