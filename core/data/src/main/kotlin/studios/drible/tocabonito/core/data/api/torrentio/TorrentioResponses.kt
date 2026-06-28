package studios.drible.tocabonito.core.data.api.torrentio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TorrentioStreamsResponse(
    val streams: List<TorrentioStream> = emptyList(),
)

@Serializable
data class TorrentioStream(
    val name: String = "",
    val title: String = "",
    val infoHash: String = "",
    val fileIdx: Int = 0,
    val url: String? = null,
    val behaviorHints: BehaviorHints? = null,
) {
    val resolvedInfoHash: String
        get() {
            val u = url ?: return infoHash
            val segments = u.trimEnd('/').split("/")
            return if (segments.size >= 2) segments[segments.size - 2] else infoHash
        }

    val resolvedFileIndex: Int
        get() {
            val u = url ?: return fileIdx
            val segments = u.trimEnd('/').split("/")
            return segments.lastOrNull()?.toIntOrNull() ?: fileIdx
        }
}

@Serializable
data class BehaviorHints(
    @SerialName("bingeGroup") val bingeGroup: String? = null,
)
