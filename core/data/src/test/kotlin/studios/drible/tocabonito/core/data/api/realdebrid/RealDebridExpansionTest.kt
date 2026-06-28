package studios.drible.tocabonito.core.data.api.realdebrid

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RealDebridExpansionTest {

    private fun fixtureClient(fixturePath: String, expectedMethod: HttpMethod = HttpMethod.Get): HttpClient {
        val json = javaClass.classLoader!!.getResource(fixturePath)!!.readText()
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    private fun noBodyClient(): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { respond("", HttpStatusCode.NoContent) }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Nested
    inner class UserEndpoint {
        @Test
        fun `parses user response`() = runTest {
            val client = RealDebridClient(fixtureClient("fixtures/realdebrid/user.json"), "fake-token")
            val user = client.user()
            user.id shouldBe 12345
            user.username shouldBe "testuser"
            user.email shouldBe "test@example.com"
            user.type shouldBe "premium"
            user.expiration shouldBe "2025-12-31T23:59:59.000Z"
        }
    }

    @Nested
    inner class AddMagnetEndpoint {
        @Test
        fun `parses add magnet response`() = runTest {
            val client = RealDebridClient(fixtureClient("fixtures/realdebrid/add_magnet.json"), "fake-token")
            val result = client.addMagnet("magnet:?xt=urn:btih:abc123")
            result.id shouldBe "TORRENT_ABC123"
            result.uri shouldBe "magnet:?xt=urn:btih:abc123def456789012345678901234567890abcd"
        }
    }

    @Nested
    inner class TorrentInfoEndpoint {
        @Test
        fun `parses torrent info with files`() = runTest {
            val client = RealDebridClient(fixtureClient("fixtures/realdebrid/torrent_info.json"), "fake-token")
            val info = client.torrentInfo("TORRENT_ABC123")
            info.id shouldBe "TORRENT_ABC123"
            info.filename shouldBe "Movie.2024.Pack"
            info.status shouldBe "downloaded"
            info.files shouldHaveSize 4
            info.links shouldHaveSize 1
        }

        @Test
        fun `torrent info files have correct structure`() = runTest {
            val client = RealDebridClient(fixtureClient("fixtures/realdebrid/torrent_info.json"), "fake-token")
            val info = client.torrentInfo("TORRENT_ABC123")
            val firstFile = info.files[0]
            firstFile.id shouldBe 1
            firstFile.path shouldBe "/Movie.2024.1080p.BluRay.x264.mkv"
            firstFile.bytes shouldBe 4500000000L
            firstFile.selected shouldBe 1
        }
    }

    @Nested
    inner class SelectFilesEndpoint {
        @Test
        fun `selectFiles completes without exception`() = runTest {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            client.selectFiles("TORRENT_ABC123", listOf(1, 2))
        }
    }

    @Nested
    inner class TranscodeEndpoint {
        @Test
        fun `parses transcode response with apple and dash`() = runTest {
            val client = RealDebridClient(fixtureClient("fixtures/realdebrid/transcode.json"), "fake-token")
            val result = client.transcode("TORRENT_ABC123")
            result.apple shouldNotBe null
            result.apple!!["full"] shouldBe "https://real-debrid.com/transcode/apple_full.m3u8"
            result.dash shouldNotBe null
            result.dash!!["full"] shouldBe "https://real-debrid.com/transcode/dash_full.mpd"
            result.dash!!["medium"] shouldBe "https://real-debrid.com/transcode/dash_medium.mpd"
        }
    }

    @Nested
    inner class VideoFileSelection {
        private val files = listOf(
            RDTorrentFile(id = 1, path = "/Movie.2024.1080p.BluRay.x264.mkv", bytes = 4500000000L, selected = 1),
            RDTorrentFile(id = 2, path = "/Movie.2024.1080p.BluRay.x264.srt", bytes = 52000L, selected = 0),
            RDTorrentFile(id = 3, path = "/Sample/sample.mkv", bytes = 80000000L, selected = 0),
            RDTorrentFile(id = 4, path = "/Extras/behind_the_scenes.mkv", bytes = 950000000L, selected = 0),
        )

        @Test
        fun `selects file at preferred index when it is a video`() {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            val selected = client.selectVideoFile(files, preferredIndex = 0)
            selected shouldBe files[0]
        }

        @Test
        fun `falls back to largest video file when preferred index is non-video`() {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            val selected = client.selectVideoFile(files, preferredIndex = 1)
            selected shouldBe files[0]
        }

        @Test
        fun `selects largest video file when preferred index is null`() {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            val selected = client.selectVideoFile(files, preferredIndex = null)
            selected shouldBe files[0]
        }

        @Test
        fun `returns null when no video files exist`() {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            val nonVideoFiles = listOf(
                RDTorrentFile(id = 1, path = "/readme.txt", bytes = 500L, selected = 0),
                RDTorrentFile(id = 2, path = "/info.nfo", bytes = 200L, selected = 0),
            )
            val selected = client.selectVideoFile(nonVideoFiles, preferredIndex = null)
            selected shouldBe null
        }

        @Test
        fun `ignores sample files even if they are video`() {
            val client = RealDebridClient(noBodyClient(), "fake-token")
            val selected = client.selectVideoFile(files, preferredIndex = 2)
            selected shouldBe files[0]
        }
    }
}
