plugins {
    alias(libs.plugins.kover)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.vextreme.vexlife.android.elements"
    compileSdk = 37

    defaultConfig { minSdk = 23 }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform(libs.composeBom))
    implementation(libs.composeFoundation)
    implementation(libs.composeMaterial3)
    implementation(libs.composeMaterialIconsExtended)
    implementation(libs.composeRuntime)
    implementation(libs.composeUi)

    testImplementation(libs.junit)
}
