@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.cgm

import android.content.Context
import com.vextreme.vexlife.android.data.cgm.CgmStoreFactory

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
    private object InboundAdmission :
        VexExecution.RelationshipId

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

    private var nextSequence:
        Long? =
        null

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
        VexExecution.beginQueued(
            executionRoot =
                executionRoot,
            owner =
                connection,
            relationshipId =
                InboundAdmission,
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
            nextSyntheticSequence()
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

    private suspend fun nextSyntheticSequence():
        Long {
        val current =
            nextSequence

        if (
            current !=
            null
        ) {
            nextSequence =
                current +
                    1L
            return current
        }

        /*
         * Restart does not create a new acquisition history. Resume the synthetic source sequence
         * from durable Store truth for this exact source/epoch.
         *
         * This lookup happens inside the queued inbound Connection relationship, so two rapid
         * synthetic packets cannot race the initialization.
         */
        val restoredMax =
            store
                .readCurrent()
                .observations
                .asSequence()
                .map {
                    it.signal
                }
                .filter { signal ->
                    when (
                        signal
                    ) {
                        is CgmPartner.Signal.Glucose ->
                            signal.source ==
                                CgmPartner.Source(
                                    deviceRef =
                                        "cgm-synthetic",
                                    acquisitionEpoch =
                                        "epoch-a",
                                )

                        is CgmPartner.Signal.Temperature ->
                            signal.source ==
                                CgmPartner.Source(
                                    deviceRef =
                                        "cgm-synthetic",
                                    acquisitionEpoch =
                                        "epoch-a",
                                )
                    }
                }
                .map { signal ->
                    when (
                        signal
                    ) {
                        is CgmPartner.Signal.Glucose ->
                            signal.sequence

                        is CgmPartner.Signal.Temperature ->
                            signal.sequence
                    }
                }
                .maxOrNull()
                ?: 0L

        val next =
            restoredMax +
                1L

        nextSequence =
            next +
                1L

        return next
    }

    fun close() {
        executionRoot.forceStopAll(
            "CGM Partner Lab disposed",
        )
    }

    companion object {
        fun production(
            context:
                Context,
        ):
            CgmPartnerLab =
            CgmPartnerLab(
                store =
                    CgmStoreFactory.production(
                        context,
                    ),
            )
    }
}

// [VXG RealForever][VEX-CGM-LABS][PARTNER-CIRCUIT-PATIENT-ZERO]
