package studios.drible.tocabonito.core.domain.model

data class StreamMetadata(
    val codec: String?,
    val hdr: String?,
    val source: String?,
    val languages: List<String>,
    val subtitles: List<String>,
) {
    val videoTypeLabel: String
        get() = listOfNotNull(source, codec, hdr).joinToString(" ")

    val languageLabel: String
        get() = languages.joinToString("/")

    val subtitleLabel: String
        get() = subtitles.joinToString("/")
}
