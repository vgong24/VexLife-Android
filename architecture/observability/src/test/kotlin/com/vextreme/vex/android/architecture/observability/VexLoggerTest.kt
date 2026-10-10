package com.vextreme.vex.android.architecture.observability

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class VexLoggerTest {
    @Test
    fun logAndTraceRefsRejectUnboundedIdentity() {
        assertFailsWith<IllegalArgumentException> {
            VexLogRef(
                "l".repeat(257),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            VexTraceRef(
                "t".repeat(257),
            )
        }
    }

    @Test
    fun singleThreadExecutorProcessesAllWorkOnOneNamedThread() {
        val executor =
            VexSingleThreadLogExecutor(
                threadName = "test.vex.logger",
            )
        val threadNames =
            Collections.synchronizedList(
                mutableListOf<String>(),
            )

        try {
            repeat(5) {
                executor.execute {
                    threadNames +=
                        Thread.currentThread().name
                }
            }

            executor.flush()

            assertEquals(
                setOf("test.vex.logger"),
                threadNames.toSet(),
            )
        } finally {
            executor.close()
        }
    }

    @Test
    fun productionExecutorBoundsPendingDiagnosticWork() {
        val executor =
            VexSingleThreadLogExecutor(
                threadName = "test.vex.logger.bounded",
                maxPendingTasks = 1,
            )
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)

        try {
            assertTrue(
                executor.execute {
                    started.countDown()
                    release.await()
                },
            )

            assertTrue(
                started.await(
                    2L,
                    TimeUnit.SECONDS,
                ),
            )

            assertFalse(
                executor.execute {},
            )
        } finally {
            release.countDown()
            executor.flush()
            executor.close()
        }
    }

    @Test
    fun loggerRejectsOversizedDiagnosticEventsBeforeQueueing() {
        val logger =
            VexLogger(
                executor = VexImmediateLogExecutor(),
                maxEventFields = 1,
                maxEventCharacters = 32L,
            )

        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("source"),
                eventRef = VexLogRef("event"),
                fields =
                    mapOf(
                        "first" to "1",
                        "second" to "2",
                    ),
            ),
        )

        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("source"),
                eventRef = VexLogRef("event"),
                fields =
                    mapOf(
                        "payload" to "x".repeat(64),
                    ),
            ),
        )

        assertEquals(
            2L,
            logger.droppedLogCount,
        )
        assertTrue(
            logger.snapshot().isEmpty(),
        )
    }

    @Test
    fun loggerCountsDiagnosticsRejectedByExecutor() {
        val rejectingExecutor =
            object : VexLogExecutor {
                override fun execute(task: () -> Unit): Boolean =
                    false

                override fun flush() = Unit

                override fun close() = Unit
            }
        val logger =
            VexLogger(
                executor = rejectingExecutor,
            )

        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("relay.items"),
                eventRef = VexLogRef("relay.state.changed"),
            ),
        )

        assertEquals(
            1L,
            logger.droppedLogCount,
        )
        assertTrue(
            logger.snapshot().isEmpty(),
        )
    }

    @Test
    fun occurredTimestampIsCapturedBeforeDeferredLoggerProcessing() {
        val tasks =
            ArrayDeque<() -> Unit>()
        val controlledExecutor =
            object : VexLogExecutor {
                override fun execute(task: () -> Unit): Boolean {
                    tasks.addLast(task)
                    return true
                }

                override fun flush() {
                    while (tasks.isNotEmpty()) {
                        tasks.removeFirst().invoke()
                    }
                }

                override fun close() = Unit
            }

        var now = 100L
        val logger =
            VexLogger(
                executor = controlledExecutor,
                clock = VexLogClock { now },
            )

        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("relay.items"),
                eventRef = VexLogRef("relay.state.changed"),
            ),
        )

        now = 999L

        assertEquals(
            100L,
            logger.snapshot().single().occurredAtEpochMillis,
        )
    }

    @Test
    fun immediateExecutorStillSerializesConcurrentProducers() {
        val logger =
            VexLogger(
                executor = VexImmediateLogExecutor(),
                maxRecentRecords = 100,
            )
        val pool =
            Executors.newFixedThreadPool(4)

        try {
            val futures =
                (1..100).map { value ->
                    pool.submit {
                        logger.log(
                            VexLogEvent(
                                sourceRef = VexLogRef("test.concurrent"),
                                eventRef = VexLogRef("event"),
                                fields =
                                    mapOf(
                                        "value" to value.toString(),
                                    ),
                            ),
                        )
                    }
                }

            futures.forEach { future ->
                future.get(
                    2L,
                    TimeUnit.SECONDS,
                )
            }

            assertEquals(
                100,
                logger.snapshot().size,
            )
            assertEquals(
                (1L..100L).toList(),
                logger.snapshot().map { record ->
                    record.sequence
                },
            )
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun concurrentInjectedExecutorCannotLoseRetainedRecords() {
        val threads =
            mutableListOf<Thread>()
        val executor =
            object : VexLogExecutor {
                override fun execute(task: () -> Unit): Boolean {
                    val thread =
                        Thread(task)
                    synchronized(threads) {
                        threads += thread
                    }
                    thread.start()
                    return true
                }

                override fun flush() {
                    val snapshot =
                        synchronized(threads) {
                            threads.toList()
                        }
                    snapshot.forEach { thread ->
                        thread.join()
                    }
                }

                override fun close() {
                    flush()
                }
            }
        val logger =
            VexLogger(
                executor = executor,
                maxRecentRecords = 128,
            )

        repeat(100) { index ->
            logger.log(
                VexLogEvent(
                    sourceRef = VexLogRef("concurrent"),
                    eventRef = VexLogRef("event"),
                    fields =
                        mapOf(
                            "index" to index.toString(),
                        ),
                ),
            )
        }

        val records =
            logger.snapshot()

        assertEquals(
            100,
            records.size,
        )
        assertEquals(
            (1L..100L).toList(),
            records.map { record ->
                record.sequence
            },
        )
    }

    @Test
    fun loggerCanCloseFromItsOwnWorkerWithoutDeadlock() {
        val executor =
            VexSingleThreadLogExecutor(
                threadName = "test.vex.logger.self-close",
            )
        val logger =
            VexLogger(
                executor = executor,
            )
        val closed =
            CountDownLatch(1)

        assertTrue(
            executor.execute {
                logger.close()
                closed.countDown()
            },
        )

        assertTrue(
            closed.await(
                2L,
                TimeUnit.SECONDS,
            ),
        )

        executor.close()
    }

    @Test
    fun loggerCloseFenceDoesNotDependOnExecutorCloseBehavior() {
        val logger =
            VexLogger(
                executor = VexImmediateLogExecutor(),
            )

        logger.close()

        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("after.immediate.close"),
                eventRef = VexLogRef("event"),
            ),
        )

        assertEquals(
            1L,
            logger.droppedLogCount,
        )
        assertTrue(
            logger.snapshot().isEmpty(),
        )
    }

    @Test
    fun loggerCloseIsRepeatableAndPostCloseLogsAreDropped() {
        val logger =
            VexLogger(
                executor =
                    VexSingleThreadLogExecutor(
                        threadName = "test.vex.logger.close",
                    ),
            )

        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("before.close"),
                eventRef = VexLogRef("event"),
            ),
        )

        logger.close()
        logger.close()

        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("after.close"),
                eventRef = VexLogRef("event"),
            ),
        )

        assertEquals(
            1L,
            logger.droppedLogCount,
        )
        assertEquals(
            listOf("before.close"),
            logger.snapshot().map { record ->
                record.event.sourceRef.value
            },
        )
    }

    @Test
    fun loggerSequencesFiltersAndBoundsRecentRecords() {
        val logger =
            VexLogger(
                executor = VexImmediateLogExecutor(),
                clock = VexLogClock { 1234L },
                maxRecentRecords = 2,
            )

        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("relay.items"),
                eventRef = VexLogRef("relay.state.changed"),
            ),
        )
        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("execution.vex.app"),
                eventRef = VexLogRef("coroutine.running"),
            ),
        )
        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("relay.items"),
                eventRef = VexLogRef("relay.state.changed"),
            ),
        )

        val all =
            logger.snapshot()

        assertEquals(2, all.size)
        assertEquals(
            listOf(2L, 3L),
            all.map { it.sequence },
        )
        assertEquals(
            listOf(1234L, 1234L),
            all.map { it.occurredAtEpochMillis },
        )

        val relayOnly =
            logger.snapshot(
                VexLogFilter(
                    sourcePrefix = "relay.",
                ),
            )

        assertEquals(1, relayOnly.size)
        assertEquals(
            "relay.items",
            relayOnly.single().event.sourceRef.value,
        )
    }

    @Test
    fun loggerDetachesMutableFieldsBeforeAsyncProcessing() {
        val logger =
            VexLogger(
                executor = VexImmediateLogExecutor(),
            )
        val fields =
            linkedMapOf(
                "count" to "1",
            )

        logger.log(
            VexLogEvent(
                sourceRef = VexLogRef("relay.items"),
                eventRef = VexLogRef("relay.input.received"),
                fields = fields,
            ),
        )

        fields["count"] = "999"

        val record =
            logger.snapshot().single()

        assertEquals(
            "1",
            record.event.fields["count"],
        )

        @Suppress("UNCHECKED_CAST")
        val exposed =
            record.event.fields as MutableMap<String, String>

        exposed["count"] = "mutated"

        assertEquals(
            "1",
            logger.snapshot().single().event.fields["count"],
        )
    }

    @Test
    fun ordinaryLoggerBugIsIsolatedButJvmFatalErrorPropagates() {
        val assertionSink =
            object : VexLogSink {
                override fun log(event: VexLogEvent) {
                    throw AssertionError("diagnostic assertion")
                }
            }

        assertionSink.logSafely(
            VexLogEvent(
                sourceRef = VexLogRef("test.assertion"),
                eventRef = VexLogRef("isolated"),
            ),
        )

        val fatalSink =
            object : VexLogSink {
                override fun log(event: VexLogEvent) {
                    throw OutOfMemoryError("fatal logger error")
                }
            }

        assertFailsWith<OutOfMemoryError> {
            fatalSink.logSafely(
                VexLogEvent(
                    sourceRef = VexLogRef("test.fatal"),
                    eventRef = VexLogRef("fatal"),
                ),
            )
        }
    }

    @Test
    fun observerLoggingFailureCannotChangeObservedValues() = runBlocking {
        val throwingSink =
            object : VexLogSink {
                override fun log(event: VexLogEvent) {
                    error("diagnostic sink unavailable")
                }
            }

        val sinkFailureValues =
            flowOf(1, 2, 3)
                .vexLog(
                    logger = throwingSink,
                    sourceRef = VexLogRef("screen.events.throwing-sink"),
                )
                .toList()

        assertEquals(
            listOf(1, 2, 3),
            sinkFailureValues,
        )

        val mapperFailureValues =
            flowOf(4, 5, 6)
                .vexLog(
                    logger = VexLogSink.None,
                    sourceRef = VexLogRef("screen.events.throwing-mapper"),
                    fields = {
                        error("diagnostic projection unavailable")
                    },
                )
                .toList()

        assertEquals(
            listOf(4, 5, 6),
            mapperFailureValues,
        )
    }

    @Test
    fun flowInterceptorLogsWhatObserverReceivedWithoutChangingValues() = runBlocking {
        val logger =
            VexLogger(
                executor = VexImmediateLogExecutor(),
            )

        val values =
            flowOf(1, 2, 3)
                .vexLog(
                    logger = logger,
                    sourceRef = VexLogRef("screen.events.items"),
                ) { value ->
                    mapOf(
                        "value" to value.toString(),
                    )
                }
                .toList()

        assertEquals(
            listOf(1, 2, 3),
            values,
        )

        val logs =
            logger.snapshot()

        assertEquals(3, logs.size)
        assertTrue(
            logs.all {
                it.event.eventRef ==
                    VexLogRef("flow.observed")
            },
        )
        assertNotSame(
            logs[0].event.fields,
            logger.snapshot()[0].event.fields,
        )
    }
}

// [VXG RealForever]
