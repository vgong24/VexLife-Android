import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kover)
    alias(libs.plugins.screenshot)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.vextreme.vexlife.android"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    compileSdk = 37

    defaultConfig {
        applicationId = "com.vextreme.vexlife.android"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.0.1-foundation"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["useTestStorageService"] = "true"
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        named("test") {
            kotlin.directories += "src/journeyTest/kotlin"
        }
        named("androidTest") {
            kotlin.directories += "src/journeyTest/kotlin"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":architecture:behavior"))
    implementation(project(":architecture:elements"))
    implementation(project(":architecture:execution"))
    implementation(project(":architecture:logging"))
    implementation(project(":architecture:relay"))

    implementation(libs.activityCompose)
    implementation(platform(libs.composeBom))
    implementation(libs.composeFoundation)
    implementation(libs.composeMaterial3)
    implementation(libs.composeMaterialIconsExtended)
    implementation(libs.composeUiToolingPreview)
    debugImplementation(libs.composeUiTooling)
    debugImplementation(libs.composeUiTestManifest)

    implementation(libs.roomRuntime)
    implementation(libs.roomKtx)
    implementation(libs.coroutinesAndroid)
    implementation(libs.coreKtx)
    implementation(libs.lifecycleViewModelKtx)
    implementation(libs.lifecycleViewModelCompose)
    implementation(libs.lifecycleRuntimeCompose)
    implementation(libs.maplibreAndroid)
    implementation(libs.valhallaMobile)
    implementation(libs.valhallaModels)
    implementation(libs.valhallaModelsConfig)

    implementation(libs.media3Common)
    implementation(libs.media3Exoplayer)
    implementation(libs.media3Session)
    ksp(libs.roomCompiler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutinesTest)
    testImplementation(libs.roomTesting)

    androidTestImplementation(platform(libs.composeBom))
    androidTestImplementation(libs.composeUiTestJunit4)
    androidTestImplementation(libs.androidxTestRunner)
    androidTestImplementation(libs.androidxTestExtJunit)
    androidTestImplementation(libs.espressoCore)
    androidTestUtil(libs.androidxTestServices)

    screenshotTestImplementation(libs.screenshotValidationApi)
    screenshotTestImplementation(libs.composeUiTooling)
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<Test>().configureEach {
    val journeyCoverageReport =
        layout.buildDirectory.file(
            "reports/vex/journey-coverage-${name}.txt",
        )

    systemProperty(
        "vex.journey.coverageReport",
        journeyCoverageReport.get().asFile.absolutePath,
    )
    systemProperty(
        "vex.journey.strictCoverage",
        providers.gradleProperty("vexStrictJourneyCoverage")
            .orElse("false")
            .get(),
    )

    // Journey debt is intentionally visible during a green ordinary test run.
    testLogging.showStandardStreams = true
}


apply(from = rootProject.file("gradle/source-alignment.gradle.kts"))
