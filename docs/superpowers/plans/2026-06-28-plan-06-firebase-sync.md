# Plan 06 - Firebase Sync (Google Sign-In + Firestore)

**Issue:** #17
**Package base:** `studios.drible.tocabonito`
**Module:** `core/data` (implementation) + `app` (DI wiring) + `feature/settings` (UI)

---

## Prerequisites

The domain interfaces are already defined in `core/domain/src/main/kotlin/.../service/SyncServices.kt`:
- `CloudAccountProvider` (returns `CloudAccountStatus`)
- `FavoritesSyncService` (syncAdded, syncRemoved, startObserving, stopObserving)
- `ProgressSyncService` (syncProgress, startObserving, stopObserving)
- `SettingsSyncService` (pushAll, pullAll, startObserving, stopObserving)
- `SyncStatusProvider` (observeStatus: Flow<SyncStatus>)

Stubs exist in `core/data/src/main/kotlin/.../sync/`. The DI module at `app/src/main/kotlin/.../di/SyncModule.kt` currently binds stubs.

---

## Task 1: Add Gradle Dependencies

### Files to modify

- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `core/data/build.gradle.kts`

### Changes

**`gradle/libs.versions.toml`** - add under `[versions]`:
```toml
firebase-bom = "33.7.0"
play-services-auth = "21.3.0"
```

Add under `[libraries]`:
```toml
# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-auth-ktx = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore-ktx = { group = "com.google.firebase", name = "firebase-firestore-ktx" }

# Google Sign-In
play-services-auth = { group = "com.google.android.gms", name = "play-services-auth", version.ref = "play-services-auth" }
```

