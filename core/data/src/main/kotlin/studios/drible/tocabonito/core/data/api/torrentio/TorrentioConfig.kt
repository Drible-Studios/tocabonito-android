package studios.drible.tocabonito.core.data.api.torrentio

data class TorrentioConfig(
    val configPath: String = "",
    val baseUrl: String = "https://torrentio.strem.fun",
)

fun interface TorrentioConfigProvider {
    suspend fun get(): TorrentioConfig
}
