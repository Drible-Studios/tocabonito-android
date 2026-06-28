package studios.drible.tocabonito.core.data.sync

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType

private fun mediaItem(id: String = "tt123", title: String = "Movie A") = MediaItem(
    id = id,
    title = title,
    overview = "Overview",
    posterPath = null,
    backdropPath = null,
    mediaType = MediaType.MOVIE,
    releaseYear = 2024,
    voteAverage = 7.5,
    genreIds = listOf(28),
)

class FirebaseFavoritesSyncServiceTest : FunSpec({

    test("syncAdded stores document at correct path") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "user1")
        val client = FakeFirestoreClient()
        val service = FirebaseFavoritesSyncService(client, auth)

        service.syncAdded(mediaItem("tt001", "Film One"))

        val path = "users/user1/favorites"
        client.documents[path]!! shouldContainKey "tt001"
        client.documents[path]!!["tt001"]!!["title"] shouldBe "Film One"
    }

    test("syncRemoved deletes document at correct path") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "user1")
        val client = FakeFirestoreClient()
        val service = FirebaseFavoritesSyncService(client, auth)

        service.syncAdded(mediaItem("tt001"))
        service.syncRemoved("tt001")

        val path = "users/user1/favorites"
        client.documents[path]!!.containsKey("tt001") shouldBe false
    }

    test("syncAdded includes all media item fields in document") {
        val auth = FakeFirebaseAuthWrapper()
        auth.simulateSignIn(uid = "user1")
        val client = FakeFirestoreClient()
        val service = FirebaseFavoritesSyncService(client, auth)

        val item = mediaItem("tt002", "Test Movie")
        service.syncAdded(item)

        val doc = client.documents["users/user1/favorites"]!!["tt002"]!!
        doc["id"] shouldBe "tt002"
        doc["title"] shouldBe "Test Movie"
        doc["mediaType"] shouldBe "movie"
        doc["releaseYear"] shouldBe 2024
        doc["voteAverage"] shouldBe 7.5
        doc["genreIds"] shouldBe listOf(28)
    }
})
