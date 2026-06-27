plugins {
    id("tocabonito.kotlin.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
    testImplementation(libs.kotest.assertions)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
