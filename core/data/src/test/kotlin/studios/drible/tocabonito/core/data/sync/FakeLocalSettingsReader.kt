package studios.drible.tocabonito.core.data.sync

class FakeLocalSettingsReadWriter : LocalSettingsReadWriter {
    var settings = mutableMapOf<String, Any?>()

    override suspend fun readAll(): Map<String, Any?> = settings.toMap()

    override suspend fun applyAll(settings: Map<String, Any?>) {
        this.settings.clear()
        this.settings.putAll(settings)
    }
}
