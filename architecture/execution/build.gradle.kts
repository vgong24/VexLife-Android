plugins {
    alias(libs.plugins.kover)
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.vextreme.vexlife.android.execution"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
    }

    testFixtures {
        enable = true
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
    implementation(libs.coroutinesCore)
    testFixturesImplementation(libs.coroutinesTest)
    testFixturesImplementation(libs.junit)
    testImplementation(libs.coroutinesTest)
    testImplementation(libs.junit)
}
