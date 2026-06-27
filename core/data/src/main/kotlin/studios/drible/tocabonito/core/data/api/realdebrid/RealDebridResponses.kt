package studios.drible.tocabonito.core.data.api.realdebrid

import kotlinx.serialization.Serializable

@Serializable
data class UnrestrictResponse(
    val id: String,
    val filename: String,
    val filesize: Long,
    val link: String,
    val host: String = "",
    val chunks: Int = 0,
    val alternative: List<AlternativeLink> = emptyList(),
)

@Serializable
data class AlternativeLink(
    val id: String,
    val filename: String,
    val download: String,
)
