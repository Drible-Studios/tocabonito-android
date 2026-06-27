pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TocaBonito"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:testing")
include(":feature:catalog")
include(":feature:player")
include(":feature:detail")
include(":feature:downloads")
include(":feature:settings")
include(":feature:mylist")
