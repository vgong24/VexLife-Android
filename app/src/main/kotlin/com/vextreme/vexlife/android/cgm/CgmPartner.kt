package com.vextreme.vexlife.android.cgm

import com.vextreme.vexlife.android.behavior.VexPartner

/**
 * Patient-zero external CGM participant.
 *
 * One Partner vocabulary is intentionally realized through materially different provider
 * representations. BLE-like bytes and an HTTP-like response body both converge into the exact same
 * CgmPartner.Signal vocabulary before Observation / Receiver / Store / Family.
 */
internal object CgmPartner :
    VexPartner<CgmPartner> {

    /**
     * Origin coordinate of one CGM acquisition history.
     *
     * Source belongs to the Partner vocabulary, not to BLE/HTTP. Changing transport must not change
     * which acquisition history a Signal belongs to.
     */
    data class Source(
        val deviceRef: String,
        val acquisitionEpoch: String,
    ) :
        VexPartner.Source<CgmPartner>

    enum class Trend {
        Falling,
        Steady,
        Rising,
    }

    sealed interface Signal :
        VexPartner.Signal<CgmPartner> {

        data class Glucose(
            val source: Source,
            val sequence: Long,
            val capturedAtEpochMillis: Long,
            val mgDl: Int,
            val reportedTrend: Trend,
        ) :
            Signal

        data class Temperature(
            val source: Source,
            val sequence: Long,
            val capturedAtEpochMillis: Long,
            val celsiusHundredths: Int,
        ) :
            Signal
    }

    /**
     * One live realization of the CGM Partner relationship.
     *
     * [Input] is deliberately NOT part of the Partner vocabulary. It belongs only to the concrete
     * provider realization. The Connection receives any Input for which a typed [Conversion] exists,
     * then runs the same Observation -> Receiver pipeline.
     */
    internal class Connection<
        Input,
    >(
        override val id: Id,
        private val conversion:
            VexPartner.Connection.Conversion<
                Input,
                CgmPartner,
                Signal
            >,
        private val receiver: Receiver,
    ) :
        VexPartner.Connection<
            CgmPartner,
            Signal
        > {

        data class Id(
            val value: String,
        ) :
            VexPartner.Connection.Id<CgmPartner>

        /** Synthetic HTTP delivery shape used only to pressure a second provider representation. */
        data class HttpResponse(
            val body: String,
        )

        /**
         * BLE-like compact packet decoder.
         *
         * Grammar:
         *     g|device|epoch|sequence|capturedAtMs|mgDl|trend
         *     t|device|epoch|sequence|capturedAtMs|celsiusHundredths
         *
         * ';' separates provider records. One transport delivery may yield several semantic Signals.
         */
        class BleDecoder :
            VexPartner.Connection.Conversion<
                ByteArray,
                CgmPartner,
                Signal
            > {
            override fun convert(
                input: ByteArray,
            ): List<Signal> =
                input
                    .toString(
                        Charsets.UTF_8,
                    )
                    .split(';')
                    .mapNotNull(
                        ::decodeBleRecord,
                    )
        }

        /**
         * HTTP-like response decoder.
         *
         * The synthetic body intentionally uses a different representation from BLE:
         *
         *     glucose?device=...&epoch=...&sequence=...&capturedAt=...&mgDl=...&trend=...
         *     temperature?device=...&epoch=...&sequence=...&capturedAt=...&celsiusHundredths=...
         *
         * The transport representation differs; the resulting Partner Signals do not.
         */
        class HttpDecoder :
            VexPartner.Connection.Conversion<
                HttpResponse,
                CgmPartner,
                Signal
            > {
            override fun convert(
                input: HttpResponse,
            ): List<Signal> =
                input
                    .body
                    .lineSequence()
                    .map(
                        String::trim,
                    )
                    .filter(
                        String::isNotEmpty,
                    )
                    .mapNotNull(
                        ::decodeHttpLine,
                    )
                    .toList()
        }

        data class Observation(
            override val connectionId: Id,
            override val signal: Signal,
            override val observedAtEpochMillis: Long,
        ) :
            VexPartner.Connection.Observation<
                CgmPartner,
                Signal
            >

        /**
         * Connection-side handoff into CGM durable admission.
         *
         * Receiver is identical for BLE/HTTP because transport representation has already been
         * converted into the Partner Signal vocabulary.
         */
        class Receiver(
            private val store: CgmStore,
        ) :
            VexPartner.Connection.Receiver<
                CgmPartner,
                Signal,
                Observation
            > {
            override suspend fun receive(
                observation:
                    Observation,
            ) {
                store.commit(
                    observation,
                )
            }
        }

        /**
         * Shared provider-independent traversal:
         *
         *     Input
         *     -> Conversion
         *     -> Signal(s)
         *     -> Observation(s)
         *     -> Receiver
         *
         * BLE and HTTP differ only before Signal.
         */
        suspend fun receive(
            input: Input,
            observedAtEpochMillis: Long,
        ):
            List<Observation> =
            conversion
                .convert(
                    input,
                )
                .map { signal ->
                    Observation(
                        connectionId =
                            id,
                        signal =
                            signal,
                        observedAtEpochMillis =
                            observedAtEpochMillis,
                    ).also { observation ->
                        receiver.receive(
                            observation,
                        )
                    }
                }
    }

    private fun decodeBleRecord(
        record: String,
    ):
        Signal? {
        val parts =
            record
                .trim()
                .split('|')

        return when (
            parts.firstOrNull()
        ) {
            "g" ->
                parts
                    .takeIf {
                        it.size ==
                            7
                    }
                    ?.let {
                        glucose(
                            deviceRef =
                                it[1],
                            acquisitionEpoch =
                                it[2],
                            sequence =
                                it[3].toLong(),
                            capturedAtEpochMillis =
                                it[4].toLong(),
                            mgDl =
                                it[5].toInt(),
                            trend =
                                it[6],
                        )
                    }

            "t" ->
                parts
                    .takeIf {
                        it.size ==
                            6
                    }
                    ?.let {
                        temperature(
                            deviceRef =
                                it[1],
                            acquisitionEpoch =
                                it[2],
                            sequence =
                                it[3].toLong(),
                            capturedAtEpochMillis =
                                it[4].toLong(),
                            celsiusHundredths =
                                it[5].toInt(),
                        )
                    }

            else ->
                null
        }
    }

    private fun decodeHttpLine(
        line: String,
    ):
        Signal? {
        val kind =
            line
                .substringBefore(
                    '?',
                    missingDelimiterValue =
                        "",
                )
        val query =
            line
                .substringAfter(
                    '?',
                    missingDelimiterValue =
                        "",
                )
                .split('&')
                .mapNotNull { pair ->
                    val key =
                        pair.substringBefore(
                            '=',
                            missingDelimiterValue =
                                "",
                        )
                    val value =
                        pair.substringAfter(
                            '=',
                            missingDelimiterValue =
                                "",
                        )

                    if (
                        key.isBlank()
                    ) {
                        null
                    } else {
                        key to
                            value
                    }
                }
                .toMap()

        val device =
            query["device"]
                ?: return null
        val epoch =
            query["epoch"]
                ?: return null
        val sequence =
            query["sequence"]
                ?.toLongOrNull()
                ?: return null
        val capturedAt =
            query["capturedAt"]
                ?.toLongOrNull()
                ?: return null

        return when (
            kind
        ) {
            "glucose" ->
                glucose(
                    deviceRef =
                        device,
                    acquisitionEpoch =
                        epoch,
                    sequence =
                        sequence,
                    capturedAtEpochMillis =
                        capturedAt,
                    mgDl =
                        query["mgDl"]
                            ?.toIntOrNull()
                            ?: return null,
                    trend =
                        query["trend"]
                            ?: return null,
                )

            "temperature" ->
                temperature(
                    deviceRef =
                        device,
                    acquisitionEpoch =
                        epoch,
                    sequence =
                        sequence,
                    capturedAtEpochMillis =
                        capturedAt,
                    celsiusHundredths =
                        query["celsiusHundredths"]
                            ?.toIntOrNull()
                            ?: return null,
                )

            else ->
                null
        }
    }

    private fun glucose(
        deviceRef: String,
        acquisitionEpoch: String,
        sequence: Long,
        capturedAtEpochMillis: Long,
        mgDl: Int,
        trend: String,
    ):
        Signal.Glucose? =
        Signal.Glucose(
            source =
                Source(
                    deviceRef =
                        deviceRef,
                    acquisitionEpoch =
                        acquisitionEpoch,
                ),
            sequence =
                sequence,
            capturedAtEpochMillis =
                capturedAtEpochMillis,
            mgDl =
                mgDl,
            reportedTrend =
                when (
                    trend.lowercase()
                ) {
                    "falling" ->
                        Trend.Falling

                    "steady" ->
                        Trend.Steady

                    "rising" ->
                        Trend.Rising

                    else ->
                        return null
                },
        )

    private fun temperature(
        deviceRef: String,
        acquisitionEpoch: String,
        sequence: Long,
        capturedAtEpochMillis: Long,
        celsiusHundredths: Int,
    ):
        Signal.Temperature =
        Signal.Temperature(
            source =
                Source(
                    deviceRef =
                        deviceRef,
                    acquisitionEpoch =
                        acquisitionEpoch,
                ),
            sequence =
                sequence,
            capturedAtEpochMillis =
                capturedAtEpochMillis,
            celsiusHundredths =
                celsiusHundredths,
        )
}

// [VXG RealForever][VEX-CGM-LABS][PARTNER-PATIENT-ZERO]
