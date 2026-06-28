plugins {
    id("tocabonito.android.application")
    id("tocabonito.android.compose")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "studios.drible.tocabonito"

    defaultConfig {
        applicationId = "studios.drible.tocabonito"
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "TMDB_API_KEY", "\"${project.findProperty("TMDB_API_KEY") ?: ""}\"")
        buildConfigField("String", "REAL_DEBRID_API_KEY", "\"${project.findProperty("REAL_DEBRID_API_KEY") ?: ""}\"")
        buildConfigField("String", "OPEN_SUBTITLES_API_KEY", "\"${project.findProperty("OPEN_SUBTITLES_API_KEY") ?: ""}\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:catalog"))
    implementation(project(":feature:player"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:downloads"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:mylist"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.work.runtime)
    implementation(libs.compose.activity)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.compose.icons.extended)
    implementation(libs.splashscreen)

    implementation(libs.room.runtime)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.play.services.auth)
}
