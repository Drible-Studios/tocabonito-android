plugins {
    id("tocabonito.android.feature")
}

android {
    namespace = "studios.drible.tocabonito.feature.detail"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.launcher)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> { useJUnitPlatform() }
