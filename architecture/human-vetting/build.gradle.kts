plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.vextreme.vexlife.android.humanvetting"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}
