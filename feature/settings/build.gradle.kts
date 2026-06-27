plugins {
    id("tocabonito.android.feature")
}

android {
    namespace = "studios.drible.tocabonito.feature.settings"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.datastore.preferences)
}