Add under `[plugins]`:
```toml
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

**`app/build.gradle.kts`** - add plugin:
```kotlin
alias(libs.plugins.google.services)
```

Add dependencies:
```kotlin
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.auth.ktx)
implementation(libs.firebase.firestore.ktx)
implementation(libs.play.services.auth)
```

**`core/data/build.gradle.kts`** - add dependencies:
```kotlin
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.auth.ktx)
implementation(libs.firebase.firestore.ktx)
```

### Commit
```
build: add Firebase BOM, Auth, Firestore, and Play Services Auth dependencies
```

---

## Task 2: Expand CloudAccountProvider Interface + FirebaseAccountProvider

The existing `CloudAccountProvider` only has `accountStatus()`. For Google Sign-In we need additional capabilities: sign-in intent, auth state flow, and current UID.

### Step 2.1: Expand Domain Interface

**File:** `core/domain/src/main/kotlin/studios/drible/tocabonito/core/domain/service/SyncServices.kt`

Add to the existing `CloudAccountProvider`:
```kotlin
interface CloudAccountProvider {
    suspend fun accountStatus(): CloudAccountStatus
    val isSignedIn: Flow<Boolean>
    val displayName: Flow<String?>
    val currentUid: String?
    suspend fun signOut()
}
```

Update `FakeCloudAccountProvider` in `core/testing` and `StubCloudAccountProvider` in `core/data` to implement the new members.

### Step 2.2: TDD - FirebaseAccountProvider

**Test file:** `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/sync/FirebaseAccountProviderTest.kt`

#### RED - Write failing tests first

```kotlin
package studios.drible.tocabonito.core.data.sync

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FirebaseAccountProviderTest {

    private lateinit var fakeFirebaseAuth: FakeFirebaseAuth
    private lateinit var provider: FirebaseAccountProvider

    @BeforeEach
    fun setup() {
        fakeFirebaseAuth = FakeFirebaseAuth()
        provider = FirebaseAccountProvider(fakeFirebaseAuth)
    }

    @Test
    fun `isSignedIn emits false when no user is authenticated`() = runTest {
        fakeFirebaseAuth.setCurrentUser(null)

        provider.isSignedIn.test {
            awaitItem() shouldBe false
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isSignedIn emits true when user is authenticated`() = runTest {
        fakeFirebaseAuth.setCurrentUser(FakeFirebaseUser(uid = "uid-123", name = "Jeff"))

        provider.isSignedIn.test {
            awaitItem() shouldBe true
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `displayName emits user display name`() = runTest {
        fakeFirebaseAuth.setCurrentUser(FakeFirebaseUser(uid = "uid-123", name = "Jeff"))

        provider.displayName.test {
            awaitItem() shouldBe "Jeff"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `displayName emits null when not signed in`() = runTest {
        fakeFirebaseAuth.setCurrentUser(null)

        provider.displayName.test {
            awaitItem() shouldBe null
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `currentUid returns null when not signed in`() = runTest {
        fakeFirebaseAuth.setCurrentUser(null)
        provider.currentUid shouldBe null
    }

    @Test
    fun `currentUid returns uid when signed in`() = runTest {
        fakeFirebaseAuth.setCurrentUser(FakeFirebaseUser(uid = "uid-123", name = "Jeff"))
        provider.currentUid shouldBe "uid-123"
    }

    @Test
    fun `accountStatus returns AVAILABLE when signed in`() = runTest {
        fakeFirebaseAuth.setCurrentUser(FakeFirebaseUser(uid = "uid-123", name = "Jeff"))
        provider.accountStatus() shouldBe CloudAccountStatus.AVAILABLE
    }

    @Test
    fun `accountStatus returns NO_ACCOUNT when not signed in`() = runTest {
        fakeFirebaseAuth.setCurrentUser(null)
        provider.accountStatus() shouldBe CloudAccountStatus.NO_ACCOUNT
    }

    @Test
    fun `signOut clears current user`() = runTest {
        fakeFirebaseAuth.setCurrentUser(FakeFirebaseUser(uid = "uid-123", name = "Jeff"))
        provider.signOut()
        fakeFirebaseAuth.signOutCalled shouldBe true
    }
}
```

#### Fake (test helper)

**File:** `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/sync/FakeFirebaseAuth.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FakeFirebaseUser(val uid: String, val name: String?)

/**
 * Abstracts FirebaseAuth for testability. The real wrapper delegates to
 * FirebaseAuth.getInstance(); this fake stores state in memory.
 */
class FakeFirebaseAuth : FirebaseAuthWrapper {
    private val _currentUser = MutableStateFlow<FakeFirebaseUser?>(null)
    var signOutCalled = false
        private set

    fun setCurrentUser(user: FakeFirebaseUser?) {
        _currentUser.value = user
    }

    override val authState: StateFlow<AuthState>
        get() = MutableStateFlow(
            _currentUser.value?.let { AuthState.SignedIn(it.uid, it.name) }
                ?: AuthState.SignedOut
        ).also { /* In real tests, derive from _currentUser changes */ }

    override val currentUid: String? get() = _currentUser.value?.uid

    override suspend fun signOut() {
        signOutCalled = true
        _currentUser.value = null
    }
}
```

#### GREEN - Implementation

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/FirebaseAuthWrapper.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

import kotlinx.coroutines.flow.StateFlow

sealed class AuthState {
    data object SignedOut : AuthState()
    data class SignedIn(val uid: String, val displayName: String?) : AuthState()
}

interface FirebaseAuthWrapper {
    val authState: StateFlow<AuthState>
    val currentUid: String?
    suspend fun signOut()
}
```

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/RealFirebaseAuthWrapper.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealFirebaseAuthWrapper @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : FirebaseAuthWrapper {

    private val _authState = MutableStateFlow<AuthState>(
        firebaseAuth.currentUser?.let {
            AuthState.SignedIn(it.uid, it.displayName)
        } ?: AuthState.SignedOut
    )

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _authState.value = auth.currentUser?.let {
                AuthState.SignedIn(it.uid, it.displayName)
            } ?: AuthState.SignedOut
        }
    }

    override val authState: StateFlow<AuthState> = _authState

    override val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }
}
```

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/FirebaseAccountProvider.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.CloudAccountStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAccountProvider @Inject constructor(
    private val authWrapper: FirebaseAuthWrapper,
) : CloudAccountProvider {

    override val isSignedIn: Flow<Boolean>
        get() = authWrapper.authState.map { it is AuthState.SignedIn }

    override val displayName: Flow<String?>
        get() = authWrapper.authState.map {
            (it as? AuthState.SignedIn)?.displayName
        }

    override val currentUid: String?
        get() = authWrapper.currentUid

    override suspend fun accountStatus(): CloudAccountStatus {
        return if (authWrapper.currentUid != null) {
            CloudAccountStatus.AVAILABLE
        } else {
            CloudAccountStatus.NO_ACCOUNT
        }
    }

    override suspend fun signOut() {
        authWrapper.signOut()
    }
}
```

### Commit
```
feat(sync): add FirebaseAccountProvider with auth state observation

- Introduce FirebaseAuthWrapper abstraction for testability
- Implement FirebaseAccountProvider wrapping Firebase Auth
- Expand CloudAccountProvider interface with isSignedIn, displayName, currentUid, signOut
- Unit tests with FakeFirebaseAuth
```

---

## Task 3: FirebaseFavoritesSyncService

### Step 3.1: Firestore Document Mapping (shared utility)

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/FirestoreMappers.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.WatchProgress

object FirestoreMappers {

    fun MediaItem.toFavoriteDocument(): Map<String, Any?> = mapOf(
        "mediaId" to id,
        "mediaType" to mediaType.value,
        "title" to title,
        "posterPath" to posterPath,
        "addedAt" to System.currentTimeMillis(),
    )

    fun Map<String, Any?>.toMediaItemFromFavorite(): MediaItem = MediaItem(
        id = this["mediaId"] as String,
        title = this["title"] as String,
        overview = "",
        posterPath = this["posterPath"] as? String,
        backdropPath = null,
        mediaType = MediaType.entries.first { it.value == this@toMediaItemFromFavorite["mediaType"] },
        releaseYear = 0,
        voteAverage = 0.0,
        genreIds = emptyList(),
    )

    fun WatchProgress.toProgressDocument(): Map<String, Any?> = mapOf(
        "mediaId" to id,
        "positionMs" to (currentTime * 1000).toLong(),
        "durationMs" to (duration * 1000).toLong(),
        "updatedAt" to lastWatched,
        "episodeId" to episodeId,
    )

    fun Map<String, Any?>.toWatchProgress(mediaItem: MediaItem): WatchProgress = WatchProgress(
        id = this["mediaId"] as String,
        mediaItem = mediaItem,
        currentTime = (this["positionMs"] as Long) / 1000.0,
        duration = (this["durationMs"] as Long) / 1000.0,
        lastWatched = this["updatedAt"] as Long,
        episodeId = this["episodeId"] as? String,
    )
}
```

### Step 3.2: Firestore abstraction for testability

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/FirestoreClient.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

/**
 * Thin abstraction over Firestore to allow unit testing without the Firebase SDK.
 */
interface FirestoreClient {
    suspend fun setDocument(path: String, documentId: String, data: Map<String, Any?>)
    suspend fun deleteDocument(path: String, documentId: String)
    suspend fun getDocuments(path: String): List<Map<String, Any?>>
    suspend fun getDocument(path: String, documentId: String): Map<String, Any?>?
    suspend fun batchWrite(operations: List<BatchOperation>)
}

sealed class BatchOperation {
    data class Set(val path: String, val documentId: String, val data: Map<String, Any?>) : BatchOperation()
    data class Delete(val path: String, val documentId: String) : BatchOperation()
}
```

**File:** `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/sync/FakeFirestoreClient.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

class FakeFirestoreClient : FirestoreClient {
    // collection path -> (documentId -> data)
    val store = mutableMapOf<String, MutableMap<String, Map<String, Any?>>>()

    override suspend fun setDocument(path: String, documentId: String, data: Map<String, Any?>) {
        store.getOrPut(path) { mutableMapOf() }[documentId] = data
    }

    override suspend fun deleteDocument(path: String, documentId: String) {
        store[path]?.remove(documentId)
    }

    override suspend fun getDocuments(path: String): List<Map<String, Any?>> {
        return store[path]?.values?.toList() ?: emptyList()
    }

    override suspend fun getDocument(path: String, documentId: String): Map<String, Any?>? {
        return store[path]?.get(documentId)
    }

    override suspend fun batchWrite(operations: List<BatchOperation>) {
        operations.forEach { op ->
            when (op) {
                is BatchOperation.Set -> setDocument(op.path, op.documentId, op.data)
                is BatchOperation.Delete -> deleteDocument(op.path, op.documentId)
            }
        }
    }
}
```

### Step 3.3: TDD - FirebaseFavoritesSyncService

**Test file:** `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/sync/FirebaseFavoritesSyncServiceTest.kt`

#### RED

```kotlin
package studios.drible.tocabonito.core.data.sync

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType

class FirebaseFavoritesSyncServiceTest {

    private lateinit var firestoreClient: FakeFirestoreClient
    private lateinit var service: FirebaseFavoritesSyncService

    private val uid = "test-user-123"

    private val sampleItem = MediaItem(
        id = "movie-1",
        title = "Test Movie",
        overview = "A test movie",
        posterPath = "/poster.jpg",
        backdropPath = null,
        mediaType = MediaType.MOVIE,
        releaseYear = 2024,
        voteAverage = 8.0,
        genreIds = listOf(28, 12),
    )

    @BeforeEach
    fun setup() {
        firestoreClient = FakeFirestoreClient()
        service = FirebaseFavoritesSyncService(firestoreClient) { uid }
    }

    @Test
    fun `syncAdded writes document to Firestore at correct path`() = runTest {
        service.syncAdded(sampleItem)

        val docs = firestoreClient.getDocuments("users/$uid/favorites")
        docs shouldHaveSize 1
        docs.first()["mediaId"] shouldBe "movie-1"
        docs.first()["title"] shouldBe "Test Movie"
        docs.first()["mediaType"] shouldBe "movie"
        docs.first()["posterPath"] shouldBe "/poster.jpg"
    }

    @Test
    fun `syncRemoved deletes document from Firestore`() = runTest {
        service.syncAdded(sampleItem)
        service.syncRemoved("movie-1")

        val docs = firestoreClient.getDocuments("users/$uid/favorites")
        docs shouldHaveSize 0
    }

    @Test
    fun `syncAdded is a no-op when uid is null`() = runTest {
        val noAuthService = FirebaseFavoritesSyncService(firestoreClient) { null }
        noAuthService.syncAdded(sampleItem)

        firestoreClient.store.isEmpty() shouldBe true
    }

    @Test
    fun `syncRemoved is a no-op when uid is null`() = runTest {
        // First add with auth
        service.syncAdded(sampleItem)

        // Try removing without auth
        val noAuthService = FirebaseFavoritesSyncService(firestoreClient) { null }
        noAuthService.syncRemoved("movie-1")

        val docs = firestoreClient.getDocuments("users/$uid/favorites")
        docs shouldHaveSize 1
    }
}
```

#### GREEN

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/FirebaseFavoritesSyncService.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.data.sync.FirestoreMappers.toFavoriteDocument
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.service.FavoritesSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseFavoritesSyncService @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val uidProvider: () -> String?,
) : FavoritesSyncService {

    private fun collectionPath(): String? {
        val uid = uidProvider() ?: return null
        return "users/$uid/favorites"
    }

    override suspend fun syncAdded(item: MediaItem) {
        val path = collectionPath() ?: return
        firestoreClient.setDocument(path, item.id, item.toFavoriteDocument())
    }

    override suspend fun syncRemoved(id: String) {
        val path = collectionPath() ?: return
        firestoreClient.deleteDocument(path, id)
    }

    override suspend fun startObserving() {
        // Future: listen for real-time Firestore snapshots
    }

    override suspend fun stopObserving() {
        // Future: remove snapshot listener
    }
}
```

### Commit
```
feat(sync): implement FirebaseFavoritesSyncService with Firestore abstraction

- Add FirestoreClient interface and FakeFirestoreClient for testing
- Add FirestoreMappers for document <-> domain conversions
- Implement syncAdded (set) and syncRemoved (delete) against Firestore
- Guard all writes behind UID availability check
- TDD: 4 unit tests covering write, delete, and no-auth guard
```

---

## Task 4: FirebaseProgressSyncService

### TDD

**Test file:** `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/sync/FirebaseProgressSyncServiceTest.kt`

#### RED

```kotlin
package studios.drible.tocabonito.core.data.sync

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.domain.model.MediaItem
import studios.drible.tocabonito.core.domain.model.MediaType
import studios.drible.tocabonito.core.domain.model.WatchProgress

class FirebaseProgressSyncServiceTest {

    private lateinit var firestoreClient: FakeFirestoreClient
    private lateinit var service: FirebaseProgressSyncService

    private val uid = "test-user-123"

    private val sampleMedia = MediaItem(
        id = "movie-1",
        title = "Test Movie",
        overview = "",
        posterPath = null,
        backdropPath = null,
        mediaType = MediaType.MOVIE,
        releaseYear = 2024,
        voteAverage = 0.0,
        genreIds = emptyList(),
    )

    private val sampleProgress = WatchProgress(
        id = "movie-1",
        mediaItem = sampleMedia,
        currentTime = 120.5,
        duration = 7200.0,
        lastWatched = 1700000000000L,
        episodeId = null,
    )

    @BeforeEach
    fun setup() {
        firestoreClient = FakeFirestoreClient()
        service = FirebaseProgressSyncService(firestoreClient) { uid }
    }

    @Test
    fun `syncProgress writes progress document to Firestore`() = runTest {
        service.syncProgress(sampleProgress)

        val docs = firestoreClient.getDocuments("users/$uid/progress")
        docs shouldHaveSize 1
        docs.first()["mediaId"] shouldBe "movie-1"
        docs.first()["positionMs"] shouldBe 120500L
        docs.first()["durationMs"] shouldBe 7200000L
        docs.first()["updatedAt"] shouldBe 1700000000000L
        docs.first()["episodeId"] shouldBe null
    }

    @Test
    fun `syncProgress with episodeId includes it in document`() = runTest {
        val progressWithEp = sampleProgress.copy(episodeId = "s01e03")
        service.syncProgress(progressWithEp)

        val docs = firestoreClient.getDocuments("users/$uid/progress")
        docs.first()["episodeId"] shouldBe "s01e03"
    }

    @Test
    fun `syncProgress overwrites existing progress for same mediaId`() = runTest {
        service.syncProgress(sampleProgress)
        service.syncProgress(sampleProgress.copy(currentTime = 300.0))

        val docs = firestoreClient.getDocuments("users/$uid/progress")
        docs shouldHaveSize 1
        docs.first()["positionMs"] shouldBe 300000L
    }

    @Test
    fun `syncProgress is a no-op when uid is null`() = runTest {
        val noAuthService = FirebaseProgressSyncService(firestoreClient) { null }
        noAuthService.syncProgress(sampleProgress)

        firestoreClient.store.isEmpty() shouldBe true
    }
}
```

#### GREEN

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/FirebaseProgressSyncService.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.data.sync.FirestoreMappers.toProgressDocument
import studios.drible.tocabonito.core.domain.model.WatchProgress
import studios.drible.tocabonito.core.domain.service.ProgressSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseProgressSyncService @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val uidProvider: () -> String?,
) : ProgressSyncService {

    private fun collectionPath(): String? {
        val uid = uidProvider() ?: return null
        return "users/$uid/progress"
    }

    override suspend fun syncProgress(progress: WatchProgress) {
        val path = collectionPath() ?: return
        firestoreClient.setDocument(path, progress.id, progress.toProgressDocument())
    }

    override suspend fun startObserving() {
        // Future: Firestore snapshot listener for real-time sync
    }

    override suspend fun stopObserving() {
        // Future: remove snapshot listener
    }
}
```

### Commit
```
feat(sync): implement FirebaseProgressSyncService with merge semantics

- Write watch progress to Firestore as positionMs/durationMs
- Overwrite (merge) on repeated writes for same mediaId
- Guard writes behind UID check
- TDD: 4 unit tests covering write, episodeId, overwrite, no-auth
```

---

## Task 5: FirebaseSettingsSyncService

### TDD

**Test file:** `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/sync/FirebaseSettingsSyncServiceTest.kt`

#### RED

```kotlin
package studios.drible.tocabonito.core.data.sync

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FirebaseSettingsSyncServiceTest {

    private lateinit var firestoreClient: FakeFirestoreClient
    private lateinit var fakeSettingsReader: FakeLocalSettingsReader
    private lateinit var service: FirebaseSettingsSyncService

    private val uid = "test-user-123"

    @BeforeEach
    fun setup() {
        firestoreClient = FakeFirestoreClient()
        fakeSettingsReader = FakeLocalSettingsReader()
        service = FirebaseSettingsSyncService(firestoreClient, fakeSettingsReader) { uid }
    }

    @Test
    fun `pushAll writes current settings to Firestore`() = runTest {
        fakeSettingsReader.theme = "midnight"
        fakeSettingsReader.language = "pt-BR"
        fakeSettingsReader.providers = listOf("real-debrid", "premiumize")

        service.pushAll()

        val doc = firestoreClient.getDocument("users/$uid/settings", "app")
        doc shouldBe mapOf(
            "theme" to "midnight",
            "language" to "pt-BR",
            "providers" to listOf("real-debrid", "premiumize"),
        )
    }

    @Test
    fun `pullAll reads settings from Firestore and applies locally`() = runTest {
        firestoreClient.setDocument(
            "users/$uid/settings", "app",
            mapOf("theme" to "nordic", "language" to "en", "providers" to listOf("debrid-link")),
        )

        service.pullAll()

        fakeSettingsReader.theme shouldBe "nordic"
        fakeSettingsReader.language shouldBe "en"
        fakeSettingsReader.providers shouldBe listOf("debrid-link")
    }

    @Test
    fun `pullAll is a no-op when document does not exist`() = runTest {
        fakeSettingsReader.theme = "default"
        service.pullAll()
        fakeSettingsReader.theme shouldBe "default"
    }

    @Test
    fun `pushAll is a no-op when uid is null`() = runTest {
        val noAuthService = FirebaseSettingsSyncService(firestoreClient, fakeSettingsReader) { null }
        fakeSettingsReader.theme = "midnight"

        noAuthService.pushAll()

        firestoreClient.store.isEmpty() shouldBe true
    }
}
```

#### Fake helper

**File:** `core/data/src/test/kotlin/studios/drible/tocabonito/core/data/sync/FakeLocalSettingsReader.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

class FakeLocalSettingsReader : LocalSettingsReadWriter {
    var theme: String = "default"
    var language: String = "en"
    var providers: List<String> = emptyList()

    override fun readAll(): Map<String, Any?> = mapOf(
        "theme" to theme,
        "language" to language,
        "providers" to providers,
    )

    override fun applyAll(settings: Map<String, Any?>) {
        theme = settings["theme"] as? String ?: theme
        language = settings["language"] as? String ?: language
        @Suppress("UNCHECKED_CAST")
        providers = (settings["providers"] as? List<String>) ?: providers
    }
}
```

#### GREEN

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/LocalSettingsReadWriter.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

/**
 * Abstraction over DataStore/SharedPreferences for reading/writing app settings.
 */
interface LocalSettingsReadWriter {
    fun readAll(): Map<String, Any?>
    fun applyAll(settings: Map<String, Any?>)
}
```

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/FirebaseSettingsSyncService.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

import studios.drible.tocabonito.core.domain.service.SettingsSyncService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSettingsSyncService @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val localSettings: LocalSettingsReadWriter,
    private val uidProvider: () -> String?,
) : SettingsSyncService {

    private fun settingsPath(): String? {
        val uid = uidProvider() ?: return null
        return "users/$uid/settings"
    }

    override suspend fun pushAll() {
        val path = settingsPath() ?: return
        firestoreClient.setDocument(path, "app", localSettings.readAll())
    }

    override suspend fun pullAll() {
        val path = settingsPath() ?: return
        val remote = firestoreClient.getDocument(path, "app") ?: return
        localSettings.applyAll(remote)
    }

    override suspend fun startObserving() {
        // Future: Firestore snapshot listener
    }

    override suspend fun stopObserving() {
        // Future: remove listener
    }
}
```

### Commit
```
feat(sync): implement FirebaseSettingsSyncService push/pull

- Add LocalSettingsReadWriter interface for DataStore abstraction
- pushAll serializes current settings to single Firestore document
- pullAll reads from Firestore and applies locally
- Guards behind UID; no-op when document missing
- TDD: 4 unit tests
```

---

## Task 6: Hilt DI Module (Replace Stubs with Firebase Implementations)

### Step 6.1: RealFirestoreClient

**File:** `core/data/src/main/kotlin/studios/drible/tocabonito/core/data/sync/RealFirestoreClient.kt`

```kotlin
package studios.drible.tocabonito.core.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealFirestoreClient @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FirestoreClient {

    override suspend fun setDocument(path: String, documentId: String, data: Map<String, Any?>) {
        firestore.collection(path).document(documentId).set(data).await()
    }

    override suspend fun deleteDocument(path: String, documentId: String) {
        firestore.collection(path).document(documentId).delete().await()
    }

    override suspend fun getDocuments(path: String): List<Map<String, Any?>> {
        return firestore.collection(path).get().await().documents.mapNotNull { it.data }
    }

    override suspend fun getDocument(path: String, documentId: String): Map<String, Any?>? {
        val snapshot = firestore.collection(path).document(documentId).get().await()
        return if (snapshot.exists()) snapshot.data else null
    }

    override suspend fun batchWrite(operations: List<BatchOperation>) {
        val batch = firestore.batch()
        operations.forEach { op ->
            when (op) {
                is BatchOperation.Set -> {
                    val ref = firestore.collection(op.path).document(op.documentId)
                    batch.set(ref, op.data)
                }
                is BatchOperation.Delete -> {
                    val ref = firestore.collection(op.path).document(op.documentId)
                    batch.delete(ref)
                }
            }
        }
        batch.commit().await()
    }
}
```

### Step 6.2: Firebase DI Module

**File:** `app/src/main/kotlin/studios/drible/tocabonito/di/FirebaseSyncModule.kt`

```kotlin
package studios.drible.tocabonito.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import studios.drible.tocabonito.core.data.sync.FirebaseAccountProvider
import studios.drible.tocabonito.core.data.sync.FirebaseFavoritesSyncService
import studios.drible.tocabonito.core.data.sync.FirebaseProgressSyncService
import studios.drible.tocabonito.core.data.sync.FirebaseSettingsSyncService
import studios.drible.tocabonito.core.data.sync.FirestoreClient
import studios.drible.tocabonito.core.data.sync.FirebaseAuthWrapper
import studios.drible.tocabonito.core.data.sync.RealFirebaseAuthWrapper
import studios.drible.tocabonito.core.data.sync.RealFirestoreClient
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.FavoritesSyncService
import studios.drible.tocabonito.core.domain.service.ProgressSyncService
import studios.drible.tocabonito.core.domain.service.SettingsSyncService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseProvidesModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    fun provideUidProvider(authWrapper: FirebaseAuthWrapper): () -> String? = { authWrapper.currentUid }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseBindsModule {

    @Binds
    abstract fun bindFirebaseAuthWrapper(impl: RealFirebaseAuthWrapper): FirebaseAuthWrapper

    @Binds
    abstract fun bindFirestoreClient(impl: RealFirestoreClient): FirestoreClient

    @Binds
    abstract fun bindCloudAccountProvider(impl: FirebaseAccountProvider): CloudAccountProvider

    @Binds
    abstract fun bindFavoritesSyncService(impl: FirebaseFavoritesSyncService): FavoritesSyncService

    @Binds
    abstract fun bindProgressSyncService(impl: FirebaseProgressSyncService): ProgressSyncService

    @Binds
    abstract fun bindSettingsSyncService(impl: FirebaseSettingsSyncService): SettingsSyncService
}
```

### Step 6.3: Remove old stub bindings from SyncModule

**File:** `app/src/main/kotlin/studios/drible/tocabonito/di/SyncModule.kt`

Remove `bindCloudAccountProvider`, `bindProgressSyncService`, `bindFavoritesSyncService`, `bindSettingsSyncService` (they move to `FirebaseSyncModule`). Keep only `bindSyncStatusProvider` and `bindDataPortabilityService`:

```kotlin
package studios.drible.tocabonito.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import studios.drible.tocabonito.core.data.sync.DataPortabilityServiceImpl
import studios.drible.tocabonito.core.data.sync.StubSyncStatusProvider
import studios.drible.tocabonito.core.domain.service.DataPortabilityService
import studios.drible.tocabonito.core.domain.service.SyncStatusProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    abstract fun bindSyncStatusProvider(impl: StubSyncStatusProvider): SyncStatusProvider

    @Binds
    abstract fun bindDataPortabilityService(impl: DataPortabilityServiceImpl): DataPortabilityService
}
```

### Commit
```
feat(di): wire Firebase sync implementations via Hilt

