plugins {
    id("tocabonito.android.feature")
}

android {
    namespace = "studios.drible.tocabonito.feature.downloads"
}

dependencies {
    implementation(project(":core:data"))

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> { useJUnitPlatform() }
