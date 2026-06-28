package studios.drible.tocabonito.core.data.download

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import java.io.File
import javax.inject.Inject

class KtorHttpDownloader @Inject constructor(
    private val httpClient: HttpClient,
) : HttpDownloader {

    override suspend fun download(
        url: String,
        destination: File,
        onProgress: suspend (bytesWritten: Long) -> Unit,
    ) {
        destination.parentFile?.mkdirs()
        httpClient.prepareGet(url).execute { response ->
            val channel = response.bodyAsChannel()
            var totalBytesWritten = 0L
            var bytesSinceLastReport = 0L
            val buffer = ByteArray(BUFFER_SIZE)

            destination.outputStream().buffered().use { output ->
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead <= 0) continue
                    output.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead
                    bytesSinceLastReport += bytesRead

                    if (bytesSinceLastReport >= PROGRESS_INTERVAL_BYTES) {
                        onProgress(totalBytesWritten)
                        bytesSinceLastReport = 0L
                    }
                }
            }
            // Final progress report
            onProgress(totalBytesWritten)
        }
    }

    companion object {
        private const val BUFFER_SIZE = 8 * 1024
        private const val PROGRESS_INTERVAL_BYTES = 1L * 1024 * 1024 // 1MB
    }
}
