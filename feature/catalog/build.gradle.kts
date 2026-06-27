plugins {
    id("tocabonito.android.feature")
}

android {
    namespace = "studios.drible.tocabonito.feature.catalog"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
}
