package studios.drible.tocabonito.core.data.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.data.api.realdebrid.RealDebridClient
import studios.drible.tocabonito.core.data.api.torrentio.TorrentioClient
import studios.drible.tocabonito.core.domain.model.StreamMetadata
import studios.drible.tocabonito.core.domain.model.StreamOption

class StreamRepositoryResolveTest {

    private val defaultMetadata = StreamMetadata(null, null, null, emptyList(), emptyList())

    private fun mockHttpClient(responses: Map<String, String>): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val url = request.url.toString()
                    val body = responses.entries.firstOrNull { (key, _) -> url.contains(key) }?.value
                        ?: error("Unhandled request: $url")
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Nested
    inner class ResolverUrlPath {
        @Test
        fun `when resolverUrl is present passes it to unrestrict directly`() = runTest {
            val responses = mapOf(
                "unrestrict/link" to """{"id":"RD123","filename":"movie.mkv","filesize":4500000000,"link":"https://real-debrid.com/d/RD123","host":"real-debrid.com","chunks":16,"alternative":[]}""",
            )
            val httpClient = mockHttpClient(responses)
            val rdClient = RealDebridClient(httpClient, "fake-token")
            val torrentioClient = TorrentioClient(httpClient)
            val repo = StreamRepositoryImpl(torrentioClient, rdClient)

            val option = StreamOption(
                title = "Movie.2024.1080p",
                quality = "1080p",
                size = "4.5 GB",
                seeders = 89,
                metadata = defaultMetadata,
                infoHash = "abc123def456789012345678901234567890abcd",
                fileIndex = 0,
                resolverUrl = "https://torrentio.strem.fun/realdebrid/abc123def456789012345678901234567890abcd/0",
            )

            val result = repo.resolveStream(option)
            result.directUrl shouldBe "https://real-debrid.com/d/RD123"
            result.id shouldBe "RD123"
        }
    }

    @Nested
    inner class FullRDFlow {
        @Test
        fun `when resolverUrl is null uses addMagnet then selectFiles then unrestrict`() = runTest {
            val responses = mapOf(
                "torrents/addMagnet" to """{"id":"TORRENT_NEW","uri":"magnet:?xt=urn:btih:abc123"}""",
                "torrents/info/TORRENT_NEW" to """{
                    "id":"TORRENT_NEW","filename":"Movie.Pack","hash":"abc123","bytes":9500000000,
                    "host":"real-debrid.com","split":40,"progress":100,"status":"downloaded",
                    "files":[
                        {"id":1,"path":"/Movie.2024.1080p.mkv","bytes":4500000000,"selected":0},
                        {"id":2,"path":"/Movie.2024.srt","bytes":52000,"selected":0}
                    ],
                    "links":["https://real-debrid.com/d/LINK_1"]
                }""",
                "torrents/selectFiles/TORRENT_NEW" to "",
                "unrestrict/link" to """{"id":"RD_FINAL","filename":"Movie.2024.1080p.mkv","filesize":4500000000,"link":"https://real-debrid.com/d/RD_FINAL","host":"real-debrid.com","chunks":16,"alternative":[]}""",
            )
            val httpClient = mockHttpClient(responses)
            val rdClient = RealDebridClient(httpClient, "fake-token")
            val torrentioClient = TorrentioClient(httpClient)
            val repo = StreamRepositoryImpl(torrentioClient, rdClient)

            val option = StreamOption(
                title = "Movie.2024.1080p",
                quality = "1080p",
                size = "4.5 GB",
                seeders = 89,
                metadata = defaultMetadata,
                infoHash = "abc123def456789012345678901234567890abcd",
                fileIndex = 0,
                resolverUrl = null,
            )

            val result = repo.resolveStream(option)
            result.id shouldBe "RD_FINAL"
            result.directUrl shouldBe "https://real-debrid.com/d/RD_FINAL"
            result.fileName shouldBe "Movie.2024.1080p.mkv"
        }
    }

    @Nested
    inner class TranscodeRecovery {
        @Test
        fun `resolveTranscode returns HLS link from transcode endpoint`() = runTest {
            val responses = mapOf(
                "torrents/addMagnet" to """{"id":"TORRENT_T","uri":"magnet:?xt=urn:btih:abc123"}""",
                "torrents/info/TORRENT_T" to """{
                    "id":"TORRENT_T","filename":"Movie.Pack","hash":"abc123","bytes":9500000000,
                    "host":"real-debrid.com","split":40,"progress":100,"status":"downloaded",
                    "files":[{"id":1,"path":"/Movie.2024.1080p.mkv","bytes":4500000000,"selected":0}],
                    "links":["https://real-debrid.com/d/LINK_T"]
                }""",
                "torrents/selectFiles/TORRENT_T" to "",
                "streaming/transcode/TORRENT_T" to """{"apple":{"full":"https://real-debrid.com/transcode/full.m3u8"},"dash":{"full":"https://real-debrid.com/transcode/full.mpd"}}""",
            )
            val httpClient = mockHttpClient(responses)
            val rdClient = RealDebridClient(httpClient, "fake-token")
            val torrentioClient = TorrentioClient(httpClient)
            val repo = StreamRepositoryImpl(torrentioClient, rdClient)

            val option = StreamOption(
                title = "Movie.2024.1080p",
                quality = "1080p",
                size = "4.5 GB",
                seeders = 89,
                metadata = defaultMetadata,
                infoHash = "abc123def456789012345678901234567890abcd",
                fileIndex = 0,
                resolverUrl = null,
            )

            val result = repo.resolveTranscode(option)
            result.hlsUrl shouldBe "https://real-debrid.com/transcode/full.m3u8"
            result.directUrl shouldBe "https://real-debrid.com/transcode/full.m3u8"
            result.id shouldNotBe ""
        }
    }
}
