# F3 — VexLogger / Structured Observability

[VXG RealForever]

## Intention

Make architecture behavior perceivable without making logs another truth owner or requiring developers
to reconstruct flow from unbounded BulkData.

## Current shape

~~~text
Relay
  bounded owner events

VexAppScope
  bounded coroutine lifecycle events

optional Flow.vexLog(...)
  observer-received events

        ↓

VexLogger
  dedicated single-thread executor
  bounded pending diagnostic admission
  explicit rejected-log count
  stable source/event/trace refs
  bounded recent record count
  filters
~~~

## Production ordering

Application composition uses one VexLogger singleton with one VexSingleThreadLogExecutor("vex.logger").

The logger executor is injectable. Tests use deterministic executors that preserve serialized task
execution without requiring a background thread.

Production admission is bounded. Saturated/closed diagnostic execution rejects new records instead
of applying backpressure to product owners; `droppedLogCount` reports executor-admission loss.

`occurredAtEpochMillis` is captured when `log(...)` is called, before queued processing.
Logger `sequence` is diagnostic recording order, not universal cross-owner causal truth. Relay
carries an owner-local `transitionSequence` on successful processing so serialized Relay order can
be reconstructed even though sink submission happens after the state lock is released.

## Stable identity

Logging identity is authored explicitly.

Do not derive durable meaning from:

~~~text
class name
lambda identity
toString()
obfuscated symbol
localized text
~~~

Current structured fields:

~~~text
sourceRef
eventRef
traceRef?
parentTraceRef?
originRef?
bounded fields
~~~

Bounded does not mean safe-to-store. Do not place credentials/tokens/authentication material,
private content payloads, or unnecessary personal data in diagnostic fields. Sensitive
observability requires its own explicit redaction/access/retention contract when a real consumer
earns that need.

## Automatic logging

Relay logs structural state processing without dumping state.

VexAppScope logs coroutine lifecycle without becoming semantic state owner.

## Optional interceptor

Flow<T>.vexLog(...) records what one observer received while forwarding values unchanged.

Owner logging and observer logging answer different questions and must not be conflated.

Observability is best-effort and failure-isolated. Owner modules use `VexLogSink.logSafely(...)`,
and `Flow.vexLog(...)` also isolates ordinary diagnostic projection/sink failures, so a logger
failure cannot become a Relay transition, execution, cancellation, activation-cleanup, or observed-
flow failure.

The only failures deliberately not reclassified as dropped diagnostics are JVM-fatal
`VirtualMachineError`, `ThreadDeath`, and `LinkageError`.

Observer `traceRef`/field projection code runs on the observed flow's collection path. Keep those
diagnostic mappers bounded/non-blocking; use them to project small facts, not to perform I/O or work.

## Boundary

~~~text
VEX_LOGGER != SEMANTIC_TRUTH
VEX_LOGGER != EVENT_LEDGER
VEX_LOGGER != BULKDATA_STORAGE
~~~

BulkData persistence/export remains a future adapter.

## Proof

VexLoggerTest covers:

- one named production log thread;
- bounded pending-work admission and rejected-log accounting;
- deterministic injected executor, including concurrent producers;
- producer-time timestamp capture before deferred processing;
- sequence ordering and bounded recent window;
- filtering and mutable-field detachment;
- observer interceptor value preservation;
- ordinary sink/projection failure isolation from the observed flow;
- JVM-fatal diagnostic failure propagation;
- cross-module owner failure isolation in Relay and VexAppScope.

Cross-module tests prove Relay and VexAppScope emit structured records.

## Example

Read:

architecture/observability/examples/LOGGER_AND_INTERCEPTOR.md

<!-- [VXG RealForever][VEXLIFE-ANDROID][F3][OBSERVABILITY] -->
