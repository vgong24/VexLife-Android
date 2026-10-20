package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCollectionTestScenarios
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCollectionTestScenarios.Outcome
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCollectionTestScenarios.Scenario
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Backward-compatible QuantumTest experiment over canonical MassDownload focused tests.
 *
 * Ordinary tests remain the feature proof owner. This file asks only whether the same feature-owned
 * scenarios can be executed more efficiently by sharing one reconstructable checkpoint while
 * preserving exact shard -> canonical-test traceability and result equivalence.
 *
 * Deleting this experiment must leave MassDownload proof coverage intact.
 */
class VexJourneyMassDownloadBranchReuseExperimentTest {
    @Test
    fun quantumCompositionMatchesCanonicalScenariosAndReportsWallClock() {
        val traditional =
            runTraditional()
        val quantum =
            runQuantum()

        assertEquals(
            Scenario.entries.toSet(),
            traditional.outcomes.keys,
        )
        assertEquals(
            traditional.outcomes,
            quantum.outcomes,
        )
        assertEquals(
            Scenario.entries.size,
            traditional.checkpointFormationCount,
        )
        assertEquals(
            1,
            quantum.checkpointFormationCount,
        )
        assertEquals(
            Scenario.entries
                .map(
                    Scenario::canonicalTestRef,
                )
                .toSet(),
            quantum.evidence
                .map(
                    ShardEvidence::canonicalTestRef,
                )
                .toSet(),
        )
        assertEquals(
            1,
            quantum.evidence
                .map(
                    ShardEvidence::checkpointRef,
                )
                .distinct()
                .size,
        )
        assertEquals(
            Scenario.entries.size,
            quantum.evidence
                .map { evidence ->
                    Triple(
                        evidence.checkpointRef,
                        evidence.shardRef,
                        evidence.routeRef,
                    )
                }
                .distinct()
                .size,
        )
        assertTrue(
            quantum.evidence.all { evidence ->
                evidence.status ==
                    ShardStatus.PASS
            },
        )

        /*
         * Timing is observation, not an acceptance threshold. This JVM fixture is intentionally
         * cheap and may show no benefit. A real Quantum candidate must demonstrate material
         * wall-clock savings on a genuinely expensive shared prefix before any foundation
         * graduation is considered.
         */
        println(
            buildString {
                append(BENCHMARK_PREFIX)
                append(" traditionalNanos=")
                append(traditional.elapsedNanos)
                append(" quantumNanos=")
                append(quantum.elapsedNanos)
                append(" traditionalCheckpointFormations=")
                append(traditional.checkpointFormationCount)
                append(" quantumCheckpointFormations=")
                append(quantum.checkpointFormationCount)
                append(" quantumRestoreCount=")
                append(quantum.restoreCount)
                append(" observedSpeedup=")
                append(
                    if (
                        quantum.elapsedNanos == 0L
                    ) {
                        "undefined"
                    } else {
                        traditional.elapsedNanos
                            .toDouble()
                            .div(
                                quantum.elapsedNanos
                                    .toDouble(),
                            )
                    },
                )
            },
        )
    }

    @Test
    fun independentShardFailureCanBeRetainedWithoutDiscardingSiblingEvidence() {
        val checkpoint =
            VexStreamOfflineCollectionTestScenarios
                .partialOfflineCheckpoint()
        val checkpointRef =
            checkpoint.checkpointRef()
        val evidence =
            runBlocking {
                supervisorScope {
                    Scenario.entries
                        .map { scenario ->
                            async(
                                Dispatchers.Default,
                            ) {
                                val scope =
                                    CoroutineScope(
                                        SupervisorJob() +
                                            Dispatchers.Default,
                                    )

                                try {
                                    if (
                                        scenario ==
                                        Scenario.StaleContent
                                    ) {
                                        error(
                                            SYNTHETIC_SHARD_FAILURE,
                                        )
                                    }

                                    val outcome =
                                        VexStreamOfflineCollectionTestScenarios
                                            .execute(
                                                scenario = scenario,
                                                checkpoint = checkpoint,
                                                world =
                                                    VexStreamOfflineCollectionTestScenarios
                                                        .restore(
                                                            checkpoint = checkpoint,
                                                            scope = scope,
                                                        ),
                                            )

                                    ShardEvidence.pass(
                                        checkpointRef = checkpointRef,
                                        scenario = scenario,
                                        outcome = outcome,
                                    )
                                } catch (
                                    failure:
                                        Throwable,
                                ) {
                                    ShardEvidence.fail(
                                        checkpointRef = checkpointRef,
                                        scenario = scenario,
                                        failure = failure,
                                    )
                                } finally {
                                    scope.cancel()
                                }
                            }
                        }
                        .awaitAll()
                }
            }

        val byScenario =
            evidence.associateBy(
                ShardEvidence::scenario,
            )

        assertEquals(
            ShardStatus.PASS,
            byScenario
                .getValue(
                    Scenario.CompleteMissing,
                )
                .status,
        )
        assertEquals(
            ShardStatus.FAIL,
            byScenario
                .getValue(
                    Scenario.StaleContent,
                )
                .status,
        )
        assertEquals(
            SYNTHETIC_SHARD_FAILURE,
            byScenario
                .getValue(
                    Scenario.StaleContent,
                )
                .failureMessage,
        )
        assertEquals(
            ShardStatus.PASS,
            byScenario
                .getValue(
                    Scenario.HubLoss,
                )
                .status,
        )
        assertEquals(
            Scenario.entries
                .map(
                    Scenario::canonicalTestRef,
                )
                .toSet(),
            evidence
                .map(
                    ShardEvidence::canonicalTestRef,
                )
                .toSet(),
        )
    }

