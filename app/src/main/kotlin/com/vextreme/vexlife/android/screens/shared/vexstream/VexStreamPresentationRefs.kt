package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.UIElement

/**
 * Stable runtime presentation addresses for dynamic VexStream data.
 *
 * Track/catalog rows cannot be permanent members of the static Screen registry because the Hub
 * owns which tracks exist. Their QA/inspection identity is therefore derived from the stable domain
 * ref, never title, translated text, visual position, or current sort order.
 *
 * VexStreamTrackRef is intentionally opaque. Runtime presentation refs encode the UTF-8 bytes as
 * lowercase hex so arbitrary domain characters cannot become ambiguous Compose/evidence path
 * delimiters. The domain identity itself remains unchanged.
 */
object VexStreamPresentationRefs {
    private const val TrackRowPrefix =
        "element.vexstream-music.track"

    fun trackRow(
        trackRef: VexStreamTrackRef,
    ): String =
        dynamicRef(
            ownerRef = TrackRowPrefix,
            instanceRef = trackRef.value,
        )

    fun trackRowAction(
        trackRef: VexStreamTrackRef,
        action: ActionElement,
    ): String =
        trackAction(
            trackRef = trackRef,
            action = action,
            placement = "row",
        )

    fun trackMenuAction(
        trackRef: VexStreamTrackRef,
        action: ActionElement,
    ): String =
        trackAction(
            trackRef = trackRef,
            action = action,
            placement = "menu",
        )

    const val SearchResultsList =
        "element.vexstream-music.search.results"

    const val QueueList =
        "element.vexstream-music.queue.entries"

    /*
     * Media3 currently projects queue media IDs, not a stronger durable queue-item identity.
     * occurrence therefore names one exact CURRENT presentation occurrence when the same media ID
     * appears more than once. It is deliberately not claimed stable across arbitrary queue rebuilds
     * or reorders. If the provider later exposes a durable item ref, that stronger identity should
     * replace this presentation coordinate.
     */
    fun queueEntryKey(
        mediaId: String,
        occurrence: Int,
    ): String {
        require(mediaId.isNotBlank()) {
            "queue mediaId must not be blank"
        }
        require(occurrence >= 1) {
            "queue occurrence must be >= 1"
        }

        return "$mediaId#$occurrence"
    }

    fun queueEntry(
        stableKey: String,
    ): String =
        dynamicRef(
            ownerRef =
                "element.vexstream-music.queue.entry",
            instanceRef = stableKey,
        )

    fun queueEntryAction(
        stableKey: String,
        action: ActionElement,
    ): String {
        val uiElement =
            action as? UIElement
                ?: error("ActionElement must also be a UIElement")

        return dynamicRef(
            ownerRef =
                uiElement.elementId +
                    ".queue-entry",
            instanceRef = stableKey,
        )
    }

    private fun trackAction(
        trackRef: VexStreamTrackRef,
        action: ActionElement,
        placement: String,
    ): String {
        val uiElement =
            action as? UIElement
                ?: error("ActionElement must also be a UIElement")

        return dynamicRef(
            ownerRef =
                uiElement.elementId +
                    "." +
                    placement,
            instanceRef = trackRef.value,
        )
    }

    private fun dynamicRef(
        ownerRef: String,
        instanceRef: String,
    ): String =
        "$ownerRef.dynamic.utf8hex." +
            instanceRef
                .encodeToByteArray()
                .joinToString(
                    separator = "",
                ) { byte ->
                    byte.toUByte()
                        .toString(16)
                        .padStart(
                            length = 2,
                            padChar = '0',
                        )
                }
}

// [VXG RealForever]