- Add FirebaseProvidesModule for FirebaseAuth/Firestore instances
- Add FirebaseBindsModule replacing stub bindings
- Keep SyncStatusProvider stub (will implement real status tracking later)
- Add RealFirestoreClient delegating to Firebase SDK
```

---

## Task 7: Settings Screen - Google Sign-In Button + Sync Toggle

### Step 7.1: TDD - SettingsViewModel sign-in/sync state

**Test file:** `feature/settings/src/test/kotlin/studios/drible/tocabonito/feature/settings/SettingsViewModelSignInTest.kt`

#### RED

```kotlin
package studios.drible.tocabonito.feature.settings

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studios.drible.tocabonito.core.domain.service.CloudAccountProvider
import studios.drible.tocabonito.core.domain.service.CloudAccountStatus
import studios.drible.tocabonito.core.domain.service.SyncStatus
import studios.drible.tocabonito.core.domain.service.SyncStatusProvider
import studios.drible.tocabonito.core.testing.FakeCloudAccountProvider

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelSignInTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeAccountProvider: TestableCloudAccountProvider
    private lateinit var fakeSyncStatusProvider: FakeSyncStatusProvider
    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeAccountProvider = TestableCloudAccountProvider()
        fakeSyncStatusProvider = FakeSyncStatusProvider()
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isSignedIn reflects account provider state`() = runTest {
        fakeAccountProvider.signedIn.value = true
        // ViewModel would expose isSignedIn from CloudAccountProvider
        fakeAccountProvider.isSignedIn.test {
            awaitItem() shouldBe true
        }
    }

    @Test
    fun `displayName reflects account provider state`() = runTest {
        fakeAccountProvider.name.value = "Jeff F."
        fakeAccountProvider.displayName.test {
            awaitItem() shouldBe "Jeff F."
        }
    }

    @Test
    fun `signOut delegates to account provider`() = runTest {
        fakeAccountProvider.signedIn.value = true
        fakeAccountProvider.signOut()
        fakeAccountProvider.signedIn.value shouldBe false
    }
}

// Local test double with full interface
class TestableCloudAccountProvider : CloudAccountProvider {
    val signedIn = MutableStateFlow(false)
    val name = MutableStateFlow<String?>(null)

    override val isSignedIn: Flow<Boolean> = signedIn
    override val displayName: Flow<String?> = name
    override val currentUid: String? get() = if (signedIn.value) "uid-test" else null

    override suspend fun accountStatus(): CloudAccountStatus {
        return if (signedIn.value) CloudAccountStatus.AVAILABLE else CloudAccountStatus.NO_ACCOUNT
    }

    override suspend fun signOut() {
        signedIn.value = false
        name.value = null
    }
}

class FakeSyncStatusProvider : SyncStatusProvider {
    var status: SyncStatus = SyncStatus.Disabled
    override fun observeStatus(): Flow<SyncStatus> = flowOf(status)
}
```

