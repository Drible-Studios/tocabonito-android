package studios.drible.tocabonito.core.data.sync

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.WatchProgress

private fun mediaItem(id: String = "tt123") = MediaItem(
    id = id,
    title = "Movie",
    overview = "Overview",
    posterPath = null,
    backdropPath = null,
    mediaType = MediaType.MOVIE,
    releaseYear = 2024,
    voteAverage = 7.5,
    genreIds = listOf(28),
)

private fun progress(
    mediaId: String = "tt123",
    episodeId: String? = null,
    lastWatched: Long = 1_000L,
    currentTime: Double = 300.0,
) = WatchProgress(
    id = "$mediaId:$episodeId",
    mediaItem = mediaItem(mediaId),
    currentTime = currentTime,
    duration = 5400.0,
    lastWatched = lastWatched,
    episodeId = episodeId,
)

class FirebaseProgressSyncServiceTest : FunSpec({

    test("syncProgress writes document when no remote exists") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "user1")
        val client = FakeFirestoreClient()
        val service = FirebaseProgressSyncService(client, auth)

        service.syncProgress(progress("tt001", lastWatched = 1000L))

        val path = "users/user1/progress"
        client.documents[path]!! shouldContainKey "tt001:null"
    }

    test("syncProgress overwrites when local is newer") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "user1")
        val client = FakeFirestoreClient()
        // Pre-seed remote with older timestamp
        client.setDocument("users/user1/progress", "tt001:null", mapOf("lastWatched" to 500L, "currentTime" to 100.0))
        val service = FirebaseProgressSyncService(client, auth)

        service.syncProgress(progress("tt001", lastWatched = 1000L, currentTime = 300.0))

        val doc = client.documents["users/user1/progress"]!!["tt001:null"]!!
        doc["lastWatched"] shouldBe 1000L
        doc["currentTime"] shouldBe 300.0
    }

    test("syncProgress does not overwrite when remote is newer") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "user1")
        val client = FakeFirestoreClient()
        // Pre-seed remote with newer timestamp
        client.setDocument("users/user1/progress", "tt001:null", mapOf("lastWatched" to 2000L, "currentTime" to 500.0))
        val service = FirebaseProgressSyncService(client, auth)

        service.syncProgress(progress("tt001", lastWatched = 1000L, currentTime = 300.0))

        val doc = client.documents["users/user1/progress"]!!["tt001:null"]!!
        doc["lastWatched"] shouldBe 2000L
        doc["currentTime"] shouldBe 500.0
    }

    test("syncProgress uses correct path with user uid") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "abc-def")
        val client = FakeFirestoreClient()
        val service = FirebaseProgressSyncService(client, auth)

        service.syncProgress(progress("tt002", episodeId = "s01e03", lastWatched = 100L))

        client.documents.containsKey("users/abc-def/progress") shouldBe true
        client.documents["users/abc-def/progress"]!! shouldContainKey "tt002:s01e03"
    }
})
