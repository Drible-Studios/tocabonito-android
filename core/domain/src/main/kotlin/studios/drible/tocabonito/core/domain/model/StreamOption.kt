package studios.drible.tocabonito.core.domain.model

data class StreamOption(
    val title: String,
    val quality: String,
    val size: String,
    val seeders: Int = 0,
    val metadata: StreamMetadata = StreamMetadata(null, null, null, emptyList(), emptyList()),
    val infoHash: String,
    val fileIndex: Int,
    val resolverUrl: String? = null,
) {
    val id: String = "$infoHash:$fileIndex:${resolverUrl ?: title}"

    val magnetLink: String
        get() = "magnet:?xt=urn:btih:$infoHash"

    fun playabilityTier(fileExtension: String): Int {
        if (metadata.codec == "H264") return 1
        if (title.lowercase().contains(".$fileExtension")) return 2
        return 3
    }
}
