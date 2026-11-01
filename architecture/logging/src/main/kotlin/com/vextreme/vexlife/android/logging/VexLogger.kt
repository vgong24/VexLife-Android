package com.vextreme.vexlife.android.logging

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.elements.UIElement
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class VexLogKind {
    AppOpened,
    Interaction,
    DynamicInteraction,
    Gesture,
    Navigation,
    Semantic,
    Runtime,
    Diagnostic,
}

/**
 * Canonical readable diagnostic fact.
 *
 * [detailsJson] is optional structured detail captured only at an explicit semantic boundary such
 * as form Submit. Draft field edits are never emitted as records.
 */
data class VexLogRecord(
    val sequence: Long,
    val timestamp: Instant,
    val durationSincePreviousSeconds: Long?,
    val kind: VexLogKind,
    val subjectRef: String,
    val action: String,
    val screenBeforeId: String?,
    val screenAfterId: String?,
    val section: Long,
    val detailsJson: String? = null,
)

/**
 * Derived readable scope: one semantic Screen visit and the events that happened inside it.
 *
 * Screen duration is computed from entry -> transition time, not by summing per-event gaps.
 */
data class VexLogScreenScope(
    val screenId: String,
    val enteredAt: Instant,
    val exitedAt: Instant?,
    val records: List<VexLogRecord>,
) {
    val durationSeconds: Long?
        get() = exitedAt?.let { exit ->
            Duration.between(enteredAt, exit).seconds.coerceAtLeast(0L)
        }
}


/**
 * Read-only structural interpretation of one stable Logger subject coordinate.
 *
 * These fields are derived from the current matching Blueprint registry; they are not duplicated
 * canonical metadata stored in each log record. A different/missing registry can therefore be
 * reported honestly without rewriting the original fact.
 */
data class VexLogSubjectFields(
    val subjectRef: String,
    val ownerScreenId: String?,
    val placementPath: List<String>,
    val elementKind: String?,
    val declaredIntent: String?,
    val resolution: Resolution,
) {
    enum class Resolution {
        RESOLVED,
        RUNTIME_ELEMENT,
        NOT_A_BLUEPRINT_ELEMENT,
        BLUEPRINT_MAPPING_MISSING,
    }
}

