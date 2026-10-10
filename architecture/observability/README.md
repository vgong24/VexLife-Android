# Vex Observability / VexLogger

[VXG RealForever]

## The question comes first

> The architecture is working. How can a developer see what launched, what changed, what observed it,
> and how one change flowed through the system without reading unbounded raw logs?

Use VexLogger.

~~~text
Relay
  emits bounded semantic transition facts

VexAppScope
  emits bounded coroutine lifecycle facts

optional observer
  .vexLog { bounded fields }

        ↓

VexLogger
  one ordered logging executor
  stable source/event refs
  bounded recent diagnostic window
  filterable snapshots

        ↓ later

BulkDataLogging / Design Lab / external sink
~~~

VexLogger is observability. It is not semantic state ownership.

## Single-thread ordering

Production VexLogger uses VexSingleThreadLogExecutor.

One dedicated thread serializes log processing so events submitted in order from one caller are
recorded in order without requiring every producer to coordinate locks.

The executor is injected:

~~~text
VexLogger(
  executor = VexSingleThreadLogExecutor()
)
~~~

Tests can instead use VexImmediateLogExecutor or another deterministic executor.

This keeps logger ordering testable rather than coupling tests to thread timing.

## Structured records, not toString()

Do not use arbitrary object toString() as protocol or durable identity.

Each event carries stable refs:

~~~text
sourceRef
eventRef
traceRef? / parentTraceRef?
originRef?
bounded string fields
~~~

Examples:

~~~text
sourceRef = relay.event-screen
eventRef  = relay.state.changed

sourceRef = execution.vex.app
eventRef  = coroutine.restarting
originRef = home.connection.listener
~~~

The fields map should contain bounded diagnostic facts such as revision/count/status. Do not dump
entire state graphs merely because logging is available.

## Automatic owner logging versus observer logging

Owner logging answers:

> What did the rightful owner do?

Relay can emit:

~~~text
relay.input.received
relay.state.changed
relay.state.unchanged
compound.activated
compound.deactivated
~~~

VexAppScope can emit:

~~~text
coroutine.starting
coroutine.running
coroutine.restarting
coroutine.completed
coroutine.cancelled
coroutine.failed
~~~

Those should happen automatically at the owner.

Observer logging answers a different question:

> What did this consumer receive?

Use the optional Flow extension:

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
    .collect { ... }
~~~

The extension returns the original values unchanged.

## Filtering

VexLogger keeps a bounded recent projection and supports filters by:

~~~text
source prefix
event prefix
trace ref
~~~

That enables a future Design Lab to ask:

~~~text
show relay.event-screen.*
show execution.vex.app coroutine failures
show one traceRef end-to-end
~~~

without scanning all BulkData.

## BulkDataLogging boundary

The current logger is not durable storage.

~~~text
VexLogger recent records
!= complete historical telemetry
!= event ledger
!= semantic truth
~~~

A later BulkDataLogging adapter can consume VexLogRecord values if a real retention/export need is
established.

## Causal tracing

VexTraceRef and parentTraceRef are reserved now so a future path can be reconstructed:

~~~text
external arrival
-> child Relay
-> Compound Relay
-> presentation mapping
-> UI observer
~~~

Do not invent traces where no actual causal relationship exists.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][OBSERVABILITY] -->
