package com.vextreme.vexlife.android.vexstream

/**
 * # VexStream Music
 *
 * Feature home:
 *
 * app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/README.md
 *
 * Search this symbol when asking:
 *
 * "How does the VexStream feature fit together on Android?"
 *
 * This is a navigation/documentation root only. It owns no repository state, playback state,
 * connection lifetime, transport, authorization, Compose state, or media bytes.
 *
 * ## Current truth owners
 *
 * Catalog / connection / download / offline truth:
 * com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
 *
 * Android Player / MediaSession lifetime:
 * com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
 *
 * Outside Player relationship:
 * com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
 * -> Media3PlayerConnection observes Service Player truth
 * -> VexStreamPlayerStore retains the current outside-Player snapshot
 * -> Media3ControllerConnection realizes bounded finite requests
 *
 * Finite semantic owners:
 * - com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
 * - com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
 * - com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatFamily
 *
 * Their relationship is:
 *
 * outside Player Signal
 * -> Player Store
 * -> Family observation
 * -> CURRENT semantic State
 * -> Control
 * -> accepted Effect / Approach
 * -> exact Player request
 * -> later outside Player truth
 *
 * Track / Collection playback, Seek, Queue-row Play and Reorder remain their existing typed payload
 * workflows. They are not absorbed into the finite Player Partner merely because Media3 realizes
 * them too.
 *
 * Offline copy + collection "Download missing" semantics:
 * com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineDocs
 *
 * Screen identity / durable interaction surfaces:
 * com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
 *
 * ## Convergence
 *
 * Do not cross-multiply independent truths into a giant VexStream State. A presentation such as the
 * App Widget may project the exact Player Store / Family-owned meanings it needs without becoming a
 * semantic owner. Service-owned Widget publication remains tied to PlaybackService lifetime.
 *
 * ## Proof direction
 *
 * Test each claim at its owner:
 * - Direct Family tests for finite semantic meaning/currentness;
 * - Player Store tests for outside-truth connection occurrence and retained-currentness;
 * - repository/media-store tests for catalog/offline integrity;
 * - presentation/render tests for UI projection;
 * - Journey / connected-device proof for actual Media3 and Android interaction.
 *
 * The terminal VexStream outcome remains the lived listening experience, not "architecture migrated."
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

// [VXG RealForever][VEXSTREAM][CURRENT-SOURCE-MAP]
