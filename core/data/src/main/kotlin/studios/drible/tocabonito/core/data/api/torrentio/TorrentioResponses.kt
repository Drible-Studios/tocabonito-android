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
    val behaviorHints: BehaviorHints? = null,
)

@Serializable
data class BehaviorHints(
    @SerialName("bingeGroup") val bingeGroup: String? = null,
)
