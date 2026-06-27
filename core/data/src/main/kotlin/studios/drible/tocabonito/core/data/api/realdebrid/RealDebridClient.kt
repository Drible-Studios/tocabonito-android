package studios.drible.tocabonito.core.data.api.realdebrid

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.http.Parameters
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamQuality

class RealDebridClient(
    private val httpClient: HttpClient,
    private val apiToken: String,
) {
    private val baseUrl = "https://api.real-debrid.com/rest/1.0"

    suspend fun unrestrict(magnetLink: String): StreamLink {
        val response: UnrestrictResponse = httpClient.submitForm(
            url = "$baseUrl/unrestrict/link",
            formParameters = Parameters.build { append("link", magnetLink) },
        ) {
            header("Authorization", "Bearer $apiToken")
        }.body()
        return response.toDomain()
    }
}

private fun UnrestrictResponse.toDomain(): StreamLink {
    val quality = resolveQuality(alternative)
    return StreamLink(
        id = id,
        fileName = filename,
        fileSize = filesize.toInt(),
        hlsUrl = null,
        directUrl = link,
        quality = quality,
    )
}

private fun resolveQuality(alternatives: List<AlternativeLink>): StreamQuality {
    if (alternatives.isEmpty()) return StreamQuality.FULL
    val names = alternatives.map { it.filename.lowercase() }
    return when {
        names.any { it.contains("h264") && (it.contains("webdl") || it.contains("web-dl")) } -> StreamQuality.HIGH
        names.any { it.contains("mobile") || it.contains("h264_mobile") } -> StreamQuality.MEDIUM
        names.any { it.contains("divx") } -> StreamQuality.LOW
        else -> StreamQuality.FULL
    }
}
