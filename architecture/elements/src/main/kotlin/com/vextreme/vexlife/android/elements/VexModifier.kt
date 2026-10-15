package com.vextreme.vexlife.android.elements

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Vex-owned semantic modifier description for Android Compose realization.
 *
 * Feature code names semantic identity; this boundary decides how that identity is projected into
 * Compose inspection/testing today. Keeping that projection here means a future addition such as
 * richer semantics, diagnostics, evidence metadata, or accessibility-oriented inspection can be
 * applied consistently without rediscovering the modifier pattern in every Screen/feature.
 *
 * [element] is for statically registered [UIElement] identity.
 * [semantic] is for stable runtime-data identity (for example one event row or one track action)
 * that must remain addressable without pretending dynamic product data belongs in the static
 * Screen registry.
 */
data class VexModifier private constructor(
    val testTag: String,
) {
    init {
        require(testTag.isNotBlank()) {
            "VexModifier testTag must not be blank"
        }
    }

    companion object {
        fun element(element: UIElement): VexModifier =
            VexModifier(
                testTag = element.elementId,
            )

        fun semantic(semanticRef: String): VexModifier =
            VexModifier(
                testTag = semanticRef,
            )
    }
}

/**
 * Applies one Vex semantic modifier projection to an ordinary Compose [Modifier].
 *
 * testTag is deliberately an implementation detail of this one boundary rather than a convention
 * every feature repeats directly.
 */
fun Modifier.vex(
    vexModifier: VexModifier,
): Modifier =
    testTag(
        vexModifier.testTag,
    )

/**
 * Compatibility/convenience path for statically registered Elements.
 *
 * Existing cells can keep saying .vexElement(element) while all Compose test-tag realization is
 * owned by [VexModifier].
 */
fun Modifier.vexElement(
    element: UIElement,
): Modifier =
    vex(
        VexModifier.element(
            element,
        ),
    )

// [VXG RealForever]
