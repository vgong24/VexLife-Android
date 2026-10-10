# VexLogger examples

Application composition:

~~~kotlin
single {
    VexLogger(
        executor = VexSingleThreadLogExecutor(
            threadName = "vex.logger",
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

Do not log entire state objects by default.
