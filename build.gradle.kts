plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ksp) apply false
}


/**
 * Canonical repository-owned JVM/source verification.
 *
 * CI, host runners, and developers should call this task rather than duplicate its task list.
 * Android/device proof remains separate because it owns a different execution substrate.
 */
tasks.register("vexVerifyJvm") {
    group = "verification"
    description =
        "Run source alignment, reusable architecture contracts, and app JVM tests."

    dependsOn(
        ":app:verifySourceAlignment",
        ":architecture:elements:test",
        ":architecture:logging:test",
        ":architecture:relay:test",
        ":app:testDebugUnitTest",
    )
}

/**
 * Canonical local source/build checkpoint that does not require a connected Android target.
 */
tasks.register("vexVerifyLocal") {
    group = "verification"
    description =
        "Run canonical JVM/source verification and assemble the debug APK."

    dependsOn(
        "vexVerifyJvm",
        ":app:assembleDebug",
    )
}
