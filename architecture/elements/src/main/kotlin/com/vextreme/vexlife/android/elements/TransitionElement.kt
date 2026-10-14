package com.vextreme.vexlife.android.elements

import androidx.annotation.StringRes
import com.vextreme.vexlife.android.vexarchitecture.VexTransitionSlot

/**
 * Registered UI control that points directly at one semantic VexArchitecture transition slot.
 *
 * The slot lets presentation ask:
 *
 *     "Which transition does this control represent from current meaning?"
 *
 * It does NOT authorize the UI to perform that transition. The element remains a normal registered
 * ButtonElement for registry/logging/journeys, while a feature-owned action/effect boundary must
 * receive the human intention and perform any platform/service command.
 *
 * LocalEffect here means the shared runtime does not own the feature effect; it does not mean the
 * Compose callback itself becomes the domain/platform owner.
 */
class TransitionButtonElement(
    elementId: String,
    @StringRes labelRes: Int,
    style: ElementStyleKey = VexElementStyles.PrimaryAction,
    icon: ElementIcon? = null,
    val transitionSlot: VexTransitionSlot,
) : ButtonElement(
    elementId = elementId,
    labelRes = labelRes,
    style = style,
    icon = icon,
    intent = ElementIntent.LocalEffect,
)

// [VXG RealForever]