class VexLogger(
    private val clock: () -> Instant = { Instant.now() },
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() },
    private val maxRecords: Int = 256,
    private val onRecord: (VexLogRecord) -> Unit = {},
    private val blueprintRegistry: UIBlueprintRegistry? = null,
) {
    /**
     * Root-owned structured logging boundary for foundations that need to report accepted facts
     * without inventing another logger root.
     *
     * A Sink decides how these structured events are projected. They do not automatically become
     * VexLogRecord UI-history entries merely because they belong to VexLogger.
     *
     * Structured logging is observational, not semantic authority. A foundation emitting an already
     * accepted fact must isolate Sink failure so logging cannot retroactively reject or rewrite that
     * fact.
     */
    fun interface Sink {
        fun record(
            event: Event,
        )

        companion object {
            val None:
                Sink =
                Sink {
                }
        }
    }

    /**
     * Structured fact ingress for runtime/semantic owners.
     *
     * The sink is a projection into this same Logger root; it is not a peer logger or event bus.
     */
    val sink:
        Sink =
        Sink(
            ::record,
        )

    /**
     * Already-accepted facts from rightful owners.
     *
     * Logger does not infer semantic/runtime meaning from Kotlin callables or class names. The
     * producer supplies the fact coordinate, action, and bounded detail that became true at its
     * boundary; Logger only orders and projects it.
     */
    sealed interface Event {
        val subjectRef:
            String
        val action:
            String
        val detailsJson:
            String?

        data class Semantic(
            override val subjectRef:
                String,
            override val action:
                String,
            override val detailsJson:
                String? =
                null,
        ) :
            Event

        data class Runtime(
            override val subjectRef:
                String,
            override val action:
                String,
            override val detailsJson:
                String? =
                null,
        ) :
            Event
    }

    private val lock = Any()
    private val _history = mutableStateOf<List<VexLogRecord>>(emptyList())
    private var nextSequence = 1L
    private var lastTimestamp: Instant? = null
    private var currentSection = 0L

    val history: State<List<VexLogRecord>>
        get() = _history

    init {
        require(maxRecords > 0) { "maxRecords must be positive" }
    }

    /**
     * Record one already-accepted/observed structured fact.
     */
    fun record(
        event:
            Event,
    ) {
        when (event) {
            is Event.Semantic ->
                append(
                    kind =
                        VexLogKind.Semantic,
                    subjectRef =
                        event.subjectRef,
                    action =
                        event.action,
                    screenBeforeId =
                        null,
                    screenAfterId =
                        null,
                    detailsJson =
                        event.detailsJson,
                )

            is Event.Runtime ->
                append(
                    kind =
                        VexLogKind.Runtime,
                    subjectRef =
                        event.subjectRef,
                    action =
                        event.action,
                    screenBeforeId =
                        null,
                    screenAfterId =
                        null,
                    detailsJson =
                        event.detailsJson,
                )
        }
    }

    fun appOpened(screen: ScreenElement) {
        append(
            kind = VexLogKind.AppOpened,
            subjectRef = "app",
            action = "App Opened",
            screenBeforeId = null,
            screenAfterId = screen.screenId,
        )
    }

    fun recordInteraction(
        element:
            UIElement,
        screen:
            ScreenElement,
        action:
            String =
            "Clicked",
        detailsJson:
            String? =
            null,
    ) {
        recordInteractionRef(
            subjectRef =
                element.elementId,
            screen =
                screen,
            action =
                action,
            detailsJson =
                detailsJson,
        )
    }

    /**
     * Dynamic repeated elements use a stable semantic ref even though they cannot all be members of
     * the static UI registry. [screen] is the Screen current when the fact is recorded; intended
     * destinations are not accepted by this production API.
     */
    fun recordInteractionRef(
        subjectRef:
            String,
        screen:
            ScreenElement,
        action:
            String =
            "Clicked",
        detailsJson:
            String? =
            null,
    ) {
        require(
            subjectRef.isNotBlank(),
        ) {
            "interaction subjectRef must not be blank"
        }

        append(
            kind =
                VexLogKind.Interaction,
            subjectRef =
                subjectRef,
            action =
                action,
            screenBeforeId =
                screen.screenId,
            screenAfterId =
                null,
            detailsJson =
                detailsJson,
        )

        if (
            subjectRef.startsWith(
                "element.",
            ) &&
            blueprintRegistry !=
                null &&
            blueprintRegistry.element(
                subjectRef,
            ) ==
                null
        ) {
            append(
                kind =
                    VexLogKind.Diagnostic,
                subjectRef =
                    subjectRef,
                action =
                    "BlueprintMappingMissing",
                screenBeforeId =
                    screen.screenId,
                screenAfterId =
                    null,
            )
        }
    }

    /**
     * Record one intentional runtime-instance interaction.
     *
     * The subject may use element.* shape because it is still a UI coordinate, but it is not
     * required to be a permanent static Blueprint member. This distinction prevents lawful dynamic
     * rows/actions from being reported as missing architecture.
     */
    fun recordRuntimeInteractionRef(
        subjectRef:
            String,
        screen:
            ScreenElement,
        action:
            String =
            "Clicked",
    ) {
        require(subjectRef.isNotBlank()) {
            "runtime interaction subjectRef must not be blank"
        }

        append(
            kind = VexLogKind.DynamicInteraction,
            subjectRef = subjectRef,
            action = action,
            screenBeforeId = screen.screenId,
            screenAfterId = null,
        )
    }

    fun recordBack(
        screen:
            ScreenElement,
    ) {
        append(
            kind =
                VexLogKind.Gesture,
            subjectRef =
                "system.back",
            action =
                "Back",
            screenBeforeId =
                screen.screenId,
            screenAfterId =
                null,
        )
    }

    /**
     * Record only the Screen that navigation reports as current after mutation.
     *
     * A prior/expected Screen is deliberately absent. Debug/test layers may derive A -> B from
     * chronological facts when they need that interpretation.
     */
    fun recordNavigationCurrent(
        screen:
            ScreenElement,
    ) {
        append(
            kind =
                VexLogKind.Navigation,
            subjectRef =
                screen.screenId,
            action =
                "CurrentScreen",
            screenBeforeId =
                null,
            screenAfterId =
                screen.screenId,
        )
    }

    /**
     * Pager callbacks arrive only after settlement, so the current Screen itself is the fact.
     */
    fun recordScreenGesture(
        screen:
            ScreenElement,
    ) {
        append(
            kind =
                VexLogKind.Gesture,
            subjectRef =
                "presentation.screen-pager",
            action =
                "Swiped",
            screenBeforeId =
                null,
            screenAfterId =
                screen.screenId,
        )
    }

    fun startNewSection() {
        synchronized(lock) {
            currentSection += 1
        }
    }

    fun clearHistory() {
        synchronized(lock) {
            _history.value = emptyList()
            nextSequence = 1L
            lastTimestamp = null
            currentSection = 0L
        }

        try {
            VexLogcat.historyCleared()
        } catch (_: Throwable) {
            // Logcat is a non-authoritative developer projection.
        }
    }

    fun renderHistory(): String = formatHistory(
        records = history.value,
        zoneId = zoneId(),
    )

    /**
     * Explicit structural projection for developer/debug use.
     *
     * Stable IDs remain the stored facts. The Blueprint registry resolves those IDs into owner,
     * placement, kind and declared UI intent at read time so Logger does not maintain a second map.
     */
    fun renderStructuredHistory(): String =
        formatStructuredHistory(
            records = history.value,
            zoneId = zoneId(),
            registry = blueprintRegistry,
        )

    private fun append(
        kind: VexLogKind,
        subjectRef: String,
        action: String,
        screenBeforeId: String?,
        screenAfterId: String?,
        detailsJson: String? = null,
    ) {
        val record = synchronized(lock) {
            val now = clock()
            val duration = lastTimestamp
                ?.let { Duration.between(it, now).seconds.coerceAtLeast(0L) }

            VexLogRecord(
                sequence = nextSequence++,
                timestamp = now,
                durationSincePreviousSeconds = duration,
                kind = kind,
                subjectRef = subjectRef,
                action = action,
                screenBeforeId = screenBeforeId,
                screenAfterId = screenAfterId,
                section = currentSection,
                detailsJson = detailsJson,
            ).also {
                _history.value = (_history.value + it).takeLast(maxRecords)
                lastTimestamp = now
            }
        }

        try {
            VexLogcat.record(record)
        } catch (_: Throwable) {
            // Logcat is a non-authoritative developer projection.
        }

        try {
            onRecord(record)
        } catch (_: Throwable) {
            // Downstream diagnostics are deliberately non-authoritative.
        }
    }

    companion object {
        /**
         * Resolve one stable subject coordinate through the rightful UI topology owner.
         *
         * element.* coordinates are expected to exist in the supplied Blueprint map unless they are
         * intentionally runtime/dynamic identities. Callers decide whether a missing mapping is a
         * conformance failure; this projection makes the gap visible instead of silently inventing
         * fields from string parsing.
         */
        fun subjectFields(
            subjectRef: String,
            registry: UIBlueprintRegistry?,
            runtimeCoordinate: Boolean = false,
        ): VexLogSubjectFields {
            if (
                !subjectRef.startsWith(
                    "element.",
                )
            ) {
                return VexLogSubjectFields(
                    subjectRef = subjectRef,
                    ownerScreenId = null,
                    placementPath = emptyList(),
                    elementKind = null,
                    declaredIntent = null,
                    resolution = VexLogSubjectFields.Resolution.NOT_A_BLUEPRINT_ELEMENT,
                )
            }

            val element =
                registry
                    ?.element(
                        subjectRef,
                    )
                    ?: return VexLogSubjectFields(
                        subjectRef = subjectRef,
                        ownerScreenId = null,
                        placementPath = emptyList(),
                        elementKind = null,
                        declaredIntent = null,
                        resolution =
                            if (
                                runtimeCoordinate
                            ) {
                                VexLogSubjectFields.Resolution.RUNTIME_ELEMENT
                            } else {
                                VexLogSubjectFields.Resolution.BLUEPRINT_MAPPING_MISSING
                            },
                    )

            val ancestors =
                buildList {
                    var current =
                        registry.parentOf(
                            element,
                        )

                    while (
                        current !=
                        null
                    ) {
                        add(
                            current.elementId,
                        )
                        current =
                            registry.parentOf(
                                current,
                            )
                    }
                }
                    .asReversed()

            return VexLogSubjectFields(
                subjectRef = subjectRef,
                ownerScreenId =
                    registry
                        .ownerScreenOf(
                            element,
                        )
                        ?.screenId,
                placementPath =
                    ancestors +
                        element.elementId,
                elementKind =
                    element.kind,
                declaredIntent =
                    (
                        element as?
                            ActionElement
                    )
                        ?.intent
                        ?.let(
                            ::intentField,
                        ),
                resolution = VexLogSubjectFields.Resolution.RESOLVED,
            )
        }

        /**
         * New explicit Debug/inspection representation.
         *
         * This intentionally keeps the old compact human history formatter available while the
         * structured representation is pressure-tested by real consumers.
         */
        fun formatStructuredHistory(
            records: List<VexLogRecord>,
            zoneId: ZoneId,
            registry: UIBlueprintRegistry?,
        ): String {
            if (records.isEmpty()) {
                return "No actions recorded yet."
            }

            val formatter =
                DateTimeFormatter
                    .ofPattern(
                        "h:mm:ssa MMM d, yyyy",
                        Locale.US,
                    )
                    .withZone(
                        zoneId,
                    )

            return records
                .joinToString(
                    separator = "\n\n",
                ) { record ->
                    val fields =
                        subjectFields(
                            subjectRef =
                                record.subjectRef,
                            registry =
                                registry,
                            runtimeCoordinate =
                                record.kind ==
                                    VexLogKind.DynamicInteraction,
                        )

                    buildString {
                        appendLine(
                            "[${formatter.format(record.timestamp)}]",
                        )
                        appendLine(
                            "fact.kind = ${record.kind}",
                        )
                        appendLine(
                            "fact.action = ${record.action}",
                        )
                        appendLine(
                            "source.ref = ${fields.subjectRef}",
                        )
                        appendLine(
                            "source.resolution = ${fields.resolution}",
                        )

                        fields.ownerScreenId
                            ?.let { screenId ->
                                appendLine(
                                    "source.screen = $screenId",
                                )
                            }

                        fields.elementKind
                            ?.let { kind ->
                                appendLine(
                                    "source.kind = $kind",
                                )
                            }

                        if (
                            fields.placementPath
                                .isNotEmpty()
                        ) {
                            appendLine(
                                "source.path = " +
                                    fields.placementPath
                                        .joinToString(
                                            " -> ",
                                        ),
                            )
                        }

                        fields.declaredIntent
                            ?.let { intent ->
                                appendLine(
                                    "source.intent = $intent",
                                )
                            }

                        record.detailsJson
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?.let { details ->
                                appendLine(
                                    "fact.details = $details",
                                )
                            }
                    }
                        .trimEnd()
                }
        }

        private fun intentField(
            intent: ElementIntent,
        ): String =
            when (intent) {
                is ElementIntent.NavigateTo ->
                    "NavigateTo(${intent.screen.screenId})"

                ElementIntent.NavigateBack ->
                    "NavigateBack"

                is ElementIntent.Open ->
                    "Open(${intent.element.elementId})"

                is ElementIntent.Close ->
                    "Close(${intent.element.elementId})"

                is ElementIntent.Toggle ->
                    "Toggle(${intent.element.elementId})"

                is ElementIntent.SwapTo ->
                    "SwapTo(${intent.container.elementId},${intent.child.elementId})"

                ElementIntent.LocalEffect ->
                    "LocalEffect"

                is ElementIntent.Invoke ->
                    "Invoke(${intent.action.actionId})"

                is ElementIntent.Sequence ->
                    intent.intents
                        .joinToString(
                            prefix = "Sequence(",
                            postfix = ")",
                            separator = " -> ",
                            transform = ::intentField,
                        )
            }

        /**
         * Groups canonical records into semantic Screen visits.
         *
         * A transition record belongs to its source Screen. The destination scope begins at that
         * same timestamp. A later AppOpened starts a new session scope rather than pretending the
         * app spent the offline interval on the previous Screen.
         */
        fun screenScopes(records: List<VexLogRecord>): List<VexLogScreenScope> {
            if (records.isEmpty()) return emptyList()

            val result = mutableListOf<VexLogScreenScope>()
            var currentScreenId: String? = null
            var enteredAt: Instant? = null
            var currentRecords = mutableListOf<VexLogRecord>()

            fun finish(exitedAt: Instant?) {
                val screenId = currentScreenId ?: return
                val entry = enteredAt ?: return
                result += VexLogScreenScope(
                    screenId = screenId,
                    enteredAt = entry,
                    exitedAt = exitedAt,
                    records = currentRecords.toList(),
                )
                currentScreenId = null
                enteredAt = null
                currentRecords = mutableListOf()
            }

            fun begin(screenId: String, timestamp: Instant) {
                currentScreenId = screenId
                enteredAt = timestamp
                currentRecords = mutableListOf()
            }

            records.forEach { record ->
                val eventScreenId = when {
                    record.kind == VexLogKind.AppOpened ->
                        record.screenAfterId ?: record.screenBeforeId

                    record.screenBeforeId != null ->
                        record.screenBeforeId

                    else ->
                        record.screenAfterId
                } ?: "screen.unknown"

                if (record.kind == VexLogKind.AppOpened) {
                    if (currentScreenId != null) {
                        // A new app session does not establish when the prior process actually ended.
                        finish(exitedAt = null)
                    }
                    begin(eventScreenId, record.timestamp)
                    currentRecords += record
                    return@forEach
                }

                if (currentScreenId == null) {
                    begin(eventScreenId, record.timestamp)
                } else if (currentScreenId != eventScreenId) {
                    // Handles truncated histories whose first observed record begins mid-session.
                    finish(exitedAt = record.timestamp)
                    begin(eventScreenId, record.timestamp)
                }

                currentRecords += record

                val destination = record.screenAfterId
                if (
                    record.screenBeforeId != null &&
                    destination != null &&
                    record.screenBeforeId != destination
                ) {
                    finish(exitedAt = record.timestamp)
                    begin(destination, record.timestamp)
                }
            }

            if (currentScreenId != null) {
                finish(exitedAt = null)
            }

            return result
        }

        fun formatHistory(
            records: List<VexLogRecord>,
            zoneId: ZoneId,
        ): String {
            if (records.isEmpty()) return "No actions recorded yet."

            val formatter = DateTimeFormatter
                .ofPattern("h:mm:ssa MMM d, yyyy", Locale.US)
                .withZone(zoneId)

            return buildString {
                screenScopes(records).forEachIndexed { scopeIndex, scope ->
                    if (scopeIndex > 0) appendLine()
                    appendLine("---${screenName(scope.screenId)}---")

                    scope.records.forEachIndexed { recordIndex, record ->
                        if (recordIndex > 0) appendLine()
                        appendLine("[${formatter.format(record.timestamp)}]")

                        when (record.kind) {
                            VexLogKind.AppOpened ->
                                appendLine("App Opened")

                            else -> {
                                val subject = eventName(record.subjectRef)
                                if (subject.equals(record.action, ignoreCase = true)) {
                                    appendLine(record.action)
                                } else {
                                    appendLine("$subject ${record.action}")
                                }

                                record.detailsJson
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { details ->
                                        appendLine(details)
                                    }
                            }
                        }
                    }

                    scope.durationSeconds?.let { seconds ->
                        if (scope.records.isNotEmpty()) appendLine()
                        appendLine("[Duration: $seconds seconds]")
                    }
                }
            }.trimEnd()
        }

        fun screenName(screenId: String?): String {
            if (screenId == null) return "UnknownScreen"
            val body = screenId.removePrefix("screen.")
            return body.split('.')
                .joinToString("") { pascal(it) } + "Screen"
        }

        /**
         * Compact event label used inside a Screen scope. The full stable ref remains in the record.
         */
        fun eventName(ref: String): String {
            if (ref == "system.back") return "Back"
            val body = ref
                .removePrefix("element.")
                .removePrefix("presentation.")
            return pascal(body.substringAfterLast('.'))
        }

        fun subjectName(ref: String): String {
            val body = ref
                .removePrefix("element.")
                .removePrefix("presentation.")
            return body.split('.')
                .joinToString(".") { pascal(it) }
        }

        private fun pascal(value: String): String =
            value.split('-', '_')
                .filter { it.isNotBlank() }
                .joinToString("") { part ->
                    part.replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
                    }
                }



    }
}
