package studios.drible.tocabonito.core.data.download

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RDRateLimiter(private val maxRequestsPerMinute: Int) {
    private val mutex = Mutex()
    private val timestamps = ArrayDeque<Long>()

    suspend fun acquire() {
        while (true) {
            val waitMs = mutex.withLock {
                val now = System.currentTimeMillis()
                val windowStart = now - 60_000L
                while (timestamps.isNotEmpty() && timestamps.first() < windowStart) {
                    timestamps.removeFirst()
                }
                if (timestamps.size >= maxRequestsPerMinute) {
                    timestamps.first() + 60_000L - now
                } else {
                    timestamps.addLast(now)
                    0L
                }
            }
            if (waitMs <= 0L) return
            delay(waitMs)
        }
    }

    fun tryAcquire(): Boolean {
        val now = System.currentTimeMillis()
        val windowStart = now - 60_000L
        while (timestamps.isNotEmpty() && timestamps.first() < windowStart) {
            timestamps.removeFirst()
        }
        if (timestamps.size >= maxRequestsPerMinute) return false
        timestamps.addLast(now)
        return true
    }
}
