package studios.drible.tocabonito.core.data.api.tmdb

import io.kotest.matchers.collections.shouldNotBeEmpty
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

class TMDBClientContractTest {

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
    fun `parses trending response`() = runTest {
        val client = TMDBClient(fixtureClient("fixtures/tmdb/trending.json"), "fake-key")
        val results = client.trending()
        results.shouldNotBeEmpty()
        results.first().title.shouldNotBeEmpty()
        results.first().id.shouldNotBeEmpty()
    }

    @Test
    fun `parses search response`() = runTest {
        val client = TMDBClient(fixtureClient("fixtures/tmdb/search.json"), "fake-key")
        val results = client.search("test")
        results.shouldNotBeEmpty()
    }

    @Test
    fun `parses movie details`() = runTest {
        val client = TMDBClient(fixtureClient("fixtures/tmdb/movie_details.json"), "fake-key")
        val result = client.details("tt1234567", "movie")
        result.title shouldBe "Test Movie"
    }
}
