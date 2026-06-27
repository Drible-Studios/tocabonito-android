package studios.drible.tocabonito.core.data.api.tmdb

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import studios.drible.tocabonito.core.domain.model.Genre
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType

class TMDBClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    private val baseUrl = "https://api.themoviedb.org/3"

    suspend fun trending(): List<MediaItem> {
        val response: TMDBPageResponse = httpClient.get("$baseUrl/trending/all/week") {
            header("Authorization", "Bearer $apiKey")
        }.body()
        return response.results.map { it.toDomain() }
    }

    suspend fun search(query: String): List<MediaItem> {
        val response: TMDBPageResponse = httpClient.get("$baseUrl/search/multi") {
            header("Authorization", "Bearer $apiKey")
            parameter("query", query)
        }.body()
        return response.results.map { it.toDomain() }
    }

    suspend fun details(id: String, type: String): MediaItem {
        val response: TMDBMovieDetail = httpClient.get("$baseUrl/movie/$id") {
            header("Authorization", "Bearer $apiKey")
        }.body()
        return response.toDomain()
    }

    suspend fun popular(type: String, page: Int = 1): List<MediaItem> {
        val endpoint = if (type == "movie") "movie/popular" else "tv/popular"
        val response: TMDBPageResponse = httpClient.get("$baseUrl/$endpoint") {
            header("Authorization", "Bearer $apiKey")
            parameter("page", page)
        }.body()
        return response.results.map { it.toDomain() }
    }

    suspend fun genres(type: String): List<Genre> {
        val endpoint = if (type == "movie") "genre/movie/list" else "genre/tv/list"
        val response: TMDBGenreListResponse = httpClient.get("$baseUrl/$endpoint") {
            header("Authorization", "Bearer $apiKey")
        }.body()
        return response.genres.map { Genre(id = it.id, name = it.name) }
    }

    suspend fun discover(
        type: String,
        genreId: Int,
        page: Int,
        params: Map<String, String> = emptyMap(),
    ): List<MediaItem> {
        val endpoint = if (type == "movie") "discover/movie" else "discover/tv"
        val response: TMDBPageResponse = httpClient.get("$baseUrl/$endpoint") {
            header("Authorization", "Bearer $apiKey")
            parameter("with_genres", genreId)
            parameter("page", page)
            params.forEach { (k, v) -> parameter(k, v) }
        }.body()
        return response.results.map { it.toDomain() }
    }
}

private fun TMDBMediaResult.toDomain(): MediaItem {
    val type = when (mediaType) {
        "tv" -> MediaType.SERIES
        else -> MediaType.MOVIE
    }
    val year = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull() ?: 0
    return MediaItem(
        id = "tt$id",
        title = title ?: name ?: "",
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        mediaType = type,
        releaseYear = year,
        voteAverage = voteAverage,
        genreIds = genreIds,
    )
}

private fun TMDBMovieDetail.toDomain(): MediaItem = MediaItem(
    id = imdbId ?: "tt$id",
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    mediaType = MediaType.MOVIE,
    releaseYear = releaseDate?.take(4)?.toIntOrNull() ?: 0,
    voteAverage = voteAverage,
    genreIds = genres.map { it.id },
)