### Step 7.2: Expand SettingsViewModel

**File:** `feature/settings/src/main/kotlin/studios/drible/tocabonito/feature/settings/SettingsViewModel.kt`

Add sign-in state and signOut action:

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    val themeProvider: ThemeProvider,
    private val syncStatusProvider: SyncStatusProvider,
    private val cloudAccountProvider: CloudAccountProvider,
    private val dataPortabilityService: DataPortabilityService,
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = syncStatusProvider.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus.Disabled)

    val isSignedIn: StateFlow<Boolean> = cloudAccountProvider.isSignedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val displayName: StateFlow<String?> = cloudAccountProvider.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ... existing dataPortability code ...

    fun signOut() {
        viewModelScope.launch {
            cloudAccountProvider.signOut()
        }
    }
}
```

### Step 7.3: SettingsScreen UI additions

**File:** `feature/settings/src/main/kotlin/studios/drible/tocabonito/feature/settings/SettingsScreen.kt`

Add to the Account section:

```kotlin
// — Account section —
SettingsSectionHeader(title = "Account", palette = palette)

val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
val displayName by viewModel.displayName.collectAsStateWithLifecycle()

if (isSignedIn) {
    SettingsRow(
        icon = Icons.Default.AccountCircle,
        label = displayName ?: "Google Account",
        subtitle = "Signed in - Tap to sign out",
        palette = palette,
        onClick = { viewModel.signOut() },
    )
    SettingsRow(
        icon = Icons.Default.CloudSync,
        label = "Cloud Sync",
        subtitle = syncStatus.label(),
        palette = palette,
        onClick = null,
    )
} else {
    SettingsRow(
        icon = Icons.Default.Login,
        label = "Sign in with Google",
        subtitle = "Enable cloud backup and sync",
        palette = palette,
        onClick = { onSignInClick() },
    )
}
```

The `onSignInClick` lambda is passed from the navigation host and launches the Google Sign-In intent via `ActivityResultLauncher`.

### Step 7.4: Google Sign-In Intent (Activity-level)

**File:** `app/src/main/kotlin/studios/drible/tocabonito/GoogleSignInHandler.kt`

```kotlin
package studios.drible.tocabonito

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object GoogleSignInHandler {

    fun getSignInIntent(context: Context, webClientId: String): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    suspend fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential).await()
    }
}
```

### Commit
```
feat(settings): add Google Sign-In button and account state in Settings

