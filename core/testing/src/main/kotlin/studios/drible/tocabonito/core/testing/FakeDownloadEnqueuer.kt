package studios.drible.tocabonito.core.testing

import studios.drible.tocabonito.core.domain.repository.DownloadEnqueuer

class FakeDownloadEnqueuer : DownloadEnqueuer {
    val enqueuedIds = mutableListOf<String>()
    var shouldThrow: Exception? = null

    override suspend fun enqueue(downloadId: String) {
        shouldThrow?.let { throw it }
        enqueuedIds.add(downloadId)
    }
}
