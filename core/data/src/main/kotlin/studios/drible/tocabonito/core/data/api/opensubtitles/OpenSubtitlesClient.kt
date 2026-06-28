package studios.drible.tocabonito.core.data.api.opensubtitles

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class OpenSubtitlesClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    private val baseUrl = "https://api.opensubtitles.com/api/v1"

    suspend fun search(imdbId: String, language: String = "pob"): List<SubtitleData> {
        val response: SubtitleSearchResponse = httpClient.get("$baseUrl/subtitles") {
            header("Api-Key", apiKey)
            parameter("imdb_id", imdbId)
            parameter("languages", language)
        }.body()
        return response.data
    }

    suspend fun download(fileId: Int): SubtitleDownloadResponse {
        return httpClient.post("$baseUrl/download") {
            header("Api-Key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(DownloadRequest(fileId))
        }.body()
    }
}

@Serializable
private data class DownloadRequest(
    @SerialName("file_id") val fileId: Int,
)
