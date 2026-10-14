import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VexLife-Android"
include(":app")
include(":architecture:elements")
include(":architecture:logging")
include(":architecture:relay")
// VexArchitecture is reusable semantic-directory vocabulary, not VexStream-owned product code.
include(":architecture:vexarchitecture")
