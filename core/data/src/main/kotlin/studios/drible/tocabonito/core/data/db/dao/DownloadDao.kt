package studios.drible.tocabonito.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import studios.drible.tocabonito.core.data.db.entity.DownloadEntity

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY dateQueued DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE state IN ('QUEUED', 'RESOLVING', 'DOWNLOADING')")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE state = 'COMPLETED'")
    fun observeCompleted(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun get(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE downloads SET state = :state WHERE id = :id")
    suspend fun updateState(id: String, state: String)

    @Query("UPDATE downloads SET progress = :progress, bytesWritten = :bytesWritten, speedBytesPerSecond = :speed WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Double, bytesWritten: Long, speed: Double)

    @Query("SELECT SUM(totalBytes) FROM downloads WHERE state = 'COMPLETED'")
    suspend fun totalStorageUsed(): Long?

    @Query("SELECT * FROM downloads WHERE mediaId = :mediaId AND (episodeId IS NULL AND :episodeId IS NULL OR episodeId = :episodeId) LIMIT 1")
    suspend fun getByMedia(mediaId: String, episodeId: String?): DownloadEntity?

    @Query("UPDATE downloads SET failureCount = :failureCount, lastError = :lastError WHERE id = :id")
    suspend fun updateError(id: String, failureCount: Int, lastError: String?)

    @Query("UPDATE downloads SET localFilePath = :localFilePath WHERE id = :id")
    suspend fun updateLocalPath(id: String, localFilePath: String)
}
