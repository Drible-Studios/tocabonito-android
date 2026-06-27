plugins {
    id("tocabonito.kotlin.library")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.test)
    implementation(libs.turbine)
    implementation(libs.junit5.api)
    implementation(libs.kotest.assertions)
    implementation(libs.kotest.property)
}
