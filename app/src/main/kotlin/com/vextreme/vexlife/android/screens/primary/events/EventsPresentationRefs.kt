package com.vextreme.vexlife.android.screens.primary.events

/**
 * Stable repeated-presentation identity derived from event identity, never visual list position.
 */
object EventsPresentationRefs {
    fun row(eventRef: String): String =
        "element.events.dynamic.$eventRef"

    fun attentionAction(
        eventRef: String,
        action: String,
    ): String =
        row(eventRef) + ".attention." + action

    fun tooltip(eventRef: String): String =
        "presentation.events.tooltip.$eventRef"
}

// [VXG RealForever]
