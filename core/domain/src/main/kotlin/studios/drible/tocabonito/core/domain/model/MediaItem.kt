package studios.drible.tocabonito.core.domain.model

data class MediaItem(
    val id: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val mediaType: MediaType,
    val releaseYear: Int,
    val voteAverage: Double,
    val genreIds: List<Int>,
) {
    val imdbId: String get() = id
    val isMovie: Boolean get() = mediaType == MediaType.MOVIE

    val posterUrl: String?
        get() = posterPath?.let { "${IMAGE_BASE_URL}w500$it" }

    val backdropUrl: String?
        get() = backdropPath?.let { "${IMAGE_BASE_URL}original$it" }

    companion object {
        private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
    }
}
