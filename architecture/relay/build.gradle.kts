import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
        progressiveMode.set(true)
    }
}

dependencies {
    api(project(":architecture:execution"))
    api(project(":architecture:observability"))
    api(libs.kotlinxCoroutinesCore)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
