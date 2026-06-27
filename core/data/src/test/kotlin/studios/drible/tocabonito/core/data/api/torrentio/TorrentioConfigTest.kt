package studios.drible.tocabonito.core.data.api.torrentio

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
import org.junit.jupiter.api.Test

class TorrentioConfigTest {

    private val emptyStreamsJson = """{"streams":[]}"""

    private fun capturingClient(onRequest: (String) -> Unit): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    onRequest(request.url.toString())
                    respond(emptyStreamsJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Test
    fun `default config produces standard base URL`() {
        val config = TorrentioConfig()
        config.baseUrl shouldBe "https://torrentio.strem.fun"
        config.configPath shouldBe ""
    }

    @Test
    fun `URL includes config path when provided`() = runTest {
        var capturedUrl = ""
        val client = TorrentioClient(
            httpClient = capturingClient { capturedUrl = it },
            configProvider = { TorrentioConfig(configPath = "realdebrid=MYTOKEN123|qualityfilter=480p,scr,cam") },
        )
        client.streams("tt1234567", "movie")
        capturedUrl shouldContain "realdebrid=MYTOKEN123|qualityfilter=480p,scr,cam"
        capturedUrl shouldContain "/stream/movie/tt1234567.json"
    }

    @Test
    fun `URL omits config path segment when config path is empty`() = runTest {
        var capturedUrl = ""
        val client = TorrentioClient(
            httpClient = capturingClient { capturedUrl = it },
            configProvider = { TorrentioConfig(configPath = "") },
        )
        client.streams("tt1234567", "movie")
        capturedUrl shouldBe "https://torrentio.strem.fun/stream/movie/tt1234567.json"
    }

    @Test
    fun `URL uses custom base URL when configured`() = runTest {
        var capturedUrl = ""
        val client = TorrentioClient(
            httpClient = capturingClient { capturedUrl = it },
            configProvider = { TorrentioConfig(baseUrl = "https://custom.torrentio.host", configPath = "key=val") },
        )
        client.streams("tt9999999", "series", season = 1, episode = 3)
        capturedUrl shouldBe "https://custom.torrentio.host/key=val/stream/series/tt9999999:1:3.json"
    }

    @Test
    fun `series URL includes season and episode with config path`() = runTest {
        var capturedUrl = ""
        val client = TorrentioClient(
            httpClient = capturingClient { capturedUrl = it },
            configProvider = { TorrentioConfig(configPath = "rd=TOKEN") },
        )
        client.streams("tt5555555", "series", season = 2, episode = 7)
        capturedUrl shouldBe "https://torrentio.strem.fun/rd=TOKEN/stream/series/tt5555555:2:7.json"
    }

    @Test
    fun `backward-compatible constructor uses empty config`() = runTest {
        var capturedUrl = ""
        val client = TorrentioClient(capturingClient { capturedUrl = it })
        client.streams("tt0000001", "movie")
        capturedUrl shouldBe "https://torrentio.strem.fun/stream/movie/tt0000001.json"
    }
}
