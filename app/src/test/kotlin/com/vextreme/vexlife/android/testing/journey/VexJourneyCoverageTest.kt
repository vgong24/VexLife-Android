package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VexJourneyCoverageTest {
    @Test
    fun dynamicActionTemplateStillCountsTowardStaticSemanticCoverage() {
        val recorder =
            VexJourneyCoverageRecorder()

        recorder.start(
            HomeScreen,
        )
        recorder.tapDynamic(
            dynamicElement(
                elementId =
                    "element.vexstream-music.dynamic.play.example",
                actionTemplate =
                    VexStreamMusicScreen.PlayTrack,
            ),
        )

        assertTrue(
            VexStreamMusicScreen.PlayTrack.elementId in
                recorder.snapshot().actionElementIds,
        )
    }

    @Test
    fun reportSemanticCoverageDebtWithoutBlockingOrdinaryDevelopment() {
        val duplicateShardIds = VexJourneyCatalog.shards
            .groupBy { it.shardId }
            .filterValues { it.size > 1 }
            .keys
        assertTrue(
            "journey shard IDs must be unique: $duplicateShardIds",
            duplicateShardIds.isEmpty(),
        )

        val duplicateJourneyIds = VexJourneyCatalog.shards
            .flatMap { shard ->
                shard.journeys
                    .groupBy { it.id }
                    .filterValues { it.size > 1 }
                    .keys
                    .map { journeyId -> "${shard.shardId}/$journeyId" }
            }
        assertTrue(
            "journey IDs must be unique inside their shard: $duplicateJourneyIds",
            duplicateJourneyIds.isEmpty(),
        )

        val duplicateCaseRefs = VexJourneyCatalog.cases
            .groupBy { it.caseRef }
            .filterValues { it.size > 1 }
            .keys
        assertTrue(
            "journey case refs must be globally unique: $duplicateCaseRefs",
            duplicateCaseRefs.isEmpty(),
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
