package com.vextreme.vexlife.android.testing.journey

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VexJourneyCoverageTest {
    @Test
    fun reportSemanticCoverageDebtWithoutBlockingOrdinaryDevelopment() {
        val duplicateTitles = VexJourneyCatalog.all
            .groupBy { it.title }
            .filterValues { it.size > 1 }
            .keys
        assertTrue(
            "journey titles must be unique: $duplicateTitles",
            duplicateTitles.isEmpty(),
        )

        val duplicateShardIds = VexJourneyCatalog.shards
            .groupBy { it.shardId }
            .filterValues { it.size > 1 }
            .keys
        assertTrue(
            "journey shard IDs must be unique: $duplicateShardIds",
            duplicateShardIds.isEmpty(),
        )

        // Malformed journey grammar is test-infrastructure failure, not coverage debt.
        VexJourneyCatalog.all.forEach { journey ->
            journey.walk(VexExpectedWalk())
        }

        val report = buildJourneyCoverageReport()
        writeReport(report)

        if (report.hasDebt) {
            System.err.println(
                buildString {
                    appendLine("WARNING: Vex semantic journey coverage debt detected.")
                    appendLine("Ordinary development remains buildable; debt is not silenced.")
                    appendLine(report.render())
                },
            )

            if (strictCoverageRequested()) {
                fail(
                    "Strict Vex journey coverage requested.\n\n" +
                        report.render(),
                )
            }
        } else {
            println(report.render())
        }
    }
}

private fun strictCoverageRequested(): Boolean =
    System.getProperty("vex.journey.strictCoverage")
        ?.toBooleanStrictOrNull()
        ?: false

private fun writeReport(report: VexJourneyCoverageReport) {
    val target = System.getProperty("vex.journey.coverageReport")
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?: return

    target.parentFile?.mkdirs()
    target.writeText(report.render())
}
