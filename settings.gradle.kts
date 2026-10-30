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
include(":architecture:behavior")
include(":architecture:elements")
include(":architecture:execution")
include(":architecture:human-vetting")
include(":architecture:logging")
include(":architecture:relay")
include(":architecture:widget")
