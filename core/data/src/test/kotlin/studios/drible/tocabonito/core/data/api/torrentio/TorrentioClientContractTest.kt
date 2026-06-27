package studios.drible.tocabonito.core.data.api.torrentio

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
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

class TorrentioClientContractTest {

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
    fun `parses streams response into StreamOptions`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams.json"))
        val results = client.streams("tt1234567", "movie")
        results shouldHaveSize 2
    }

    @Test
    fun `maps quality from name field after newline`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams.json"))
        val results = client.streams("tt1234567", "movie")
        results[0].quality shouldBe "4K"
        results[1].quality shouldBe "1080p"
    }

    @Test
    fun `extracts seeders from title`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams.json"))
        val results = client.streams("tt1234567", "movie")
        results[0].seeders shouldBe 150
        results[1].seeders shouldBe 89
    }

    @Test
    fun `extracts size from title`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams.json"))
        val results = client.streams("tt1234567", "movie")
        results[0].size shouldBe "45.2 GB"
        results[1].size shouldBe "4.5 GB"
    }

    @Test
    fun `maps infoHash and fileIndex`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams.json"))
        val results = client.streams("tt1234567", "movie")
        results[0].infoHash shouldBe "abc123def456789012345678901234567890abcd"
        results[0].fileIndex shouldBe 0
        results[1].infoHash shouldBe "def456abc789012345678901234567890abcd1234"
        results[1].fileIndex shouldBe 1
    }

    @Test
    fun `parses metadata codec from title`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams.json"))
        val results = client.streams("tt1234567", "movie")
        // first stream: x265 → H265, second stream: x264 → H264
        results[0].metadata.codec shouldBe "H265"
        results[1].metadata.codec shouldBe "H264"
    }

    @Test
    fun `parses metadata source from title`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams.json"))
        val results = client.streams("tt1234567", "movie")
        results[0].metadata.source shouldBe "BluRay"
        results[1].metadata.source shouldBe "WEB-DL"
    }

    @Test
    fun `parses languages from emoji flags`() = runTest {
        val client = TorrentioClient(fixtureClient("fixtures/torrentio/streams.json"))
        val results = client.streams("tt1234567", "movie")
        // first stream has 🇬🇧🇧🇷 → EN, PT
        results[0].metadata.languages.shouldNotBeEmpty()
    }
}