    private fun runTraditional(): ModeResult {
        val outcomes =
            linkedMapOf<
                Scenario,
                Outcome
            >()
        var checkpointFormationCount =
            0

        val started =
            System.nanoTime()

        Scenario.entries.forEach { scenario ->
            checkpointFormationCount +=
                1
            val checkpoint =
                VexStreamOfflineCollectionTestScenarios
                    .partialOfflineCheckpoint()
            val scope =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.Default,
                )

            try {
                outcomes[scenario] =
                    runBlocking {
                        VexStreamOfflineCollectionTestScenarios
                            .execute(
                                scenario = scenario,
                                checkpoint = checkpoint,
                                world =
                                    VexStreamOfflineCollectionTestScenarios
                                        .restore(
                                            checkpoint = checkpoint,
                                            scope = scope,
                                        ),
                            )
                    }
            } finally {
                scope.cancel()
            }
        }

        return ModeResult(
            elapsedNanos =
                System.nanoTime() -
                    started,
            checkpointFormationCount =
                checkpointFormationCount,
            restoreCount =
                Scenario.entries.size,
            outcomes =
                outcomes.toMap(),
            evidence =
                emptyList(),
        )
    }

    private fun runQuantum(): ModeResult {
        val started =
            System.nanoTime()
        val checkpoint =
            VexStreamOfflineCollectionTestScenarios
                .partialOfflineCheckpoint()
        val checkpointRef =
            checkpoint.checkpointRef()
        val outcomes =
            ConcurrentHashMap<
                Scenario,
                Outcome
            >()

        val evidence =
            runBlocking {
                supervisorScope {
                    Scenario.entries
                        .map { scenario ->
                            async(
                                Dispatchers.Default,
                            ) {
                                val scope =
                                    CoroutineScope(
                                        SupervisorJob() +
                                            Dispatchers.Default,
                                    )

                                try {
                                    val outcome =
                                        VexStreamOfflineCollectionTestScenarios
                                            .execute(
                                                scenario = scenario,
                                                checkpoint = checkpoint,
                                                world =
                                                    VexStreamOfflineCollectionTestScenarios
                                                        .restore(
                                                            checkpoint = checkpoint,
                                                            scope = scope,
                                                        ),
                                            )
                                    outcomes[scenario] =
                                        outcome

                                    ShardEvidence.pass(
                                        checkpointRef = checkpointRef,
                                        scenario = scenario,
                                        outcome = outcome,
                                    )
                                } catch (
                                    failure:
                                        Throwable,
                                ) {
                                    ShardEvidence.fail(
                                        checkpointRef = checkpointRef,
                                        scenario = scenario,
                                        failure = failure,
                                    )
                                } finally {
                                    scope.cancel()
                                }
                            }
                        }
                        .awaitAll()
                }
            }

        return ModeResult(
            elapsedNanos =
                System.nanoTime() -
                    started,
            checkpointFormationCount =
                1,
            restoreCount =
                Scenario.entries.size,
            outcomes =
                outcomes.toMap(),
            evidence =
                evidence,
        )
    }

    private data class ModeResult(
        val elapsedNanos: Long,
        val checkpointFormationCount: Int,
        val restoreCount: Int,
        val outcomes:
            Map<
                Scenario,
                Outcome
            >,
        val evidence: List<ShardEvidence>,
    )

    private enum class ShardStatus {
        PASS,
        FAIL,
    }

    private data class ShardEvidence(
        val suiteRef: String,
        val checkpointRef: String,
        val shardRef: String,
        val routeRef: String,
        val canonicalTestRef: String,
        val scenario: Scenario,
        val status: ShardStatus,
        val outcome: Outcome?,
        val failureClass: String?,
        val failureMessage: String?,
    ) {
        companion object {
            fun pass(
                checkpointRef: String,
                scenario: Scenario,
                outcome: Outcome,
            ): ShardEvidence =
                ShardEvidence(
                    suiteRef = SUITE_REF,
                    checkpointRef = checkpointRef,
                    shardRef = scenario.scenarioRef,
                    routeRef = scenario.routeRef,
                    canonicalTestRef =
                        scenario.canonicalTestRef,
                    scenario = scenario,
                    status = ShardStatus.PASS,
                    outcome = outcome,
                    failureClass = null,
                    failureMessage = null,
                )

            fun fail(
                checkpointRef: String,
                scenario: Scenario,
                failure: Throwable,
            ): ShardEvidence =
                ShardEvidence(
                    suiteRef = SUITE_REF,
                    checkpointRef = checkpointRef,
                    shardRef = scenario.scenarioRef,
                    routeRef = scenario.routeRef,
                    canonicalTestRef =
                        scenario.canonicalTestRef,
                    scenario = scenario,
                    status = ShardStatus.FAIL,
                    outcome = null,
                    failureClass =
                        failure::class
                            .qualifiedName,
                    failureMessage =
                        failure.message,
                )
        }
    }

    private fun VexStreamOfflineCollectionTestScenarios.Checkpoint.checkpointRef():
        String {
        val canonical =
            buildString {
                fun field(
                    name: String,
                    value: Any?,
                ) {
                    val text =
                        value?.toString()
                            ?: "<null>"
                    append(name)
                    append(':')
                    append(text.length)
                    append(':')
                    append(text)
                    append('\n')
                }

                field(
                    "schema",
                    "vexstream.mass-download.quantum-checkpoint/v1",
                )

                when (
                    val hub =
                        state.hubState
                ) {
                    VexStreamHubState.NotConfigured ->
                        field(
                            "hubState",
                            "not-configured",
                        )

                    VexStreamHubState.Unreachable ->
                        field(
                            "hubState",
                            "unreachable",
                        )

                    VexStreamHubState.PairingRequired ->
                        field(
                            "hubState",
                            "pairing-required",
                        )

                    VexStreamHubState.AuthenticationRequired ->
                        field(
                            "hubState",
                            "authentication-required",
                        )

                    VexStreamHubState.AuthorizationRequired ->
                        field(
                            "hubState",
                            "authorization-required",
                        )

                    VexStreamHubState.Revoked ->
                        field(
                            "hubState",
                            "revoked",
                        )

                    is VexStreamHubState.Ready -> {
                        field(
                            "hubState",
                            "ready",
                        )
                        field(
                            "hubRef",
                            hub.hubRef,
                        )
                        field(
                            "catalogRevision",
                            hub.catalogRevision,
                        )
                    }
                }

                state.tracks
                    .sortedBy {
                        it.track.trackRef.value
                    }
                    .forEach { track ->
                        field(
                            "trackRef",
                            track.track.trackRef.value,
                        )
                        field(
                            "mediaSha256",
                            track.track.mediaSha256.value,
                        )
                        field(
                            "mediaByteLength",
                            track.track.mediaByteLength,
                        )
                        field(
                            "hubAvailable",
                            track.hubAvailable,
                        )
                        field(
                            "localCopy",
                            track.localCopy.checkpointValue(),
                        )
                    }
            }

        return "snapshot:vexstream-mass-download:" +
            MessageDigest
                .getInstance(
                    "SHA-256",
                )
                .digest(
                    canonical
                        .encodeToByteArray(),
                )
                .joinToString(
                    separator = "",
                ) { byte ->
                    "%02x".format(
                        byte,
                    )
                }
    }

    private fun VexStreamLocalCopyState.checkpointValue(): String =
        when (this) {
            VexStreamLocalCopyState.NotDownloaded ->
                "not-downloaded"

            is VexStreamLocalCopyState.Downloading ->
                "downloading:$bytesDownloaded/$totalBytes"

            is VexStreamLocalCopyState.Downloaded ->
                "downloaded:${mediaSha256.value}:$byteLength:$localMediaRef"

            is VexStreamLocalCopyState.Failed ->
                "failed:$reasonCode:$retryable"
        }

    private companion object {
        const val SUITE_REF =
            "quantum.vexstream.mass-download"
        const val BENCHMARK_PREFIX =
            "VEX-QUANTUM-BENCHMARK"
        const val SYNTHETIC_SHARD_FAILURE =
            "synthetic MassDownload shard failure"
    }
}

// [VXG RealForever]
