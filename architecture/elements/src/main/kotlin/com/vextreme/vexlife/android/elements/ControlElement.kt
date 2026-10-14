package com.vextreme.vexlife.android.elements

import androidx.annotation.StringRes
import com.vextreme.vexlife.android.vexarchitecture.VexControl

/**
 * Registered UI control that points directly at one VexArchitecture Control.
 *
 * The element lets a fresh developer jump from a Screen affordance to the feature realm that owns
 * its meaning. It does not authorize presentation to perform platform work.
 */
class VexControlButtonElement(
    elementId: String,
    @StringRes labelRes: Int,
    style: ElementStyleKey = VexElementStyles.PrimaryAction,
    icon: ElementIcon? = null,
    val control: VexControl,
) : ButtonElement(
    elementId = elementId,
    labelRes = labelRes,
    style = style,
    icon = icon,
    intent = ElementIntent.LocalEffect,
)

// [VXG RealForever]
