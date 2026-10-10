plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.vextreme.vexlife.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.vextreme.vexlife.android"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.0.1-foundation"
    }

    buildFeatures {
        compose = true
        buildConfig = false
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
    implementation(project(":sdk:state"))
    implementation(libs.activityCompose)
    implementation(platform(libs.composeBom))
    implementation(libs.composeMaterial3)
    implementation(libs.composeUiToolingPreview)
    debugImplementation(libs.composeUiTooling)

    testImplementation(libs.junit)
}
