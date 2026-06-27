package studios.drible.tocabonito.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import studios.drible.tocabonito.core.data.db.entity.WatchProgressEntity

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress ORDER BY lastWatched DESC")
    fun observeAll(): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE mediaId = :mediaId AND (episodeId = :episodeId OR (:episodeId IS NULL AND episodeId IS NULL))")
    suspend fun get(mediaId: String, episodeId: String?): WatchProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE mediaId = :mediaId AND (episodeId = :episodeId OR (:episodeId IS NULL AND episodeId IS NULL))")
    suspend fun delete(mediaId: String, episodeId: String?)
}
