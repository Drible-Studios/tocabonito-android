package studios.drible.tocabonito.core.domain.service

import kotlinx.coroutines.flow.Flow

interface KeyValueStore {
    fun getString(key: String): String?
    fun getBoolean(key: String): Boolean
    fun getLong(key: String): Long
    fun set(key: String, value: Any?)
    fun remove(key: String)
}

interface KeyValueSyncStore {
    fun getString(key: String): String?
    fun getBoolean(key: String): Boolean
    fun getLong(key: String): Long
    fun set(key: String, value: Any?)
    fun remove(key: String)
    fun synchronize(): Boolean
    fun allEntries(): Map<String, Any>
}

interface NetworkConnectivity {
    fun observeIsConnected(): Flow<Boolean>
    fun observeIsMetered(): Flow<Boolean>
}

interface NotificationObserving {
    fun observe(name: String): Flow<Unit>
}
