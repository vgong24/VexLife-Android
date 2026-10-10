package com.vextreme.vex.android.architecture.observability

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * Optional observation interceptor.
 *
 * This records what this observer received. It does not replace automatic owner-level logging from
 * Relay or VexAppScope.
 */
fun <Value> Flow<Value>.vexLog(
    logger: VexLogSink,
    sourceRef: VexLogRef,
    eventRef: VexLogRef = VexLogRef("flow.observed"),
    traceRef: ((Value) -> VexTraceRef?) = { null },
    fields: (Value) -> Map<String, String> = { emptyMap() },
): Flow<Value> =
    onEach { value ->
        logger.log(
            VexLogEvent(
                sourceRef = sourceRef,
                eventRef = eventRef,
                traceRef = traceRef(value),
                fields = LinkedHashMap(fields(value)),
            ),
        )
    }

// [VXG RealForever]
