package com.vextreme.vex.android.architecture.observability

import java.util.Collections
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class VexLoggerTest {
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
