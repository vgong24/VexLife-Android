import org.gradle.api.GradleException

/**
 * Static source-alignment checks.
 *
 * This intentionally does NOT prove runtime behavior. Unit/integration/device tests own questions
 * such as navigation correctness, Room migrations, transaction behavior, and persistence.
 *
 * These checks only answer: "did source drift away from the repository's declared shape?"
 */
val collectSourceAlignmentViolations: () -> List<String> = {
    val violations = mutableListOf<String>()
    val mainKotlin = project.file("src/main/kotlin")
    val conventionSourceRoots = listOf(
        mainKotlin,
        rootProject.file("architecture/elements/src/main/kotlin"),
    )
    val screenRoot = project.file(
        "src/main/kotlin/com/vextreme/vexlife/android/screens",
    )
    val appScreensFile = screenRoot.resolve("AppScreens.kt")
    val appScreensText = appScreensFile
        .takeIf { it.isFile }
        ?.readText()
        .orEmpty()

    if (!screenRoot.isDirectory) {
        violations += "missing screen source root: ${screenRoot.relativeTo(project.projectDir)}"
    } else {
        val screenFiles = screenRoot
            .walkTopDown()
            .filter { file ->
                file.isFile &&
                    file.name.endsWith("Screen.kt") &&
                    file.name != "AppScreens.kt"
            }
            .toList()

        screenFiles.forEach { file ->
            val screenName = file.nameWithoutExtension
            val source = file.readText()
            val relative = file.relativeTo(project.projectDir)

            if (!source.contains("object $screenName : AppScreenBlueprint")) {
                violations += "$relative should declare 'object $screenName : AppScreenBlueprint'"
            }

            if (!source.contains("@VexScreenPreviews")) {
                violations += "$relative is a Screen but has no colocated @VexScreenPreviews"
            }

            if (!source.contains("fun ${screenName}Preview(")) {
                violations +=
                    "$relative should expose the ordinary preview entry '${screenName}Preview()'"
            }

            if (!appScreensText.contains(screenName)) {
                violations += "$relative is not listed in screens/AppScreens.kt"
            }
        }
    }

    conventionSourceRoots.forEach { sourceRoot ->
        if (sourceRoot.isDirectory) {
            sourceRoot
                .walkTopDown()
                .filter { it.isFile && it.name.endsWith("Page.kt") }
                .forEach { file ->
                    violations +=
                        "${file.relativeTo(rootProject.projectDir)} uses retired Page screen vocabulary"
                }

            sourceRoot
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val source = file.readText()
                    if (source.contains("PageBlueprint")) {
                        violations +=
                            "${file.relativeTo(rootProject.projectDir)} references retired PageBlueprint"
                    }
                }
        }
    }

    val appComposition = project.file(
        "src/main/kotlin/com/vextreme/vexlife/android/ui/VexLifeApp.kt",
    )
    if (appComposition.isFile) {
        val source = appComposition.readText()
        if (
            source.contains("com.vextreme.vexlife.android.screens.primary.") ||
            source.contains("com.vextreme.vexlife.android.screens.shared.")
        ) {
            violations +=
                "ui/VexLifeApp.kt should render registered Screens through AppScreenBlueprint; " +
                    "feature Screen imports belong in AppScreens/owning Screen files"
        }
    }

    val previewSupport = project.file(
        "src/main/kotlin/com/vextreme/vexlife/android/preview/VexPreview.kt",
    )
    if (previewSupport.isFile) {
        val source = previewSupport.readText()
        if (
            source.contains("com.vextreme.vexlife.android.screens.primary.") ||
            source.contains("com.vextreme.vexlife.android.screens.shared.")
        ) {
            violations +=
                "preview/VexPreview.kt should own generic preview environment only; " +
                    "feature Screen scenarios belong in their *Screen.kt files"
        }
    }

    val dataRoot = project.file(
        "src/main/kotlin/com/vextreme/vexlife/android/data",
    )
    if (dataRoot.isDirectory) {
        dataRoot
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith("Entity.kt") }
            .forEach { file ->
                val source = file.readText()
                val relative = file.relativeTo(project.projectDir)

                val tableName = Regex(
                    """@Entity\s*\([^)]*tableName\s*=\s*"([^"]+)"""",
                    RegexOption.DOT_MATCHES_ALL,
                ).find(source)?.groupValues?.getOrNull(1)

                if (tableName == null) {
                    violations += "$relative should declare an explicit Room tableName"
                } else if (!tableName.matches(Regex("[a-z][a-z0-9_]*"))) {
                    violations += "$relative Room tableName '$tableName' should be snake_case"
                }

                Regex("""@ColumnInfo\s*\(\s*name\s*=\s*"([^"]+)"""")
                    .findAll(source)
                    .forEach { match ->
                        val columnName = match.groupValues[1]
                        if (!columnName.matches(Regex("[a-z][a-z0-9_]*"))) {
                            violations +=
                                "$relative Room column '$columnName' should be snake_case"
                        }
                    }

                val persistedProperties =
                    Regex("""(?m)^\s*val\s+[A-Za-z_][A-Za-z0-9_]*\s*:""")
                        .findAll(source)
                        .count()
                val explicitColumns =
                    Regex("""@ColumnInfo\s*\(\s*name\s*=""")
                        .findAll(source)
                        .count()

                if (persistedProperties > 0 && explicitColumns < persistedProperties) {
                    violations +=
                        "$relative has $persistedProperties persisted properties but only " +
                            "$explicitColumns explicit @ColumnInfo names"
                }
            }
    }

    violations.distinct()
}

tasks.register("sourceAlignmentReport") {
    group = "verification"
    description = "Warns when Screen/Preview/Room source conventions drift."

    doLast {
        val violations = collectSourceAlignmentViolations()
        if (violations.isEmpty()) {
            logger.lifecycle("Source alignment: OK")
        } else {
            violations.forEach { violation ->
                logger.warn("SOURCE ALIGNMENT: $violation")
            }
        }
    }
}

tasks.register("verifySourceAlignment") {
    group = "verification"
    description = "Fails when repository-owned source conventions drift."

    doLast {
        val violations = collectSourceAlignmentViolations()
        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Source alignment verification failed:")
                    violations.forEach { appendLine(" - $it") }
                    appendLine()
                    append(
                        "Run :app:sourceAlignmentReport for warning-only output.",
                    )
                },
            )
        }
    }
}

tasks.named("check").configure {
    dependsOn("verifySourceAlignment")
}
