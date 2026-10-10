# VexLogger examples

Application composition:

~~~kotlin
single {
    VexLogger(
        executor = VexSingleThreadLogExecutor(
            threadName = "vex.logger",
            maxPendingTasks = 1024,
        ),
    )
}
~~~

Filter recent architecture events:

~~~kotlin
val relayEvents = vexLogger.snapshot(
    VexLogFilter(
        sourcePrefix = "relay.",
    ),
)
~~~

Optional observer-side interception:

~~~kotlin
relay.currentState
    .vexLog(
        logger = vexLogger,
        sourceRef = VexLogRef("screen.events.compose"),
    ) { state ->
        mapOf(
            "revision" to state.revision.toString(),
        )
    }
    .collect { state ->
        render(state)
    }
~~~

Keep observer projections small and non-blocking; they execute on the collector path.

If diagnostics saturate the production logger, product work continues and the loss is observable:

~~~kotlin
val dropped = vexLogger.droppedLogCount
~~~

Do not log entire state objects, credentials, tokens, private content payloads, or unnecessary personal
data by default. VexLogger is a bounded recent diagnostic projection, not
durable BulkData storage.
