# Vex Relay — EAP-00 processing kernel

[VXG RealForever]

## The question

> External information can arrive while the human is somewhere else in the app. How do we keep the
> source truth reliable, combine only the current dependencies that matter, and make the processing
> logic easy to find without turning Compose or a tooltip manager into the state owner?

This module is the **current-main extraction** of the smallest Relay processing contracts earned by
the Events Attention Practicum.

Historical draft PR #3 remains architecture quarry. EAP-00 does not merge/cherry-pick that branch
wholesale and does not import its experimental AppScope/observability stack.

## Developer shape

One truth:

~~~text
external/source state
-> VexRelay.processState(...)
-> one current state projection
~~~

Several related truths:

~~~text
current screen ----\
external feed ------+-> EventsAttentionRelay.processStates(...) -> current Events projection
ack/preference -----/
~~~

The concrete Relay should be boring to inspect:

1. read its input state to see the dependencies;
2. read processState / processStates to see the reconciliation law;
3. read its output state to see what consumers are allowed to depend on.

## EAP-00 owns

- explicit transitive defensive-copy contract;
- one serialized processing boundary;
- source-level semantic freshness in the smallest rightful Relay;
- Compound Relay retained input + derived output;
- nullable top-level input patches as "no new dependency value supplied";
- sibling patch serialization so concurrent inputs cannot erase one another;
- semantic no-op suppression;
- Compound revision only when retained/derived meaning changes.

## EAP-00 deliberately does not own

- external SDK/service adapters;
- scheduled synthetic source lifecycle;
- coroutine/job supervision or startup/readiness;
- persistent event history;
- tooltip/snackbar/dialog rendering;
- transient priority/arbitration policy beyond the pure Events practicum fixture;
- anchored geometry;
- Journey/ScreenCrawler integration;
- VexLogger/BulkDataLogging.

Those are not omissions to paper over. They are the next consumers that must earn deeper machinery.

## Events practicum proof

VexRelayEventsPracticumTest already asks the first real questions:

~~~text
newer provider revision -> accepted
older provider revision -> cannot overwrite

external event arrives while Home current
-> event truth retained
-> attention candidate = none

Events becomes current
-> same retained event becomes eligible

acknowledge first event
-> event remains in truth
-> next unacknowledged event becomes candidate

screen/feed/preference arrive concurrently
-> one coherent result
-> no sibling dependency is lost
~~~

Permanent distinction:

~~~text
CANONICAL_EVENT_EXISTS != PRESENTATION_CURRENTLY_ELIGIBLE
STATEFLOW_CURRENT_PROJECTION != EVENT_LEDGER
ARRIVAL_ORDER != SEMANTIC_SOURCE_ORDER
~~~

## Next edge — EAP-01

Use this kernel from the actual Events feature:

~~~text
deterministic scheduled synthetic provider
-> feature-owned external feed Relay
-> stable dynamic Events list
-> information continues arriving while another Screen is current
~~~

Only that real producer lifetime should decide whether a reusable async dependency-binding /
activation owner is needed.

<!-- [VXG RealForever][VEXLIFE-ANDROID][EVENTS-ATTENTION-PRACTICUM][EAP-00][RELAY] -->
