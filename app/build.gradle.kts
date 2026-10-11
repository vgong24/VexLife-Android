plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
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
    implementation(project(":architecture:elements"))
    implementation(project(":architecture:logging"))

    implementation(libs.activityCompose)
    implementation(platform(libs.composeBom))
    implementation(libs.composeFoundation)
    implementation(libs.composeMaterial3)
    implementation(libs.composeMaterialIconsExtended)
    implementation(libs.composeUiToolingPreview)
    debugImplementation(libs.composeUiTooling)

    implementation(libs.roomRuntime)
    implementation(libs.roomKtx)
    implementation(libs.coroutinesAndroid)
    implementation(libs.coreKtx)
    ksp(libs.roomCompiler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutinesTest)
    testImplementation(libs.roomTesting)
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}


apply(from = rootProject.file("gradle/source-alignment.gradle.kts"))
