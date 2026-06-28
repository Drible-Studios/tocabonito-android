package studios.drible.tocabonito.core.data.sync

interface LocalSettingsReadWriter {
    suspend fun readAll(): Map<String, Any?>
    suspend fun applyAll(settings: Map<String, Any?>)
}
