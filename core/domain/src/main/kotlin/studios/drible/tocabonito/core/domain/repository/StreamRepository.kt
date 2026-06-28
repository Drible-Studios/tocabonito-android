package studios.drible.tocabonito.core.domain.repository

import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamOption

interface StreamRepository {
    suspend fun availableStreams(imdbId: String, type: MediaType, season: Int?, episode: Int?): List<StreamOption>
    suspend fun resolveStream(option: StreamOption): StreamLink
    suspend fun resolveTranscode(option: StreamOption): StreamLink
    /** Re-resolve a torrent's transcode link by torrent ID (e.g., after playback error). */
    suspend fun resolveTranscode(torrentId: String): StreamLink
}
