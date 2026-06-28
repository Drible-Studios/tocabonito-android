package studios.drible.tocabonito.core.data.api.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TMDBPageResponse(
    val page: Int,
    val results: List<TMDBMediaResult>,
    @SerialName("total_pages") val totalPages: Int,
)

@Serializable
data class TMDBMediaResult(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
)

@Serializable
data class TMDBMovieDetail(
    val id: Int,
    @SerialName("imdb_id") val imdbId: String? = null,
    val title: String = "",
    val name: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val genres: List<TMDBGenre> = emptyList(),
) {
    val displayTitle: String get() = title.ifEmpty { name }
    val displayDate: String? get() = releaseDate ?: firstAirDate
}

@Serializable
data class TMDBGenre(val id: Int, val name: String)

@Serializable
data class TMDBSeasonsResponse(
    val seasons: List<TMDBSeason>,
)

@Serializable
data class TMDBSeason(
    @SerialName("season_number") val seasonNumber: Int,
    val episodes: List<TMDBEpisode> = emptyList(),
)

@Serializable
data class TMDBEpisode(
    val id: Int,
    val name: String,
    val overview: String = "",
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("air_date") val airDate: String? = null,
)

@Serializable
data class TMDBGenreListResponse(val genres: List<TMDBGenre>)