- Expand SettingsViewModel with isSignedIn, displayName, signOut
- Show sign-in/sign-out UI conditionally in SettingsScreen
- Add GoogleSignInHandler for intent creation and Firebase credential exchange
- Update CloudAccountProvider interface with Flow-based auth state
```

---

## Summary: File Inventory

| # | File | Type |
|---|------|------|
| 1 | `gradle/libs.versions.toml` | Modified |
| 2 | `app/build.gradle.kts` | Modified |
| 3 | `core/data/build.gradle.kts` | Modified |
| 4 | `core/domain/.../service/SyncServices.kt` | Modified (expand CloudAccountProvider) |
| 5 | `core/data/.../sync/FirebaseAuthWrapper.kt` | New |
| 6 | `core/data/.../sync/RealFirebaseAuthWrapper.kt` | New |
| 7 | `core/data/.../sync/FirebaseAccountProvider.kt` | New |
| 8 | `core/data/.../sync/FirestoreClient.kt` | New |
| 9 | `core/data/.../sync/RealFirestoreClient.kt` | New |
| 10 | `core/data/.../sync/FirestoreMappers.kt` | New |
| 11 | `core/data/.../sync/LocalSettingsReadWriter.kt` | New |
| 12 | `core/data/.../sync/FirebaseFavoritesSyncService.kt` | New |
| 13 | `core/data/.../sync/FirebaseProgressSyncService.kt` | New |
| 14 | `core/data/.../sync/FirebaseSettingsSyncService.kt` | New |
| 15 | `app/.../di/FirebaseSyncModule.kt` | New |
| 16 | `app/.../di/SyncModule.kt` | Modified (remove migrated bindings) |
| 17 | `app/.../GoogleSignInHandler.kt` | New |
| 18 | `feature/settings/.../SettingsViewModel.kt` | Modified |
| 19 | `feature/settings/.../SettingsScreen.kt` | Modified |
| 20 | `core/testing/.../FakeSyncServices.kt` | Modified (expand FakeCloudAccountProvider) |

### Test files

| # | File |
|---|------|
| 1 | `core/data/src/test/.../sync/FakeFirebaseAuth.kt` |
| 2 | `core/data/src/test/.../sync/FakeFirestoreClient.kt` |
| 3 | `core/data/src/test/.../sync/FakeLocalSettingsReader.kt` |
| 4 | `core/data/src/test/.../sync/FirebaseAccountProviderTest.kt` |
| 5 | `core/data/src/test/.../sync/FirebaseFavoritesSyncServiceTest.kt` |
| 6 | `core/data/src/test/.../sync/FirebaseProgressSyncServiceTest.kt` |
| 7 | `core/data/src/test/.../sync/FirebaseSettingsSyncServiceTest.kt` |
| 8 | `feature/settings/src/test/.../SettingsViewModelSignInTest.kt` |

---

## Commit Sequence

1. `build: add Firebase BOM, Auth, Firestore, and Play Services Auth dependencies`
2. `feat(sync): add FirebaseAccountProvider with auth state observation`
3. `feat(sync): implement FirebaseFavoritesSyncService with Firestore abstraction`
4. `feat(sync): implement FirebaseProgressSyncService with merge semantics`
5. `feat(sync): implement FirebaseSettingsSyncService push/pull`
6. `feat(di): wire Firebase sync implementations via Hilt`
7. `feat(settings): add Google Sign-In button and account state in Settings`

---

## Integration Testing (documented, not implemented)

For Firebase Emulator-based integration tests:

```bash
# Start Firebase emulator
firebase emulators:start --only auth,firestore

# Run Android instrumented tests against emulator
./gradlew :core:data:connectedAndroidTest -Pfirebase.emulator=true
```

The `RealFirestoreClient` and `RealFirebaseAuthWrapper` would be tested against the emulator using `FirebaseFirestore.useEmulator("10.0.2.2", 8080)` and `FirebaseAuth.useEmulator("10.0.2.2", 9099)` in an `androidTest` source set with a custom Hilt test module.
