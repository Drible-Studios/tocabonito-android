package studios.drible.tocabonito.core.data.download

class CircuitBreaker(
    private val maxFailures: Int,
    private val resetTimeoutMs: Long,
) {
    private var failureCount = 0
    private var lastFailureTime = 0L

    val isOpen: Boolean
        get() {
            if (failureCount < maxFailures) return false
            val elapsed = System.currentTimeMillis() - lastFailureTime
            if (elapsed >= resetTimeoutMs) {
                failureCount = 0
                return false
            }
            return true
        }

    fun recordFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
    }

    fun recordSuccess() {
        failureCount = 0
    }
}
