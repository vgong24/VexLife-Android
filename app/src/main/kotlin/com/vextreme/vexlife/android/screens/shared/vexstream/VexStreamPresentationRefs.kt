package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef

/**
 * Stable runtime presentation addresses for dynamic VexStream data.
 *
 * Track/catalog rows cannot be permanent members of the static Screen registry because the Hub
 * owns which tracks exist. Their QA/inspection identity is therefore derived from the stable domain
 * ref, never title, translated text, visual position, or current sort order.
 *
 * VexStreamTrackRef is intentionally opaque. Prefixing its exact value with its length keeps the
 * formed presentation ref unambiguous without imposing a new character vocabulary on the domain
 * identity.
 */
object VexStreamPresentationRefs {
    private const val TrackRowPrefix =
        "element.vexstream-music.dynamic.track"

    fun trackRow(
        trackRef: VexStreamTrackRef,
    ): String =
        "$TrackRowPrefix.${trackRef.value.length}:${trackRef.value}"
}

// [VXG RealForever]
