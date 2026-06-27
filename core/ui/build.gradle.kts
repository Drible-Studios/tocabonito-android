plugins {
    id("tocabonito.android.library")
    id("tocabonito.android.compose")
}

android {
    namespace = "studios.drible.tocabonito.core.ui"
}

dependencies {
    implementation("javax.inject:javax.inject:1")
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.viewmodel)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> { useJUnitPlatform() }
