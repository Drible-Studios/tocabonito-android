package studios.drible.tocabonito.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val id: String,
    val mediaId: String,
    val mediaTitle: String,
    val mediaPosterPath: String?,
    val mediaBackdropPath: String?,
    val mediaType: String,
    val mediaReleaseYear: Int,
    val mediaVoteAverage: Double,
    val mediaGenreIds: String,
    val episodeId: String?,
    val currentTime: Double,
    val duration: Double,
    val lastWatched: Long,
)
