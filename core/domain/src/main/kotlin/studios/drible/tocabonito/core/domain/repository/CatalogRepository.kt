package studios.drible.tocabonito.core.domain.repository

import studios.drible.tocabonito.core.domain.model.Episode
import studios.drible.tocabonito.core.domain.model.Genre
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.YearFilter

interface CatalogRepository {
    suspend fun trending(): List<MediaItem>
    suspend fun popular(type: MediaType): List<MediaItem>
    suspend fun topRated(type: MediaType): List<MediaItem>
    suspend fun search(query: String): List<MediaItem>
    suspend fun details(id: String, type: MediaType): MediaItem
    suspend fun seasons(seriesId: String): List<List<Episode>>
    suspend fun genres(type: MediaType): List<Genre>
    suspend fun discover(genreId: Int, type: MediaType, yearFilter: YearFilter?, page: Int): List<MediaItem>
}
