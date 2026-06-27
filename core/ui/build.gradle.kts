plugins {
    id("tocabonito.android.library")
    id("tocabonito.android.compose")
}

android {
    namespace = "studios.drible.tocabonito.core.ui"
}

dependencies {
    implementation(libs.compose.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
}
