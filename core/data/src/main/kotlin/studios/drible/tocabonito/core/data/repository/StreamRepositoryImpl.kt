package studios.drible.tocabonito.core.data.repository

import studios.drible.tocabonito.core.data.api.realdebrid.RealDebridClient
import studios.drible.tocabonito.core.data.api.torrentio.TorrentioClient
import studios.drible.tocabonito.core.data.stream.StreamSorter
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamOption
import studios.drible.tocabonito.core.domain.model.StreamQuality
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

    override suspend fun resolveStream(option: StreamOption): StreamLink {
        return if (option.resolverUrl != null) {
            resolveViaRedirect(option)
        } else {
            resolveViaFullFlow(option)
        }
    }

    override suspend fun resolveTranscode(option: StreamOption): StreamLink {
        val torrentId = ensureTorrentReady(option)
        val transcodeResult = realDebridClient.transcode(torrentId)
        val hlsUrl = transcodeResult.apple?.get("full")
            ?: transcodeResult.dash?.get("full")
            ?: error("No transcode stream available for torrent $torrentId")
        return StreamLink(
            id = torrentId,
            fileName = option.title,
            fileSize = 0,
            hlsUrl = hlsUrl,
            directUrl = hlsUrl,
            quality = StreamQuality.FULL,
        )
    }

    private suspend fun resolveViaRedirect(option: StreamOption): StreamLink {
        return realDebridClient.unrestrict(option.resolverUrl!!)
    }

    private suspend fun resolveViaFullFlow(option: StreamOption): StreamLink {
        val torrentId = ensureTorrentReady(option)
        val info = realDebridClient.torrentInfo(torrentId)
        val link = info.links.firstOrNull()
            ?: error("No links available after selecting files for torrent $torrentId")
        return realDebridClient.unrestrict(link)
    }

    private suspend fun ensureTorrentReady(option: StreamOption): String {
        val addResult = realDebridClient.addMagnet(option.magnetLink)
        val torrentId = addResult.id
        val info = realDebridClient.torrentInfo(torrentId)
        val videoFile = realDebridClient.selectVideoFile(info.files, option.fileIndex)
        if (videoFile != null) {
            realDebridClient.selectFiles(torrentId, listOf(videoFile.id))
        }
        return torrentId
    }
}
