package studios.drible.tocabonito.core.domain.model

data class Episode(
    val id: String,
    val name: String,
    val overview: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val stillPath: String?,
    val airDate: String?,
) {
    val stillUrl: String?
        get() = stillPath?.let { "https://image.tmdb.org/t/p/w300$it" }

    val formattedCode: String
        get() = "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
}
