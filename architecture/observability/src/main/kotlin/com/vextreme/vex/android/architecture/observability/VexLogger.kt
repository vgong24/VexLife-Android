package com.vextreme.vex.android.architecture.observability

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@JvmInline
value class VexLogRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexLogRef must be non-blank"
        }
    }

    override fun toString(): String = value
}

@JvmInline
value class VexTraceRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexTraceRef must be non-blank"
        }
    }

    override fun toString(): String = value
}

data class VexLogEvent(
    val sourceRef: VexLogRef,
    val eventRef: VexLogRef,
    val traceRef: VexTraceRef? = null,
    val parentTraceRef: VexTraceRef? = null,
    val originRef: VexLogRef? = null,
    val fields: Map<String, String> = emptyMap(),
) {
    fun detached(): VexLogEvent =
        copy(
            fields = LinkedHashMap(fields),
        )
}

data class VexLogRecord(
    val sequence: Long,
    val occurredAtEpochMillis: Long,
    val event: VexLogEvent,
) {
    fun detached(): VexLogRecord =
        copy(
            event = event.detached(),
        )
}

fun interface VexLogClock {
    fun nowEpochMillis(): Long

    companion object {
        val System =
            VexLogClock {
                java.lang.System.currentTimeMillis()
            }
    }
}

/**
 * Execution seam for logger ordering.
 *
 * Production uses one dedicated thread. Tests may inject an immediate or controlled executor.
 */
interface VexLogExecutor : AutoCloseable {
    fun execute(task: () -> Unit)

    fun flush()
}

class VexSingleThreadLogExecutor(
    threadName: String = "vex.logger",
) : VexLogExecutor {
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { task ->
            Thread(
                task,
                threadName,
            ).apply {
                isDaemon = true
            }
        }

    override fun execute(task: () -> Unit) {
        executor.execute(task)
    }

    override fun flush() {
        executor.submit {}.get()
    }

    override fun close() {
        executor.shutdown()

        if (!executor.awaitTermination(2L, TimeUnit.SECONDS)) {
            executor.shutdownNow()
        }
    }
}

class VexImmediateLogExecutor : VexLogExecutor {
    override fun execute(task: () -> Unit) {
        task()
    }

    override fun flush() = Unit

    override fun close() = Unit
}

interface VexLogSink {
    fun log(event: VexLogEvent)

    data object None : VexLogSink {
        override fun log(event: VexLogEvent) = Unit
    }
}

data class VexLogFilter(
    val sourcePrefix: String? = null,
    val eventPrefix: String? = null,
    val traceRef: VexTraceRef? = null,
) {
    fun matches(record: VexLogRecord): Boolean {
        val event = record.event

        return (sourcePrefix == null || event.sourceRef.value.startsWith(sourcePrefix)) &&
            (eventPrefix == null || event.eventRef.value.startsWith(eventPrefix)) &&
            (traceRef == null || event.traceRef == traceRef)
    }
}

/**
 * Ordered, bounded in-memory diagnostic projection.
 *
 * VexLogger is not semantic truth and not durable BulkData storage.
 */
class VexLogger(
    private val executor: VexLogExecutor,
    private val clock: VexLogClock = VexLogClock.System,
    private val maxRecentRecords: Int = 512,
) : VexLogSink,
    AutoCloseable {
    private val sequence =
        AtomicLong(0L)

    private val mutableRecentLogs =
        MutableStateFlow<List<VexLogRecord>>(
            emptyList(),
        )

    val recentLogs: StateFlow<List<VexLogRecord>> =
        DefensiveLogStateFlow(
            mutableRecentLogs.asStateFlow(),
        )

    init {
        require(maxRecentRecords >= 1) {
            "maxRecentRecords must be at least 1"
        }
    }

    override fun log(event: VexLogEvent) {
        val detachedEvent =
            event.detached()

        executor.execute {
            val record =
                VexLogRecord(
                    sequence = sequence.incrementAndGet(),
                    occurredAtEpochMillis = clock.nowEpochMillis(),
                    event = detachedEvent,
                )

            mutableRecentLogs.value =
                (mutableRecentLogs.value + record)
                    .takeLast(maxRecentRecords)
                    .map { it.detached() }
        }
    }

    /**
     * Wait until every event submitted before this call has been processed.
     */
    fun flush() {
        executor.flush()
    }

    fun snapshot(
        filter: VexLogFilter = VexLogFilter(),
    ): List<VexLogRecord> {
        flush()

        return recentLogs.value
            .filter(filter::matches)
            .map { it.detached() }
    }

    override fun close() {
        executor.flush()
        executor.close()
    }
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class DefensiveLogStateFlow(
    private val source: StateFlow<List<VexLogRecord>>,
) : StateFlow<List<VexLogRecord>> {
    override val replayCache: List<List<VexLogRecord>>
        get() = listOf(value)

    override val value: List<VexLogRecord>
        get() =
            source.value.map { record ->
                record.detached()
            }

    override suspend fun collect(
        collector: FlowCollector<List<VexLogRecord>>,
    ): Nothing =
        source.collect { records ->
            collector.emit(
                records.map { record ->
                    record.detached()
                },
            )
        }
}

// [VXG RealForever]
