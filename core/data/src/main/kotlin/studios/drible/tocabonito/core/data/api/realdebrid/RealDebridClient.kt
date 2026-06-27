package studios.drible.tocabonito.core.data.api.realdebrid

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.Parameters
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamQuality

class RealDebridClient(
    private val httpClient: HttpClient,
    private val apiToken: String,
) {
    private val baseUrl = "https://api.real-debrid.com/rest/1.0"

    private val videoExtensions = setOf(
        "mkv", "mp4", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "ts",
    )

    suspend fun user(): RDUser {
        return httpClient.get("$baseUrl/user") {
            header("Authorization", "Bearer $apiToken")
        }.body()
    }

    suspend fun addMagnet(magnetLink: String): RDAddMagnetResponse {
        return httpClient.submitForm(
            url = "$baseUrl/torrents/addMagnet",
            formParameters = Parameters.build { append("magnet", magnetLink) },
        ) {
            header("Authorization", "Bearer $apiToken")
        }.body()
    }

    suspend fun torrentInfo(id: String): RDTorrentInfo {
        return httpClient.get("$baseUrl/torrents/info/$id") {
            header("Authorization", "Bearer $apiToken")
        }.body()
    }

    suspend fun selectFiles(id: String, fileIds: List<Int>) {
        httpClient.submitForm(
            url = "$baseUrl/torrents/selectFiles/$id",
            formParameters = Parameters.build {
                append("files", fileIds.joinToString(","))
            },
        ) {
            header("Authorization", "Bearer $apiToken")
        }
    }

    suspend fun transcode(id: String): RDTranscodeResponse {
        return httpClient.get("$baseUrl/streaming/transcode/$id") {
            header("Authorization", "Bearer $apiToken")
        }.body()
    }

    suspend fun unrestrict(link: String): StreamLink {
        val response: UnrestrictResponse = httpClient.submitForm(
            url = "$baseUrl/unrestrict/link",
            formParameters = Parameters.build { append("link", link) },
        ) {
            header("Authorization", "Bearer $apiToken")
        }.body()
        return response.toDomain()
    }

    fun selectVideoFile(files: List<RDTorrentFile>, preferredIndex: Int?): RDTorrentFile? {
        val videoFiles = files.filter { file ->
            val ext = file.path.substringAfterLast('.').lowercase()
            ext in videoExtensions && !isSampleFile(file.path)
        }
        if (videoFiles.isEmpty()) return null

        if (preferredIndex != null && preferredIndex < files.size) {
            val preferred = files[preferredIndex]
            val prefExt = preferred.path.substringAfterLast('.').lowercase()
            if (prefExt in videoExtensions && !isSampleFile(preferred.path)) {
                return preferred
            }
        }

        return videoFiles.maxByOrNull { it.bytes }
    }

    private fun isSampleFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.contains("/sample/") || lower.contains("sample.") ||
            lower.startsWith("/sample") || lower.contains(".sample.")
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
