package studios.drible.tocabonito.core.data.repository

import studios.drible.tocabonito.core.data.api.realdebrid.RealDebridClient
import studios.drible.tocabonito.core.data.api.torrentio.TorrentioClient
import studios.drible.tocabonito.core.data.stream.StreamSorter
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamOption
import studios.drible.tocabonito.core.domain.repository.StreamRepository
import javax.inject.Inject

class StreamRepositoryImpl @Inject constructor(
    private val torrentioClient: TorrentioClient,
    private val realDebridClient: RealDebridClient,
) : StreamRepository {

    override suspend fun availableStreams(
        imdbId: String,
        type: MediaType,
        season: Int?,
        episode: Int?,
    ): List<StreamOption> {
        val streams = torrentioClient.streams(imdbId, type.value, season, episode)
        return StreamSorter.sort(streams)
    }

    override suspend fun resolveStream(option: StreamOption): StreamLink =
        realDebridClient.unrestrict(option.magnetLink)
}
