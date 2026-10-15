package com.vextreme.vexlife.android.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Current semantic presentation parent for custom Vex Compose realization.
 *
 * The static Screen Blueprint DSL already carries parent paths while declarations are formed.
 * Custom Compose needs the same law at render time so nested presentation cannot silently fall back
 * to isolated IDs.
 */
private val LocalVexComposeParentRef =
    staticCompositionLocalOf<String?> {
        null
    }

/**
 * Enters one registered Vex element as the parent of nested custom Compose presentation.
 */
@Composable
fun VexComposeScope(
    parent: UIElement,
    content: @Composable () -> Unit,
) {
    VexComposeScope(
        parentRef = parent.elementId,
        content = content,
    )
}

/**
 * Enters one already-formed runtime semantic/presentation ref as the parent of nested custom
 * Compose presentation.
 */
@Composable
fun VexComposeScope(
    parentRef: String,
    content: @Composable () -> Unit,
) {
    require(parentRef.isNotBlank()) {
        "Vex Compose parent ref must not be blank"
    }

    CompositionLocalProvider(
        LocalVexComposeParentRef provides parentRef,
        content = content,
    )
}

/**
 * Resolves one nested local ID beneath the current Vex Compose parent.
 *
 * Failing closed here is intentional: a Vex primitive rendered outside a Vex parent would otherwise
 * recreate the exact continuity gap this foundation exists to prevent.
 */
@Composable
internal fun vexComposeChildRef(
    localId: String,
): String {
    val parent =
        requireNotNull(
            LocalVexComposeParentRef.current,
        ) {
            "Vex Compose primitive requires an enclosing VexComposeScope or Vex container"
        }

    return appendVexElementPath(
        parentRef = parent,
        childPath = localId,
    )
}

/**
 * Re-enters a newly formed child as the parent for its nested content.
 */
@Composable
internal fun VexComposeChildScope(
    childRef: String,
    content: @Composable () -> Unit,
) {
    VexComposeScope(
        parentRef = childRef,
        content = content,
    )
}

// [VXG RealForever]
