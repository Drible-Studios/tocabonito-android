package studios.drible.tocabonito.core.data.api.realdebrid

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
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
import studios.drible.tocabonito.core.domain.model.StreamQuality

class RealDebridClientContractTest {

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
    fun `parses unrestrict response into StreamLink`() = runTest {
        val client = RealDebridClient(fixtureClient("fixtures/realdebrid/unrestrict.json"), "fake-token")
        val result = client.unrestrict("magnet:?xt=urn:btih:abc123")
        result.id.shouldNotBeEmpty()
    }

    @Test
    fun `maps id field`() = runTest {
        val client = RealDebridClient(fixtureClient("fixtures/realdebrid/unrestrict.json"), "fake-token")
        val result = client.unrestrict("magnet:?xt=urn:btih:abc123")
        result.id shouldBe "ABCDEF123"
    }

    @Test
    fun `maps filename field`() = runTest {
        val client = RealDebridClient(fixtureClient("fixtures/realdebrid/unrestrict.json"), "fake-token")
        val result = client.unrestrict("magnet:?xt=urn:btih:abc123")
        result.fileName shouldBe "Movie.2024.1080p.BluRay.x264.mkv"
    }

    @Test
    fun `maps filesize field`() = runTest {
        val client = RealDebridClient(fixtureClient("fixtures/realdebrid/unrestrict.json"), "fake-token")
        val result = client.unrestrict("magnet:?xt=urn:btih:abc123")
        result.fileSize shouldBe 4500000000L.toInt()
    }

    @Test
    fun `maps link to directUrl`() = runTest {
        val client = RealDebridClient(fixtureClient("fixtures/realdebrid/unrestrict.json"), "fake-token")
        val result = client.unrestrict("magnet:?xt=urn:btih:abc123")
        result.directUrl shouldBe "https://real-debrid.com/d/ABCDEF123"
    }

    @Test
    fun `hlsUrl is always null`() = runTest {
        val client = RealDebridClient(fixtureClient("fixtures/realdebrid/unrestrict.json"), "fake-token")
        val result = client.unrestrict("magnet:?xt=urn:btih:abc123")
        result.hlsUrl shouldBe null
    }

    @Test
    fun `resolves quality from alternatives with mobile filename`() = runTest {
        val client = RealDebridClient(fixtureClient("fixtures/realdebrid/unrestrict.json"), "fake-token")
        val result = client.unrestrict("magnet:?xt=urn:btih:abc123")
        // fixture has alternatives with h264_mobile → MEDIUM
        result.quality shouldBe StreamQuality.MEDIUM
    }
}
