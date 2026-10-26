plugins {
    alias(libs.plugins.kover)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.vextreme.vexlife.android.widget"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform(libs.composeBom))
    implementation(libs.composeRuntime)
    implementation(libs.glance)
    implementation(libs.glanceAppwidget)
    implementation("androidx.datastore:datastore-preferences-core:1.2.1")

    testImplementation(libs.junit)
}
