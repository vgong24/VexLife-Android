plugins {
    alias(libs.plugins.kover)
    alias(libs.plugins.screenshot) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ksp) apply false
}

dependencies {
    kover(project(":app"))
    kover(project(":architecture:behavior"))
    kover(project(":architecture:elements"))
    kover(project(":architecture:execution"))
    kover(project(":architecture:logging"))
    kover(project(":architecture:relay"))
    kover(project(":architecture:widget"))
}

apply(
    from =
        rootProject.file(
            "gradle/test-foundation.gradle.kts",
        ),
)
