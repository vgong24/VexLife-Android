package com.vextreme.vexlife.android.vexstream

/**
 * # VexStream Music
 *
 * VexStream's current architecture is relationship-first:
 *
 *     Music Experience
 *         -> semantic Families
 *         -> Player Partner
 *         -> one external music/playback Source
 *
 * Current semantic owners:
 *
 *     VexStreamPlaybackFamily
 *     VexStreamQueueFamily
 *     VexStreamRepeatFamily
 *
 * Current external relationship:
 *
 *     VexStreamPlayerPartner
 *       Source
 *       Signal
 *       Connection
 *
 * Player Store owns retained outside Snapshot truth assembled from Partner Signals. It is not a
 * Family and Partner does not own the retained Snapshot.
 *
 * Current Media3 realization exposes two live Connection occurrences to the same Source:
 *
 *     Media3PlayerConnection       observation
 *     Media3ControllerConnection   request / progress
 *
 * Media3 framework objects are realization details; they do not define semantic ownership.
 *
 * Direct Family consequences follow the accepted circuit:
 *
 *     CURRENT State + Control
 *       -> accepted Act
 *       -> bound Method
 *       -> VexExecution
 *       -> Player Partner request
 *
 * Provider outcome returns separately:
 *
 *     external Source
 *       -> Signal / Observation
 *       -> Store
 *       -> Family observation
 *
 * Track / Collection playback remain payload/source-resolution workflows because they have not
 * earned an independent semantic State/Control question merely by existing near the player. Their
 * execution uses canonical VexExecution overlap instead of feature-local scheduler mirrors.
 *
 * Queue-row Play/Reorder and Seek retain exact-current payload checks at the concrete Partner request
 * boundary where immediate provider-currentness results are required. Current foundations are not
 * used as a hammer where they do not preserve the lived contract.
 *
 * Widget is downstream presentation custody. The Playback Widget may use a Compound Relay when it
 * genuinely relates independent Player Store + Playback Family currents, but Widget does not become
 * another semantic Family/Partner root.
 *
 * Repository/catalog/offline/pairing concerns remain with their existing rightful data/repository
 * owners; they do not become Player semantic State merely because Music consumes them.
 *
 * Permanent distinctions:
 *
 *     PARTNER_SOURCE != TRANSPORT_REALIZATION
 *     CONNECTION_OCCURRENCE != SOURCE
 *     PARTNER_SIGNAL != STORE_SNAPSHOT
 *     STORE_SNAPSHOT != FAMILY_STATE
 *     EFFECT_REQUESTED != PROVIDER_OUTCOME
 *     SERVICE_LIFETIME != PLAYBACK_TRUTH
 *     WIDGET_SURFACE_STATE != PRODUCT_STATE
 *
 * The terminal VexStream outcome remains the lived phone experience: browse, stream, seek, queue,
 * download/verify, survive network loss/restart, and use authorized remote playback when available.
 * Architecture exists to make that experience stable; architecture migration is not itself the
 * terminal outcome.
 *
 * [VXG RealForever]
 */
@Suppress("unused")
internal object VexStreamDocs {
    const val FEATURE_REF: String =
        "feature.vexstream"

    const val CURRENT_FEATURE_NAME: String =
        "VexStream Music"

    val historicalSearchNames: List<String> =
        listOf(
            "VexStream Android Link",
            "VexStream V4 Companion",
        )
}

// [VXG RealForever][VEXSTREAM][CURRENT]
