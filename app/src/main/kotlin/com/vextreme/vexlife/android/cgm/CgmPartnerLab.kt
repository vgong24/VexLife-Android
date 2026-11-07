@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.cgm

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.Job

/**
 * Lab-only composition owner for the first complete Partner circuit.
 *
 * This is intentionally NOT a generic Partnership abstraction. It wires one concrete example so we
 * can inspect the repeated relationship before promoting more SDK nouns.
 *
 *     synthetic bytes
 *     -> CgmPartner.Connection.Decoder
 *     -> CgmPartner.Signal(s)
 *     -> Connection.Observation
 *     -> Connection.Receiver
 *     -> CgmStore
 *     -> CgmFamily
 *     -> DisplayState / custom Vex Compose
 */
internal class CgmPartnerLab(
    private val nowEpochMillis:
        () -> Long =
        System::currentTimeMillis,
    val store:
        CgmStore =
        InMemoryCgmStore(),
    val family:
        CgmFamily =
        CgmFamily(),
    private val executionRoot:
        VexExecutionRoot =
        VexExecutionRoot(),
) {
    private val receiver =
        CgmPartner.Connection.Receiver(
            store =
                store,
        )

    private val connection =
        CgmPartner.Connection(
            id =
                CgmPartner.Connection.Id(
                    value =
                        "cgm.synthetic.connection",
                ),
            decoder =
                CgmPartner.Connection.Decoder(),
            receiver =
                receiver,
        )

    private var nextSequence =
        1L

    init {
        /*
         * Sustained Store observation is source-addressed execution custody:
         *
         *     exact Flow source -> one maintained Job
         *
         * It intentionally does NOT mint a Family ResourceId/RelationshipId merely to keep a
         * listener alive.
         */
        VexExecution.observe(
            executionRoot =
                executionRoot,
            owner =
                family,
            source =
                store::snapshots,
        ) { snapshot ->
            family.observeStore(
                snapshot,
            )
        }
    }

    /**
     * Simulate one transport packet containing two independent Partner statements.
     *
     * The button that calls this method is a Lab control over the synthetic source. It deliberately
     * does not enter CgmFamily.Control because receiving outside truth is not a Family-authored
     * product Effect.
     */
    fun receiveSyntheticPacket():
        Job =
        VexExecution.begin(
            executionRoot =
                executionRoot,
            owner =
                this,
            operation =
                ::receiveSyntheticPacketAndAwait,
            argument =
                Unit,
        )

    /**
     * Awaitable form used by durability/restart proof.
     *
     * Returning from this function means every decoded Observation has completed Receiver -> Store
     * admission. It does not mean Family has already interpreted the Store Flow emission.
     */
    internal suspend fun receiveSyntheticPacketAndAwait(
        @Suppress("UNUSED_PARAMETER")
        unit:
            Unit,
    ):
        List<
            CgmPartner.Connection.Observation
        > {
        val sequence =
            nextSequence++
        val capturedAt =
            nowEpochMillis()
        val mgDl =
            100 +
                (
                    sequence *
                        3L
                )
                    .rem(
                        50L,
                    )
                    .toInt()
        val trend =
            when (
                sequence %
                    3L
            ) {
                0L ->
                    "falling"

                1L ->
                    "steady"

                else ->
                    "rising"
            }
        val celsiusHundredths =
            3_100 +
                sequence
                    .rem(
                        100L,
                    )
                    .toInt()

        val packet =
            buildString {
                append(
                    "g|cgm-synthetic|epoch-a|",
                )
                append(
                    sequence,
                )
                append('|')
                append(
                    capturedAt,
                )
                append('|')
                append(
                    mgDl,
                )
                append('|')
                append(
                    trend,
                )
                append(';')
                append(
                    "t|cgm-synthetic|epoch-a|",
                )
                append(
                    sequence,
                )
                append('|')
                append(
                    capturedAt,
                )
                append('|')
                append(
                    celsiusHundredths,
                )
            }
                .toByteArray(
                    Charsets.UTF_8,
                )

        return connection.receive(
            bytes =
                packet,
            observedAtEpochMillis =
                nowEpochMillis(),
        )
    }

    fun close() {
        executionRoot.forceStopAll(
            "CGM Partner Lab disposed",
        )
    }
}

// [VXG RealForever][VEX-CGM-LABS][PARTNER-CIRCUIT-PATIENT-ZERO]
