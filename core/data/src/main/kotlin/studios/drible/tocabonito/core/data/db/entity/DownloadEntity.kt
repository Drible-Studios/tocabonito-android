package studios.drible.tocabonito.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val mediaId: String,
    val episodeId: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val mediaType: String,
    val quality: String,
    val source: String,
    val codec: String,
    val state: String,
    val progress: Double,
    val bytesWritten: Long,
    val totalBytes: Long,
    val estimatedBytes: Long,
    val localFilePath: String?,
    val fileExtension: String?,
    val dateQueued: Long,
    val dateCompleted: Long?,
    val failureCount: Int,
    val lastError: String?,
    val priority: Int,
    val allowedOnCellular: Boolean,
    val speedBytesPerSecond: Double?,
)
