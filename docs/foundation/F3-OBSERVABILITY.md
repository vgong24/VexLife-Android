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
  stable source/event/trace refs
  bounded recent records
  filters
~~~

## Production ordering

Application composition uses one VexLogger singleton with one VexSingleThreadLogExecutor("vex.logger").

The logger executor is injectable. Tests use deterministic executors.

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

## Automatic logging

Relay logs structural state processing without dumping state.

VexAppScope logs coroutine lifecycle without becoming semantic state owner.

## Optional interceptor

Flow<T>.vexLog(...) records what one observer received while forwarding values unchanged.

Owner logging and observer logging answer different questions and must not be conflated.

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
- deterministic injected executor;
- sequence ordering;
- bounded recent window;
- filtering;
- mutable-field detachment;
- observer interceptor value preservation.

Cross-module tests prove Relay and VexAppScope emit structured records.

## Example

Read:

architecture/observability/examples/LOGGER_AND_INTERCEPTOR.md

<!-- [VXG RealForever][VEXLIFE-ANDROID][F3][OBSERVABILITY] -->
