package studios.drible.tocabonito.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val mediaType: String,
    val releaseYear: Int,
    val voteAverage: Double,
    val genreIds: String,
    val addedAt: Long,
)
