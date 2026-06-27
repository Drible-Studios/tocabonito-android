package studios.drible.tocabonito.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import studios.drible.tocabonito.core.data.db.dao.DownloadDao
import studios.drible.tocabonito.core.data.db.dao.FavoriteDao
import studios.drible.tocabonito.core.data.db.dao.WatchProgressDao
import studios.drible.tocabonito.core.data.db.entity.DownloadEntity
import studios.drible.tocabonito.core.data.db.entity.FavoriteEntity
import studios.drible.tocabonito.core.data.db.entity.WatchProgressEntity

@Database(
    entities = [FavoriteEntity::class, WatchProgressEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TocaBonitoDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun downloadDao(): DownloadDao
}
