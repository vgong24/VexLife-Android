package com.vextreme.vexlife.android.cgm

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
        CgmStore(),
    val family:
        CgmFamily =
        CgmFamily(),
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
        family.observeStore(
            store.current,
        )
    }

    /**
     * Simulate one transport packet containing two independent Partner statements.
     *
     * The button that calls this method is a Lab control over the synthetic source. It deliberately
     * does not enter CgmFamily.Control because receiving outside truth is not a Family-authored
     * product Effect.
     */
    fun receiveSyntheticPacket():
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

        val observations =
            connection.receive(
                bytes =
                    packet,
                observedAtEpochMillis =
                    nowEpochMillis(),
            )

        /*
         * Keep the first patient zero painfully explicit:
         *
         * Partner receipt commits Store truth first.
         * Family then interprets the Store snapshot.
         *
         * A Room-backed realization may later drive this from an observable query/Flow, but hiding
         * the ordering behind another abstraction here would make the relationship harder to inspect.
         */
        family.observeStore(
            store.current,
        )

        return observations
    }
}

// [VXG RealForever][VEX-CGM-LABS][PARTNER-CIRCUIT-PATIENT-ZERO]
