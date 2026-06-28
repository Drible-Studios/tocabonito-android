package studios.drible.tocabonito.core.data.repository

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import studios.drible.tocabonito.core.data.api.opensubtitles.OpenSubtitlesClient
import studios.drible.tocabonito.core.domain.model.SubtitleTrack
import studios.drible.tocabonito.core.domain.repository.SubtitleRepository
import java.io.File
import javax.inject.Inject

class SubtitleRepositoryImpl @Inject constructor(
    private val openSubtitlesClient: OpenSubtitlesClient,
    private val downloadHttpClient: HttpClient,
    private val context: Context,
) : SubtitleRepository {

    private val cacheDir: File
        get() = File(context.cacheDir, "subtitles").also { it.mkdirs() }

    override suspend fun fetchSubtitle(imdbId: String, language: String): SubtitleTrack? {
        val cachedFile = File(cacheDir, "${imdbId}_$language.srt")
        if (cachedFile.exists()) {
            return cachedFile.toSubtitleTrack(language)
        }

        val results = openSubtitlesClient.search(imdbId, language)
        if (results.isEmpty()) return null

        // Pick best: highest download count, non-HI preferred
        val best = results
            .sortedWith(compareBy({ it.attributes.hearingImpaired }, { -it.attributes.downloadCount }))
            .first()

        val fileId = best.attributes.files.firstOrNull()?.file_id ?: return null

        val downloadResponse = openSubtitlesClient.download(fileId)

        val channel = downloadHttpClient.get(downloadResponse.link).bodyAsChannel()
        cachedFile.outputStream().use { out ->
            channel.toInputStream().use { input -> input.copyTo(out) }
        }

        return cachedFile.toSubtitleTrack(language)
    }

    private fun File.toSubtitleTrack(language: String) = SubtitleTrack(
        index = 100,
        name = "Portuguese (BR)",
        languageCode = language,
        codec = "text/x-ssa",
        isExternal = true,
        externalUrl = this.absolutePath,
    )
}
