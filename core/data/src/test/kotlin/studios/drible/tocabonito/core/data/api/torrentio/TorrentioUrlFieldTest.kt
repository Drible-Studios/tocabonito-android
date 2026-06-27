package studios.drible.tocabonito.core.data.api.torrentio

import io.kotest.matchers.shouldBe
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

class TorrentioUrlFieldTest {

    private fun fixtureClient(fixturePath: String): HttpClient {
        val json = javaClass.classLoader!!.getResource(fixturePath)!!.readText()
        return HttpClient(MockEngine) {
            engine {
                addHandler { respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Test
    fun `stream with url field populates resolverUrl in domain model`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams_with_url.json"))
        val results = client.streams("tt1234567", "movie")
        results[0].resolverUrl shouldBe "https://torrentio.strem.fun/realdebrid/abc123def456789012345678901234567890abcd/0"
    }

    @Test
    fun `stream without url field has null resolverUrl`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams_with_url.json"))
        val results = client.streams("tt1234567", "movie")
        results[1].resolverUrl shouldBe null
    }

    @Test
    fun `resolvedInfoHash extracts hash from url path`() {
        val stream = TorrentioStream(
            name = "Torrentio\n1080p",
            title = "test",
            infoHash = "original_hash",
            fileIdx = 5,
            url = "https://torrentio.strem.fun/realdebrid/abc123def456789012345678901234567890abcd/3",
        )
        stream.resolvedInfoHash shouldBe "abc123def456789012345678901234567890abcd"
    }

    @Test
    fun `resolvedFileIndex extracts index from url path`() {
        val stream = TorrentioStream(
            name = "Torrentio\n1080p",
            title = "test",
            infoHash = "original_hash",
            fileIdx = 5,
            url = "https://torrentio.strem.fun/realdebrid/abc123def456789012345678901234567890abcd/3",
        )
        stream.resolvedFileIndex shouldBe 3
    }

    @Test
    fun `resolvedInfoHash falls back to infoHash when url is null`() {
        val stream = TorrentioStream(
            name = "Torrentio\n1080p",
            title = "test",
            infoHash = "fallback_hash_value",
            fileIdx = 1,
        )
        stream.resolvedInfoHash shouldBe "fallback_hash_value"
    }

    @Test
    fun `resolvedFileIndex falls back to fileIdx when url is null`() {
        val stream = TorrentioStream(
            name = "Torrentio\n1080p",
            title = "test",
            infoHash = "fallback_hash_value",
            fileIdx = 7,
        )
        stream.resolvedFileIndex shouldBe 7
    }
}
