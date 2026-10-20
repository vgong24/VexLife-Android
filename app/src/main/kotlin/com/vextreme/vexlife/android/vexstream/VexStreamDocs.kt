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
 * ## Human outcome
 *
 * VexStream should let a listener:
 *
 * 1. find and pair with their VexStream desktop/Home producer;
 * 2. browse the known music library;
 * 3. play through Android Media3;
 * 4. deliberately download selected media or the currently missing part of a collection;
 * 5. understand which collection media is already on this phone;
 * 6. keep playing verified local media with no network;
 * 7. use an admitted private remote route when away from home without exposing the desktop
 *    administration surface.
 *
 * ## Current truth owners
 *
 * Catalog / connection / download / offline truth:
 * com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
 *
 * Playback lifetime + queue provider:
 * com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
 *
 * Playback semantic family:
 * com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackDocs
 *
 * Offline copy + collection "Download missing" semantics:
 * com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineDocs
 *
 * Screen identity / durable interaction surfaces:
 * com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
 *
 * ## Architecture direction
 *
 * The Vex Behavior grammar is applied when a behavioral question has independently meaningful
 * current truth:
 *
 * State -> Control resolves Act -> Act selects Effect -> provider realization -> observed State
 *
 * Playback is the first production-shaped family.
 *
 * Queue should become a sibling family only where queue position/order/shuffle/repeat semantics are
 * independently meaningful from Playback.
 *
 * Download / local-copy behavior should expose intentional request, progress, verification,
 * completion, removal, and failure without making incidental cache equal downloaded media.
 *
 * Connection / pairing should keep reachability, authentication/approval, remembered identity, and
 * current route distinct.
 *
 * Library/catalog objects remain repository data unless a repeated behavioral question earns another
 * family. Dynamic tracks must not become a static blueprint object per song.
 *
 * ## Convergence / Relay
 *
 * Do not cross-multiply independent truths into giant VexStream States.
 *
 * When a real consumer needs several current truths together, use the smallest rightful convergence
 * owner:
 *
 * playback projection -----\
 * queue projection ----------+-> derived UI/action availability
 * local media truth --------/
 *
 * A convergence owner does not become the transport, scheduler, persistence layer, or provider.
 *
 * ## Desktop companion
 *
 * The current desktop producer is VexStream Desktop V4 in vgong24/VexstreamMusic.
 *
 * Desktop owns the sanitized producer contract and exact media bytes. Android consumes that contract.
 * Android owns phone-local verification/storage and Media3 realization.
 *
 * Keep:
 *
 * CATALOG_READ != MEDIA_STREAM
 * MEDIA_STREAM != MEDIA_COPY
 * STREAMABLE != DOWNLOADED_LOCAL
 * REACHABILITY != AUTHENTICATION
 * PRIVATE_ROUTE != MUSIC_AUTHORITY
 * EFFECT_ATTEMPTED != PROVIDER_SUCCEEDED
 *
 * Remote/direct/relay experiments must remain transport adapters behind the music semantics until
 * their authority and lived route are accepted.
 *
 * ## Proof direction
 *
 * Test each transformation at its owner:
 *
 * - Behavior relationship tests for semantic meaning;
 * - repository/media-store tests for catalog/offline integrity;
 * - presentation/render tests for UI projection;
 * - Journey for the human path;
 * - ScreenCrawler for selected locale/font/viewport evidence;
 * - device/provider proof for Media3, storage, pairing, route and lifecycle claims.
 *
 * The terminal VexStream outcome is not "architecture migrated." It is a lived phone walk:
 *
 * pair once
 * -> browse real desktop library
 * -> stream and seek
 * -> download and verify
 * -> remove network / stop producer
 * -> local-only playback
 * -> restart VexLife
 * -> local copy restored
 * -> later leave Wi-Fi with mobile data
 * -> authorized remote stream works when a current private route exists
 * -> no-data playback still works for downloaded media
 *
 * No one proof surface substitutes for another.
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

// [VXG RealForever]
