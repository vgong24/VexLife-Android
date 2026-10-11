# Vex Relay — processing kernel

[VXG RealForever]

## The question

> External information can arrive while the human is somewhere else in the app. How do we keep the
> source truth reliable, combine only the current dependencies that matter, and make the processing
> logic easy to find without turning Compose or a tooltip manager into the state owner?

This module is the **current-main extraction** of the smallest Relay processing contracts earned by
the Events Attention Practicum.

Historical draft PR #3 remains architecture quarry. EAP-00 did not merge/cherry-pick that branch
wholesale and did not import its experimental AppScope/observability stack.

The current concrete consumer is the Events Attention Practicum under
`docs/foundation/EVENTS-ATTENTION-PRACTICUM.md`. That feature now exercises this kernel with a
normalized external feed, stable dynamic list identity, acknowledgement/defer/suppress state,
attention availability, anchored tooltip rendering, deterministic Journey signals, and ScreenCrawler
selection. Those feature concerns remain outside this module rather than accreting into Relay itself.

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

### Compound observation surfaces

A Compound Relay has three intentionally different observation surfaces:

~~~text
states
  full retained totality:
  inputState + outputState + revision

  use when:
  coordination/currentness/full internal relationship state actually matters

inputStates
  coherent retained InputState
  distinct-by-input is built into VexCompoundRelay

  use when:
  a downstream coordinator/diagnostic/presentation surface genuinely needs dependency/currentness
  context itself

outputStates
  derived OutputState only
  distinct-by-output is built into VexCompoundRelay

  use when:
  UI/downstream behavior depends only on the semantic result
~~~

And matching current snapshots:

~~~text
current
currentInput
currentOutput
~~~

Permanent distinction:

~~~text
COMPOUND_INTERNAL_STATE_CHANGED
!=
CONSUMER_OUTPUT_CHANGED
~~~

A caller must **not** be required to remember this pattern:

~~~kotlin
relay.states
    .map { it.outputState }
    .distinctUntilChanged()
~~~

That would encode a Relay invariant as consumer folklore and eventually create avoidable
recomposition/flicker bugs. `VexCompoundRelay.outputStates` owns that projection once.

`currentOutput` provides the corresponding defensively copied current snapshot for initial UI
state.

The full `states` stream remains available deliberately; it is not deprecated or hidden. Some
coordination owners genuinely need retained-input/currentness changes even when derived output is
equal.

### "Input Relay" is a role, not a second Relay class

Any `VexObservableRelay<State>` can be an input source to another Relay:

~~~text
ScreenContextRelay.states
ExternalFeedRelay.states
AttentionPreferenceRelay.states
        \      |      /
         \     |     /
          -> Compound InputState
             -> processStates(...)
             -> OutputState
~~~

The Compound `InputState` is the coherent retained snapshot/envelope of those dependencies. It is
not itself required to be a special Relay subtype.

If presentation genuinely needs current dependency context, consume `inputStates/currentInput`.
If presentation only needs the post-calculation semantic result, consume
`outputStates/currentOutput`. Do not re-run Compound business logic in Compose merely because the
inputs are available.

## EAP-00 owns

- explicit transitive defensive-copy contract;
- one serialized processing boundary;
- source-level semantic freshness in the smallest rightful Relay;
- Compound Relay retained input + derived output;
- nullable top-level input patches as "no new dependency value supplied";
- sibling patch serialization so concurrent inputs cannot erase one another;
- semantic no-op suppression;
- Compound revision only when retained/derived meaning changes;
- canonical distinct-by-output projection through `outputStates`;
- defensively copied `currentOutput` for downstream initialization.

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

Those are not omissions to paper over. They belong to narrower app/presentation/proof owners. The
Events practicum has now earned several of them without moving their behavior into this module.

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

## Current consumer boundary

The current Events consumer composes the kernel like this:

~~~text
provider-specific source truth
-> normalized EventsRepository feed
-> EventsAttentionRelay.processStates(...)
-> candidate identity + availability
-> anchored presentation
~~~

The module still does **not** own provider lifecycle, repository history, Compose, tooltip geometry,
or Journey execution.

A reusable async dependency-binding/activation layer remains unearned. The current Events feature
binds its concrete dependencies explicitly. Add a generic activation abstraction only after another
real consumer demonstrates recurring duplicated lifecycle mechanics that cannot be kept local.

<!-- [VXG RealForever][VEXLIFE-ANDROID][EVENTS-ATTENTION-PRACTICUM][EAP-00][RELAY] -->
