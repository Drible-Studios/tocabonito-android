package studios.drible.tocabonito.core.data.download

import java.io.File

interface HttpDownloader {
    suspend fun download(
        url: String,
        destination: File,
        onProgress: suspend (bytesWritten: Long) -> Unit,
    )
}
