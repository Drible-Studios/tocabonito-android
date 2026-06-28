package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.WatchProgress

fun MediaItem.toFavoriteDocument(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "overview" to overview,
    "posterPath" to posterPath,
    "backdropPath" to backdropPath,
    "mediaType" to mediaType.value,
    "releaseYear" to releaseYear,
    "voteAverage" to voteAverage,
    "genreIds" to genreIds,
    "addedAt" to System.currentTimeMillis(),
)

fun Map<String, Any?>.toMediaItemFromFavorite(): MediaItem = MediaItem(
    id = get("id") as? String ?: "",
    title = get("title") as? String ?: "",
    overview = get("overview") as? String ?: "",
    posterPath = get("posterPath") as? String,
    backdropPath = get("backdropPath") as? String,
    mediaType = (get("mediaType") as? String)?.let { value ->
        MediaType.entries.firstOrNull { it.value == value }
    } ?: MediaType.MOVIE,
    releaseYear = (get("releaseYear") as? Number)?.toInt() ?: 0,
    voteAverage = (get("voteAverage") as? Number)?.toDouble() ?: 0.0,
    genreIds = (get("genreIds") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
)

fun WatchProgress.toProgressDocument(): Map<String, Any?> = mapOf(
    "id" to id,
    "mediaId" to mediaItem.id,
    "mediaTitle" to mediaItem.title,
    "mediaType" to mediaItem.mediaType.value,
    "currentTime" to currentTime,
    "duration" to duration,
    "lastWatched" to lastWatched,
    "episodeId" to episodeId,
)

fun Map<String, Any?>.toWatchProgress(): WatchProgress {
    val mediaId = get("mediaId") as? String ?: ""
    val mediaItem = MediaItem(
        id = mediaId,
        title = get("mediaTitle") as? String ?: "",
        overview = "",
        posterPath = null,
        backdropPath = null,
        mediaType = (get("mediaType") as? String)?.let { value ->
            MediaType.entries.firstOrNull { it.value == value }
        } ?: MediaType.MOVIE,
        releaseYear = 0,
        voteAverage = 0.0,
        genreIds = emptyList(),
    )
    return WatchProgress(
        id = get("id") as? String ?: "$mediaId:${get("episodeId")}",
        mediaItem = mediaItem,
        currentTime = (get("currentTime") as? Number)?.toDouble() ?: 0.0,
        duration = (get("duration") as? Number)?.toDouble() ?: 0.0,
        lastWatched = (get("lastWatched") as? Number)?.toLong() ?: 0L,
        episodeId = get("episodeId") as? String,
    )
}
