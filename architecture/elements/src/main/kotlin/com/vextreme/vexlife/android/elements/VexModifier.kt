package com.vextreme.vexlife.android.elements

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Vex-owned root for Compose modifier chains.
 *
 * Product presentation starts from VexModifier instead of the bare Compose Modifier companion so
 * Vex-wide modifier policy has one visible architectural entry point. VexModifier remains a real
 * Compose [Modifier], so the complete Compose modifier ecosystem continues to work normally:
 *
 * VexModifier
 *     .fillMaxWidth()
 *     .padding(...)
 *     .clickable(...)
 *
 * Semantic identity is attached through [vex]. Future shared inspection/accessibility/evidence
 * behavior can evolve at this boundary without creating a second modifier system.
 */
object VexModifier : Modifier by Modifier

/**
 * Attaches one statically registered Vex element identity to this Compose modifier chain.
 */
fun Modifier.vex(
    element: UIElement,
): Modifier =
    vex(
        element.elementId,
    )

/**
 * Attaches one already-formed Vex semantic/presentation identity to this Compose modifier chain.
 *
 * Feature code should normally obtain [semanticRef] from its rightful identity owner (registered
 * UIElement, feature PresentationRefs, or inherited Vex Compose parent scope) rather than forming
 * ad-hoc strings at the call site.
 */
fun Modifier.vex(
    semanticRef: String,
): Modifier {
    require(semanticRef.isNotBlank()) {
        "Vex semantic ref must not be blank"
    }

    return testTag(
        semanticRef,
    )
}

/**
 * Compatibility/convenience spelling for existing static Element cells.
 */
fun Modifier.vexElement(
    element: UIElement,
): Modifier =
    vex(
        element,
    )

// [VXG RealForever]
