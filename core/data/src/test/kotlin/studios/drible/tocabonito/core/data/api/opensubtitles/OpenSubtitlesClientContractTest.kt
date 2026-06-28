package studios.drible.tocabonito.core.data.api.opensubtitles

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class OpenSubtitlesClientContractTest {

    private fun fixtureClient(
        fixturePath: String,
        onRequest: ((io.ktor.client.request.HttpRequestData) -> Unit)? = null,
    ): HttpClient {
        val jsonText = javaClass.classLoader!!.getResource(fixturePath)!!.readText()
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    onRequest?.invoke(request)
                    respond(jsonText, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Test
    fun `search sends correct API key header and imdb query`() = runTest {
        var capturedHeaders: Headers? = null
        var capturedUrl: String? = null
        val client = OpenSubtitlesClient(
            fixtureClient("fixtures/opensubtitles/search.json") { request ->
                capturedHeaders = request.headers
                capturedUrl = request.url.toString()
            },
            apiKey = "test-api-key",
        )

        client.search("tt1234567")

        capturedHeaders!!["Api-Key"] shouldBe "test-api-key"
        capturedUrl!! shouldContain "imdb_id=tt1234567"
        capturedUrl!! shouldContain "languages=pob"
    }

    @Test
    fun `search parses response into SubtitleData list`() = runTest {
        val client = OpenSubtitlesClient(
            fixtureClient("fixtures/opensubtitles/search.json"),
            apiKey = "test-api-key",
        )

        val results = client.search("tt1234567")

        results shouldHaveSize 2
        results[0].id shouldBe "123456"
        results[0].attributes.language shouldBe "pt-BR"
        results[0].attributes.files[0].file_id shouldBe 99001
        results[0].attributes.files[0].file_name shouldBe "Movie.2024.1080p.pob.srt"
    }

    @Test
    fun `download sends POST with file_id and returns link`() = runTest {
        var capturedMethod: HttpMethod? = null
        val client = OpenSubtitlesClient(
            fixtureClient("fixtures/opensubtitles/download.json") { request ->
                capturedMethod = request.method
            },
            apiKey = "test-api-key",
        )

        val result = client.download(99001)

        capturedMethod shouldBe HttpMethod.Post
        result.link shouldBe "https://dl.opensubtitles.org/en/download/sub/99001"
        result.file_name shouldBe "Movie.2024.1080p.pob.srt"
    }
}
