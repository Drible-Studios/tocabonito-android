plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// Force dependency versions to ones that are fully cached locally.
// This is required because the build machine is behind an authenticating proxy
// and cannot download artifacts not yet in the Gradle cache.
subprojects {
    configurations.all {
        resolutionStrategy.force(
            // activity group — navigation-compose:2.8.5 constrains to 1.8.2 (not cached)
            "androidx.activity:activity:1.9.3",
            "androidx.activity:activity-ktx:1.9.3",
            "androidx.activity:activity-compose:1.9.3",
            // core group — room/hilt constrain to 1.13.0 (no .aar cached); 1.13.1 is cached
            "androidx.core:core:1.13.1",
            "androidx.core:core-ktx:1.13.1"
        )
    }
}
