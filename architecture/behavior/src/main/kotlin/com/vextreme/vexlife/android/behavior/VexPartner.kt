package com.vextreme.vexlife.android.behavior

/**
 * Compile-time coordinate for one independently-owned participant at the other end of a Vex
 * relationship.
 *
 * WHAT THIS IS
 * Partner does not own Family semantics and is not permanently a Source or Target. Those are
 * directional roles. A Partner may report outside truth toward the application and, when the
 * relationship earns it, receive an attempted Family Effect in the opposite direction.
 *
 * WHY THIS ROOT EXISTS
 * Connection-side concepts were otherwise easy to leave as package peers whose relationship had to
 * be reconstructed from names such as "CgmSignal", "BleDecoder", and "DeviceObservation". The
 * recursive Partner coordinate lets a concrete domain keep that relationship source-readable:
 *
 *     <Domain>Partner
 *     -> Signal
 *     -> Connection
 *        -> Id
 *        -> Decoder
 *        -> Observation
 *        -> Receiver
 *
 * This is deliberately smaller than a mirrored Family. It does not define State, Control, Effect,
 * persistence, repositories, or a generic "Partnership" object. Repeated consumers must earn any
 * additional child.
 */
interface VexPartner<
    Partner,
> where
    Partner : VexPartner<Partner> {

    /** What one exact Partner can communicate in its own domain vocabulary. */
    interface Signal<
        Partner,
    > where
        Partner : VexPartner<Partner>

    /**
     * Live communication relationship for one exact Partner + Signal vocabulary.
     *
     * The contract does not prescribe BLE, HTTP, Room, Media3, or another provider technology.
     * Concrete realizations keep those mechanics local while preserving the Partner coordinate.
     */
    interface Connection<
        Partner,
        PartnerSignal,
    > where
        Partner : VexPartner<Partner>,
        PartnerSignal : Signal<Partner> {

        /** Exact typed identity of this live Connection relationship. */
        val id:
            Id<Partner>

        /** Typed identity for the concrete Connection occurrence/source of an Observation. */
        interface Id<
            Partner,
        > where
            Partner : VexPartner<Partner>

        /**
         * Provider representation -> zero or more Partner Signals.
         *
         * Zero-to-many is intentional: one transport unit may contain several independent semantic
         * statements. Meaningful collection Signals remain one Signal when the collection itself
         * carries domain meaning; Decoder must not flatten that relationship merely because it can.
         */
        fun interface Decoder<
            Input,
            Partner,
            PartnerSignal,
        > where
            Partner : VexPartner<Partner>,
            PartnerSignal : Signal<Partner> {
            fun decode(
                input: Input,
            ): List<PartnerSignal>
        }

        /**
         * One encounter of one Partner Signal through one exact Connection.
         *
         * Signal identity and Connection occurrence remain distinct so the same source fact may be
         * encountered live, during recovery, or through another realization without becoming a
         * different domain statement.
         */
        interface Observation<
            Partner,
            PartnerSignal,
        > where
            Partner : VexPartner<Partner>,
            PartnerSignal : Signal<Partner> {
            val connectionId:
                Id<Partner>
            val signal:
                PartnerSignal
            val observedAtEpochMillis:
                Long
        }

        /**
         * Inbound Connection-side handoff into the consumer's admission boundary.
         *
         * The handoff is suspending because a truthful consumer may need to await transactional
         * persistence before it can say the Observation was durably admitted. Receiver intentionally
         * owns no Store or Family contract here; a concrete Partner binds it to the exact
         * Store/reconciliation boundary that consumer earns.
         */
        fun interface Receiver<
            Partner,
            PartnerSignal,
        > where
            Partner : VexPartner<Partner>,
            PartnerSignal : Signal<Partner> {
            suspend fun receive(
                observation:
                    Observation<
                        Partner,
                        PartnerSignal
                    >,
            )
        }
    }
}

// [VXG RealForever][VEX-PARTNER][PATIENT-ZERO]
