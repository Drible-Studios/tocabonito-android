package studios.drible.tocabonito.core.data.sync

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubLocalSettingsReadWriter @Inject constructor() : LocalSettingsReadWriter {
    override suspend fun readAll(): Map<String, Any?> = emptyMap()
    override suspend fun applyAll(settings: Map<String, Any?>) = Unit
}
