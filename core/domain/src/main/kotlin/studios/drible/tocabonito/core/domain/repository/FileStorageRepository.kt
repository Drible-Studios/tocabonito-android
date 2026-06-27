package studios.drible.tocabonito.core.domain.repository

interface FileStorageRepository {
    fun allocate(downloadId: String, mediaId: String, episodeId: String?, ext: String): String
    fun moveToFinal(fromPath: String, toPath: String)
    fun delete(filePath: String)
    fun fileExists(filePath: String): Boolean
    fun totalUsedBytes(): Long
    fun deviceFreeSpace(): Long
}
