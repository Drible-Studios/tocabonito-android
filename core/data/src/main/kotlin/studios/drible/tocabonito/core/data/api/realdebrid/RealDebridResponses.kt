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

@Serializable
data class RDUser(
    val id: Int,
    val username: String,
    val email: String,
    val points: Int = 0,
    val locale: String = "en",
    val avatar: String = "",
    val type: String = "",
    val premium: Long = 0,
    val expiration: String = "",
)

@Serializable
data class RDAddMagnetResponse(
    val id: String,
    val uri: String = "",
)

@Serializable
data class RDTorrentInfo(
    val id: String,
    val filename: String = "",
    val hash: String = "",
    val bytes: Long = 0,
    val host: String = "",
    val split: Int = 0,
    val progress: Int = 0,
    val status: String = "",
    val files: List<RDTorrentFile> = emptyList(),
    val links: List<String> = emptyList(),
)

@Serializable
data class RDTorrentFile(
    val id: Int,
    val path: String,
    val bytes: Long,
    val selected: Int = 0,
)

@Serializable
data class RDTranscodeResponse(
    val apple: Map<String, String>? = null,
    val dash: Map<String, String>? = null,
)
