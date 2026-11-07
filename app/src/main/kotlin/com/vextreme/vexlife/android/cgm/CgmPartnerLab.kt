@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.cgm

import android.content.Context
import com.vextreme.vexlife.android.data.cgm.CgmStoreFactory
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.Job

/**
 * Lab composition for one CGM Partner vocabulary realized through two provider representations.
 *
 * Both paths intentionally converge before Observation:
 *
 *     BLE-like ByteArray -> BleDecoder ----\
 *                                       +-> CgmPartner.Signal(s)
 *     HTTP-like body    -> HttpDecoder ---/
 *                                             ↓
 *                                      Connection.Observation
 *                                             ↓
 *                                      Connection.Receiver
 *                                             ↓
 *                                          CgmStore
 *                                             ↓
 *                                          CgmFamily
 *
 * This is not a generic Partnership owner. It is a pressure fixture proving what repeats.
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

    private data class SyntheticSample(
        val sequence: Long,
        val capturedAtEpochMillis: Long,
        val mgDl: Int,
        val trend: String,
        val celsiusHundredths: Int,
    )

    private val receiver =
        CgmPartner.Connection.Receiver(
            store =
                store,
        )

    private val bleConnection =
        CgmPartner.Connection(
            id =
                CgmPartner.Connection.Id(
                    value =
                        "cgm.synthetic.ble",
                ),
            conversion =
                CgmPartner.Connection.BleDecoder(),
            receiver =
                receiver,
        )

    private val httpConnection =
        CgmPartner.Connection(
            id =
                CgmPartner.Connection.Id(
                    value =
                        "cgm.synthetic.http",
                ),
            conversion =
                CgmPartner.Connection.HttpDecoder(),
            receiver =
                receiver,
        )

    private var nextSequence:
        Long? =
        null

    init {
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

    /** Existing/default synthetic ingress remains the BLE-like provider path. */
    fun receiveSyntheticPacket():
        Job =
        receiveSyntheticBlePacket()

    fun receiveSyntheticBlePacket():
        Job =
        VexExecution.beginQueued(
            executionRoot =
                executionRoot,
            owner =
                bleConnection,
            relationshipId =
                InboundAdmission,
            operation =
                ::receiveSyntheticBlePacketAndAwait,
            argument =
                Unit,
        )

    fun receiveSyntheticHttpResponse():
        Job =
        VexExecution.beginQueued(
            executionRoot =
                executionRoot,
            owner =
                httpConnection,
            relationshipId =
                InboundAdmission,
            operation =
                ::receiveSyntheticHttpResponseAndAwait,
            argument =
                Unit,
        )

    internal suspend fun receiveSyntheticBlePacketAndAwait(
        @Suppress("UNUSED_PARAMETER")
        unit:
            Unit,
    ):
        List<
            CgmPartner.Connection.Observation
        > {
        val sample =
            nextSyntheticSample()

        return bleConnection.receive(
            input =
                blePacket(
                    sample,
                ),
            observedAtEpochMillis =
                nowEpochMillis(),
        )
    }

    internal suspend fun receiveSyntheticHttpResponseAndAwait(
        @Suppress("UNUSED_PARAMETER")
        unit:
            Unit,
    ):
        List<
            CgmPartner.Connection.Observation
        > {
        val sample =
            nextSyntheticSample()

        return httpConnection.receive(
            input =
                httpResponse(
                    sample,
                ),
            observedAtEpochMillis =
                nowEpochMillis(),
        )
    }

    /**
     * Equivalent-source conversion fixture used by focused tests.
     *
     * This does not admit either representation to Store. It proves provider representation may
     * change while Partner semantic vocabulary remains equal.
     */
    internal fun equivalentSyntheticConversions(
        sequence: Long,
        capturedAtEpochMillis: Long,
        mgDl: Int,
        trend: String,
        celsiusHundredths: Int,
    ):
        Pair<
            List<CgmPartner.Signal>,
            List<CgmPartner.Signal>
        > {
        val sample =
            SyntheticSample(
                sequence =
                    sequence,
                capturedAtEpochMillis =
                    capturedAtEpochMillis,
                mgDl =
                    mgDl,
                trend =
                    trend,
                celsiusHundredths =
                    celsiusHundredths,
            )

        val ble =
            CgmPartner.Connection.BleDecoder()
                .convert(
                    blePacket(
                        sample,
                    ),
                )
        val http =
            CgmPartner.Connection.HttpDecoder()
                .convert(
                    httpResponse(
                        sample,
                    ),
                )

        return ble to
            http
    }

    private suspend fun nextSyntheticSample():
        SyntheticSample {
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

        return SyntheticSample(
            sequence =
                sequence,
            capturedAtEpochMillis =
                capturedAt,
            mgDl =
                mgDl,
            trend =
                trend,
            celsiusHundredths =
                celsiusHundredths,
        )
    }

    private fun blePacket(
        sample:
            SyntheticSample,
    ):
        ByteArray =
        buildString {
            append(
                "g|cgm-synthetic|epoch-a|",
            )
            append(
                sample.sequence,
            )
            append('|')
            append(
                sample.capturedAtEpochMillis,
            )
            append('|')
            append(
                sample.mgDl,
            )
            append('|')
            append(
                sample.trend,
            )
            append(';')
            append(
                "t|cgm-synthetic|epoch-a|",
            )
            append(
                sample.sequence,
            )
            append('|')
            append(
                sample.capturedAtEpochMillis,
            )
            append('|')
            append(
                sample.celsiusHundredths,
            )
        }
            .toByteArray(
                Charsets.UTF_8,
            )

    private fun httpResponse(
        sample:
            SyntheticSample,
    ):
        CgmPartner.Connection.HttpResponse =
        CgmPartner.Connection.HttpResponse(
            body =
                buildString {
                    append(
                        "glucose?device=cgm-synthetic&epoch=epoch-a&sequence=",
                    )
                    append(
                        sample.sequence,
                    )
                    append(
                        "&capturedAt=",
                    )
                    append(
                        sample.capturedAtEpochMillis,
                    )
                    append(
                        "&mgDl=",
                    )
                    append(
                        sample.mgDl,
                    )
                    append(
                        "&trend=",
                    )
                    append(
                        sample.trend,
                    )
                    append(
                        '\n',
                    )
                    append(
                        "temperature?device=cgm-synthetic&epoch=epoch-a&sequence=",
                    )
                    append(
                        sample.sequence,
                    )
                    append(
                        "&capturedAt=",
                    )
                    append(
                        sample.capturedAtEpochMillis,
                    )
                    append(
                        "&celsiusHundredths=",
                    )
                    append(
                        sample.celsiusHundredths,
                    )
                },
        )

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

        val source =
            CgmPartner.Source(
                deviceRef =
                    "cgm-synthetic",
                acquisitionEpoch =
                    "epoch-a",
            )

        /*
         * Restart does not create a new acquisition history. Resume the synthetic Source sequence
         * from durable Store truth inside the queued live-Connection admission lane.
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
                                source

                        is CgmPartner.Signal.Temperature ->
                            signal.source ==
                                source
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

// [VXG RealForever][VEX-CGM-LABS][MULTI-REALIZATION-PARTNER-CIRCUIT]
