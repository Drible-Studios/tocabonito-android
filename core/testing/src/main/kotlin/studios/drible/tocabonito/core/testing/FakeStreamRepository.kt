package studios.drible.tocabonito.core.testing

import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.StreamLink
import studios.drible.tocabonito.core.domain.model.StreamOption
import studios.drible.tocabonito.core.domain.repository.StreamRepository

class FakeStreamRepository : StreamRepository {
    var streamsResult: List<StreamOption> = emptyList()
    var resolveResult: StreamLink? = null
    var shouldThrow: Exception? = null

    override suspend fun availableStreams(
        imdbId: String,
        type: MediaType,
        season: Int?,
        episode: Int?
    ): List<StreamOption> {
        shouldThrow?.let { throw it }
        return streamsResult
    }

    override suspend fun resolveStream(option: StreamOption): StreamLink {
        shouldThrow?.let { throw it }
        return resolveResult ?: throw IllegalStateException("No resolve result configured")
    }
}
