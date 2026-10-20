package com.vextreme.vexlife.android.elements

/**
 * Canonical relative-path normalization shared by the static Screen Blueprint DSL and custom
 * Vex Compose presentation.
 *
 * Keeping one formatter prevents registered identity and rendered identity from drifting merely
 * because they were formed through different authoring surfaces.
 */
internal fun normalizeVexElementPath(
    value: String,
): String =
    value.trim()
        .replace('/', '.')
        .replace(Regex("\\s+"), "-")
        .trim('.')
        .also { normalized ->
            require(normalized.isNotBlank()) {
                "Vex element path must not be blank"
            }
        }

/**
 * Forms one nested semantic address beneath an already-established parent.
 */
internal fun appendVexElementPath(
    parentRef: String,
    childPath: String,
): String {
    require(parentRef.isNotBlank()) {
        "Vex parent ref must not be blank"
    }

    return parentRef +
        "." +
        normalizeVexElementPath(
            childPath,
        )
}

// [VXG RealForever]
