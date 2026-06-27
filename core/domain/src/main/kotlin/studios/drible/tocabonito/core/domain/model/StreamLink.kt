package studios.drible.tocabonito.core.domain.model

data class StreamLink(
    val id: String,
    val fileName: String,
    val fileSize: Int,
    val hlsUrl: String?,
    val directUrl: String,
    val quality: StreamQuality,
)

enum class StreamQuality(val value: String) {
    FULL("full"),
    HIGH("h264WebDL"),
    MEDIUM("h264Mobile"),
    LOW("divx"),
}
