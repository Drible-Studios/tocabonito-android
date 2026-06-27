package studios.drible.tocabonito.core.domain.service

import kotlinx.serialization.Serializable

@Serializable
data class PortableBackup(
    val version: Int,
    val exportedAt: Long,
    val sourceProvider: String,
    val favorites: List<PortableFavorite>,
    val watchProgress: List<PortableWatchProgress>,
    val settings: Map<String, String>,
)

@Serializable
data class PortableFavorite(
    val mediaId: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val mediaType: String,
    val releaseYear: Int,
    val voteAverage: Double,
    val genreIds: List<Int>,
    val dateAdded: Long,
)

@Serializable
data class PortableWatchProgress(
    val mediaId: String,
    val episodeId: String?,
    val currentTime: Double,
    val duration: Double,
    val lastWatched: Long,
    val mediaTitle: String,
    val mediaType: String,
)

data class ImportResult(
    val favoritesImported: Int,
    val progressRecordsImported: Int,
    val settingsImported: Int,
    val skippedDuplicates: Int,
)

interface DataPortabilityService {
    suspend fun exportAll(): PortableBackup
    suspend fun importAll(backup: PortableBackup): ImportResult
}
