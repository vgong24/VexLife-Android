package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.UIElement

/**
 * One current VexStream track-presentation neighborhood.
 *
 * The same domain track can be rendered in more than one Compose surface at once (for example the
 * underlying Songs pane while Search is open). QA identity therefore includes presentation scope
 * as well as domain identity; trackRef alone is not a unique rendered-node address.
 */
@JvmInline
internal value class VexStreamTrackPresentationScope private constructor(
    val value: String,
) {
    companion object {
        val Songs =
            VexStreamTrackPresentationScope("songs")
        val Downloads =
            VexStreamTrackPresentationScope("downloads")
        val Search =
            VexStreamTrackPresentationScope("search")

        fun collection(
            collectionKey: String,
        ): VexStreamTrackPresentationScope {
            require(collectionKey.isNotBlank()) {
                "collection presentation key must not be blank"
            }
            return VexStreamTrackPresentationScope(
                "collection:$collectionKey",
            )
        }
    }
}

/**
 * Stable runtime presentation addresses for dynamic VexStream data.
 *
 * Track/catalog rows cannot be permanent members of the static Screen registry because the Hub
 * owns which tracks exist. Their QA/inspection identity is therefore derived from the stable domain
 * ref plus the exact current presentation scope, never title, translated text, visual position, or
 * current sort order.
 *
 * Runtime instance coordinates are encoded as lowercase UTF-8 hex so opaque domain/presentation
 * characters cannot become ambiguous Compose/evidence path delimiters. Domain identity itself is
 * not rewritten.
 */
object VexStreamPresentationRefs {
    private const val TrackRowPrefix =
        "element.vexstream-music.track"

    const val SearchResultsList =
        "element.vexstream-music.search.results"

    const val QueueList =
        "element.vexstream-music.queue.entries"

    fun trackRow(
        scope: VexStreamTrackPresentationScope,
        trackRef: VexStreamTrackRef,
    ): String =
        dynamicRef(
            ownerRef = TrackRowPrefix,
            instanceRef =
                trackInstance(
                    scope = scope,
                    trackRef = trackRef,
                ),
        )

    fun trackRowAction(
        scope: VexStreamTrackPresentationScope,
        trackRef: VexStreamTrackRef,
        action: ActionElement,
    ): String =
        trackAction(
            scope = scope,
            trackRef = trackRef,
            action = action,
            placement = "row",
        )

    fun trackMenuAction(
        scope: VexStreamTrackPresentationScope,
        trackRef: VexStreamTrackRef,
        action: ActionElement,
    ): String =
        trackAction(
            scope = scope,
            trackRef = trackRef,
            action = action,
            placement = "menu",
        )

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
        scope: VexStreamTrackPresentationScope,
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
            instanceRef =
                trackInstance(
                    scope = scope,
                    trackRef = trackRef,
                ),
        )
    }

    private fun trackInstance(
        scope: VexStreamTrackPresentationScope,
        trackRef: VexStreamTrackRef,
    ): String =
        scope.value +
            "\u0000" +
            trackRef.value

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
