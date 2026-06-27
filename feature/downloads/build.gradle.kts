plugins {
    id("tocabonito.android.feature")
}

android {
    namespace = "studios.drible.tocabonito.feature.downloads"
}

dependencies {
    implementation(project(":core:data"))
}
