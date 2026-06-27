package studios.drible.tocabonito.core.data.download

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CircuitBreakerTest : FunSpec({

    test("circuit is closed initially") {
        val cb = CircuitBreaker(maxFailures = 3, resetTimeoutMs = 1000L)
        cb.isOpen shouldBe false
    }

    test("circuit remains closed below failure threshold") {
        val cb = CircuitBreaker(maxFailures = 3, resetTimeoutMs = 1000L)
        cb.recordFailure()
        cb.recordFailure()
        cb.isOpen shouldBe false
    }

    test("circuit opens after reaching max failures") {
        val cb = CircuitBreaker(maxFailures = 3, resetTimeoutMs = 60_000L)
        repeat(3) { cb.recordFailure() }
        cb.isOpen shouldBe true
    }

    test("circuit resets after timeout") {
        val cb = CircuitBreaker(maxFailures = 1, resetTimeoutMs = 1L)
        cb.recordFailure()
        Thread.sleep(10)
        cb.isOpen shouldBe false
    }

    test("recordSuccess resets failure count") {
        val cb = CircuitBreaker(maxFailures = 3, resetTimeoutMs = 60_000L)
        repeat(3) { cb.recordFailure() }
        cb.recordSuccess()
        cb.isOpen shouldBe false
    }
})
