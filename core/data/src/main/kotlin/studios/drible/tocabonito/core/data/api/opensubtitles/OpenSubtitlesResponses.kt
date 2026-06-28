package studios.drible.tocabonito.core.data.api.opensubtitles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubtitleSearchResponse(
    val data: List<SubtitleData>,
    @SerialName("total_count") val totalCount: Int = 0,
)

@Serializable
data class SubtitleData(
    val id: String,
    val attributes: SubtitleAttributes,
)

@Serializable
data class SubtitleAttributes(
    val language: String,
    val files: List<SubtitleFile>,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("hearing_impaired") val hearingImpaired: Boolean = false,
)

@Serializable
data class SubtitleFile(
    @SerialName("file_id") val file_id: Int,
    @SerialName("file_name") val file_name: String,
)

@Serializable
data class SubtitleDownloadResponse(
    val link: String,
    @SerialName("file_name") val file_name: String,
    val remaining: Int = 0,
)
