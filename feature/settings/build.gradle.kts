plugins {
    id("tocabonito.android.feature")
}

android {
    namespace = "studios.drible.tocabonito.feature.settings"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.datastore.preferences)
    implementation(libs.compose.icons.extended)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.json)
}

tasks.withType<Test> { useJUnitPlatform() }
