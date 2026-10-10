package com.vextreme.vex.android.architecture.observability

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
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
        require(value.length <= 256) {
            "VexLogRef must be at most 256 characters"
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
        require(value.length <= 256) {
            "VexTraceRef must be at most 256 characters"
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
    /**
     * Returns false when the diagnostic task was intentionally not admitted.
     *
     * Logging backpressure must not become product backpressure.
     */
    fun execute(task: () -> Unit): Boolean

    fun flush()
}

class VexSingleThreadLogExecutor(
    threadName: String = "vex.logger",
    maxPendingTasks: Int = 1024,
) : VexLogExecutor {
    @Volatile
    private var workerThread: Thread? =
        null

    private val pendingPermits =
        Semaphore(maxPendingTasks)

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { task ->
            Thread(
                task,
                threadName,
            ).apply {
                isDaemon = true
                this@VexSingleThreadLogExecutor.workerThread =
                    this
            }
        }

    init {
        require(maxPendingTasks >= 1) {
            "maxPendingTasks must be at least 1"
        }
    }

    override fun execute(task: () -> Unit): Boolean {
        if (!pendingPermits.tryAcquire()) {
            return false
        }

        return try {
            executor.execute {
                try {
                    task()
                } finally {
                    pendingPermits.release()
                }
            }
            true
        } catch (rejected: RejectedExecutionException) {
            pendingPermits.release()
            false
        }
    }

    override fun flush() {
        if (Thread.currentThread() === workerThread) {
            return
        }

        try {
            executor.submit {}.get()
        } catch (_: RejectedExecutionException) {
            // Already closed: every task admitted before shutdown is handled by close()/termination.
        }
    }

    override fun close() {
        executor.shutdown()

        if (Thread.currentThread() === workerThread) {
            return
        }

        if (!executor.awaitTermination(2L, TimeUnit.SECONDS)) {
            executor.shutdownNow()
        }
    }
}

class VexImmediateLogExecutor : VexLogExecutor {
    private val lock =
        Any()

    override fun execute(task: () -> Unit): Boolean {
        synchronized(lock) {
            task()
        }
        return true
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

/**
 * Best-effort observability boundary.
 *
 * Architecture owners use this when logging must never change semantic state, cancellation,
 * activation, or observer delivery. A broken/closed diagnostic sink may lose that diagnostic event;
 * it must not become the failure of the operation being observed.
 *
 * Sink health can be observed by the sink's own rightful owner. Do not recursively log a logging
 * failure back into the same sink.
 */
internal inline fun runDiagnosticSafely(
    block: () -> Unit,
) {
    try {
        block()
    } catch (failure: Throwable) {
        when (failure) {
            is VirtualMachineError,
            is ThreadDeath,
            is LinkageError -> throw failure

            else ->
                Unit
        }
    }
}

fun VexLogSink.logSafely(event: VexLogEvent) {
    runDiagnosticSafely {
        log(event)
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
    private val maxEventFields: Int = 32,
    private val maxEventCharacters: Long = 8_192L,
) : VexLogSink,
    AutoCloseable {
    private val sequence =
        AtomicLong(0L)
    private val admissionLock =
        Any()
    private var closed =
        false
    private val recordLock =
        Any()
    private val droppedLogs =
        AtomicLong(0L)

    val droppedLogCount: Long
        get() = droppedLogs.get()

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
        require(maxEventFields >= 0) {
            "maxEventFields must be non-negative"
        }
        require(maxEventCharacters >= 1L) {
            "maxEventCharacters must be at least 1"
        }
    }

    override fun log(event: VexLogEvent) {
        val admitted =
            synchronized(admissionLock) {
                if (closed || !eventWithinBounds(event)) {
                    false
                } else {
                    val detachedEvent =
                        event.detached()
                    val occurredAtEpochMillis =
                        clock.nowEpochMillis()

                    executor.execute {
                        synchronized(recordLock) {
                            val record =
                                VexLogRecord(
                                    sequence = sequence.incrementAndGet(),
                                    occurredAtEpochMillis = occurredAtEpochMillis,
                                    event = detachedEvent,
                                )

                            mutableRecentLogs.value =
                                (mutableRecentLogs.value + record)
                                    .takeLast(maxRecentRecords)
                                    .map { it.detached() }
                        }
                    }
                }
            }

        if (!admitted) {
            droppedLogs.incrementAndGet()
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

    private fun eventWithinBounds(
        event: VexLogEvent,
    ): Boolean {
        if (event.fields.size > maxEventFields) {
            return false
        }

        var characters =
            event.sourceRef.value.length.toLong() +
                event.eventRef.value.length.toLong() +
                (event.traceRef?.value?.length?.toLong() ?: 0L) +
                (event.parentTraceRef?.value?.length?.toLong() ?: 0L) +
                (event.originRef?.value?.length?.toLong() ?: 0L)

        for ((key, value) in event.fields) {
            characters +=
                key.length.toLong() +
                    value.length.toLong()

            if (characters > maxEventCharacters) {
                return false
            }
        }

        return characters <= maxEventCharacters
    }

    override fun close() {
        val shouldClose =
            synchronized(admissionLock) {
                if (closed) {
                    false
                } else {
                    closed = true
                    true
                }
            }

        if (!shouldClose) {
            return
        }

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
