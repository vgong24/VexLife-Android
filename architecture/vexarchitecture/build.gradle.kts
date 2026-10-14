plugins {
    // Keep the module on the same repository-owned Android/Kotlin toolchain as the other
    // architecture modules without adding Compose or app dependencies.
    alias(libs.plugins.kover)
    alias(libs.plugins.androidLibrary)
}

android {
    // This namespace owns only the reusable VexArchitecture grammar.
    namespace = "com.vextreme.vexlife.android.vexarchitecture"
    compileSdk = 37

    defaultConfig {
        // The contracts use no platform APIs, so retain the reusable architecture minSdk.
        minSdk = 23
    }

    compileOptions {
        // JDK 17 is already the repository-owned compilation boundary.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    // Do not create a second JDK choice for this foundation.
    jvmToolchain(17)
}

dependencies {
    // Core contract proof is ordinary JVM proof; Android/device behavior belongs to consumers.
    testImplementation(libs.junit)
}
