plugins {
    id("tocabonito.android.feature")
}

android {
    namespace = "studios.drible.tocabonito.feature.player"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    // Legacy PlayerView for AndroidView interop
    implementation("androidx.media3:media3-ui:1.6.0")
    implementation(libs.compose.icons.extended)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.launcher)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> { useJUnitPlatform() }
