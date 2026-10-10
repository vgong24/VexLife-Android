# VexAppScope example

~~~kotlin
val appScope = VexAppScope(
    logSink = vexLogger,
)

val handle = appScope.launchCoroutine(
    coroutineRef = VexCoroutineRef("home.connection.listener"),
    restartPolicy = VexCoroutineRestartPolicy.OnFailure(
        maxRestarts = 3,
        delayMillis = 1_000,
    ),
) {
    // Any launch { ... } here is a child of this named execution.
    observeHomeConnection()
}

// deliberate stop
handle.forceStop("home disabled")
handle.join()
~~~

Application composition:

~~~kotlin
single { VexAppScope(logSink = get<VexLogger>()) }
~~~

Do not use VexAppScope as durable operation identity or semantic retry authority.
