package studios.drible.tocabonito.core.data.download

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RDRateLimiterTest : FunSpec({

    test("tryAcquire returns true when under limit") {
        val limiter = RDRateLimiter(maxRequestsPerMinute = 5)
        limiter.tryAcquire() shouldBe true
    }

    test("tryAcquire returns false when at limit") {
        val limiter = RDRateLimiter(maxRequestsPerMinute = 3)
        repeat(3) { limiter.tryAcquire() }
        limiter.tryAcquire() shouldBe false
    }

    test("tryAcquire allows up to max requests") {
        val limiter = RDRateLimiter(maxRequestsPerMinute = 5)
        val results = (1..5).map { limiter.tryAcquire() }
        results.all { it } shouldBe true
    }

    test("tryAcquire blocks after limit reached") {
        val limiter = RDRateLimiter(maxRequestsPerMinute = 2)
        limiter.tryAcquire()
        limiter.tryAcquire()
        limiter.tryAcquire() shouldBe false
    }
})
