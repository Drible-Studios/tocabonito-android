package studios.drible.tocabonito.core.data.api.torrentio

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import studios.drible.tocabonito.core.data.stream.StreamMetadataParser
import studios.drible.tocabonito.core.domain.model.StreamOption

class TorrentioClient(
    private val httpClient: HttpClient,
    private val configProvider: TorrentioConfigProvider = TorrentioConfigProvider { TorrentioConfig() },
) {

    suspend fun streams(
        imdbId: String,
        type: String,
        season: Int? = null,
        episode: Int? = null,
    ): List<StreamOption> {
        val config = configProvider.get()
        val streamPath = if (season != null && episode != null) {
            "stream/$type/$imdbId:$season:$episode.json"
        } else {
            "stream/$type/$imdbId.json"
        }
        val configSegment = if (config.configPath.isNotEmpty()) "${config.configPath}/" else ""
        val url = "${config.baseUrl}/$configSegment$streamPath"
        val response: TorrentioStreamsResponse = httpClient.get(url).body()
        return response.streams.map { it.toDomain() }
    }
}

private fun TorrentioStream.toDomain(): StreamOption {
    val quality = name.substringAfter("\n", missingDelimiterValue = "").trim().ifEmpty { name }
    val size = extractSize(title)
    val seeders = extractSeeders(title)
    val metadata = StreamMetadataParser.parse(title)
    return StreamOption(
        title = title.substringBefore("\n").trim(),
        quality = quality,
        size = size,
        seeders = seeders,
        metadata = metadata,
        infoHash = resolvedInfoHash,
        fileIndex = resolvedFileIndex,
        resolverUrl = url,
    )
}

private val sizePattern = Regex("""💾\s*([\d.]+)\s*([KMGT]?B)""")

private fun extractSize(title: String): String {
    val match = sizePattern.find(title) ?: return ""
    return "${match.groupValues[1]} ${match.groupValues[2]}"
}

private val seedersPattern = Regex("""👤\s*(\d+)""")

private fun extractSeeders(title: String): Int {
    val match = seedersPattern.find(title) ?: return 0
    return match.groupValues[1].toIntOrNull() ?: 0
}
