# Vex Relay — processing kernel

[VXG RealForever]

## The question

> External information can arrive while the human is somewhere else in the app. How do we keep the
> source truth reliable, combine only the current dependencies that matter, and make the processing
> logic easy to find without turning Compose or a overlay manager into the state owner?

This module is the **current-main extraction** of the smallest Relay processing contracts earned by
the Events Convergence Practicum.

Historical draft PR #3 remains architecture quarry. EAP-00 did not merge/cherry-pick that branch
wholesale and did not import its experimental AppScope/observability stack.

The current concrete consumer is the Events Convergence Practicum under
`docs/foundation/EVENTS-CONVERGENCE-PRACTICUM.md`. That feature now exercises this kernel with a
normalized external feed, stable dynamic list identity, acknowledgement/defer/suppress state,
attention availability, anchored overlay rendering, deterministic Journey signals, and ScreenCrawler
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
external feed ------+-> EventsConvergenceRelay.processStates(...) -> current Events projection
ack/preference -----/
~~~

The concrete Relay should be boring to inspect:

1. read `dependencies()` to see what continuously changes around it;
2. read its public intention/surface methods to see what callers may explicitly ask;
3. read `processState / processStates` to see the reconciliation law;
4. read its output state to see what consumers are allowed to depend on.

### Declarative Compound dependencies

A Compound Relay may declare continuously observed Flows:

~~~kotlin
override fun dependencies() =
    vexDependencies<ExampleInputState> {
        observe(
            dependencyRef = "feed",
            flow = repository.states,
        ) { feed ->
            ExampleInputState(feed = feed)
        }
    }
~~~

`activate(scope)` starts exactly one named child coroutine per declared dependency. Feature authors
therefore do not write launch/collect/send plumbing and cannot accidentally place two never-ending
`collect` calls sequentially in one coroutine.

The boundary is intentional:

~~~text
dependency observation
  may suspend while waiting for outside truth

processStates()
  non-suspending atomic in-memory reducer
  one admitted patch at a time
~~~

`suspend` is not used as an ordering substitute. Concurrent dependency arrivals are serialized by
the Relay; semantic freshness/order still belongs to each rightful source (for example a provider
revision). Blocking IO or SDK waits do not belong inside `processStates()`.

### IDE authoring perk — reveal optional InputState fields

Compound InputState fields intentionally default to `null` when `null` means "this partial patch
does not supply a new value". That keeps ordinary patches small, but a fresh developer may want to
see every available named field while authoring one.

This repository carries one shared Android Studio / IntelliJ required-plugin hint:

~~~text
.idea/externalDependencies.xml
-> Kotlin Named Argument Stubs
-> plugin id ru.alezhu.idea.plugins.named_argument_stubs
~~~

On a fresh clone, JetBrains IDEs read that file and notify the developer when the plugin is missing
or disabled. The plugin can generate named-argument stubs for Kotlin function and constructor calls,
so a call such as:

~~~kotlin
EventsConvergenceInputState()
~~~

can be expanded into an editable discovery scaffold resembling:

~~~kotlin
EventsConvergenceInputState(
    currentScreenId = null,
    feed = null,
    preferences = null,
    lab = null,
    profile = null,
    visibleEventRefsBySurface = null,
    transientBlocked = null,
)
~~~

The scaffold is an IDE authoring aid, not the intended final patch shape. Delete the fields this
particular patch does not supply:

~~~kotlin
EventsConvergenceInputState(
    feed = feed.deepCopy(),
)
~~~

Android Studio's built-in intention to specify remaining arguments by name may also be available via
`Alt+Enter`. The repository plugin hint exists because that built-in intention is not consistently
offered for every optional-argument/K2 editing case.

Only `.idea/externalDependencies.xml` is admitted to Git. Other `.idea` workspace state remains
ignored.

### Immutable ownership and structural sharing

Relay still owns a defensive boundary, but "defensive" does not require allocating a new object when
the value is transitively immutable.

~~~text
mutable / provider-owned value
-> deepCopy() detaches at the ownership boundary

transitively immutable Vex value
-> deepCopy() may return the same instance
~~~

Use `VexImmutableState`, `VexImmutableInputState`, or `VexImmutableOutputState` only when every value
reachable from the state is immutable for that state's lifetime. A state containing a
`MutableList`, `MutableMap`, array, mutable SDK object, or another externally mutable value must keep
a real detaching `deepCopy()` implementation.

This lets one admitted Compound patch change several facts atomically without recursively copying
unchanged state:

~~~kotlin
val coherent =
    previous.copy(
        currentScreenId =
            patch.currentScreenId
                ?: previous.currentScreenId,
        transientBlocked =
            patch.transientBlocked
                ?: previous.transientBlocked,
    )
~~~

The new root is different, but unchanged immutable children remain the same objects. `processStates`
still returns one temporary candidate; Relay compares that candidate with retained current meaning
and commits at most one next state. There is no separately retained `processingState`.

`VexCompoundRelay` also preserves the exact retained input/output branch when that branch is
semantically equal. An input-only revision therefore does not manufacture a replacement output
object.

### Nested Compound convergence

A Compound Relay may consume another Compound Relay exactly like any other observable truth. Prefer
the smallest projection that answers the parent's question:

~~~text
child.outputStates
  default when parent needs only the child's derived meaning

child.inputStates
  only when parent genuinely needs the child's retained dependency context

child.states
  only when full child coordination/revision state matters
~~~

For the normal case:

~~~text
A ----\
B -----+-> Child.processStates() -> ChildOutput --\
C ----/                                           +-> Parent.processStates()
D ------------------------------------------------/
~~~

`outputStates` is distinct-by-output. Therefore:

~~~text
child input/revision changes
+ child output meaning stays equal
-> parent does not process

child output meaning changes
-> parent receives one changed semantic parameter
-> parent processes once
~~~

When the child output is transitively immutable, the exact output object may be structurally shared
into the parent instead of recopied. This keeps convergence hierarchical without turning nested
Relays into a chain of defensive allocations.

Third-party SDK/provider objects remain outside this trusted immutable graph. Normalize/snapshot
provider-owned mutable objects into Vex-owned state at the adapter boundary first.

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

`currentOutput` provides the corresponding ownership-safe current snapshot for initial UI state.
Mutable-backed outputs are detached; transitively immutable outputs may be shared.

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

- explicit ownership-snapshot contract with immutable structural-sharing opt-in;
- one serialized processing boundary;
- source-level semantic freshness in the smallest rightful Relay;
- Compound Relay retained input + derived output;
- nullable top-level input patches as "no new dependency value supplied";
- sibling patch serialization so concurrent inputs cannot erase one another;
- one admitted patch may change several fields atomically while unchanged immutable children are shared;
- unchanged Compound input/output branches retain their already-owned object identity;
- nested Compound consumers can observe the smallest distinct projection without re-running child logic;
- semantic no-op suppression;
- Compound revision only when retained/derived meaning changes;
- canonical distinct-by-output projection through `outputStates`;
- ownership-safe `currentOutput` for downstream initialization.

## EAP-00 deliberately does not own

- external SDK/service adapters;
- scheduled synthetic source lifecycle;
- generic application-wide coroutine/job supervision or startup/readiness beyond the bounded
  dependency-observation owner described above;
- persistent event history;
- overlay/snackbar/dialog rendering;
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
-> EventsConvergenceRelay.processStates(...)
-> candidate identity + availability
-> anchored presentation
~~~

The module still does **not** own provider lifecycle, repository history, Compose, overlay geometry,
or Journey execution.

Events now earns the smallest reusable dependency-binding layer: named Flow dependencies are
declared by the Compound Relay and activated under a supplied owner scope. This does not turn Relay
into a scheduler, operation system, service host, or readiness framework. More execution machinery
still requires a real consumer.

<!-- [VXG RealForever][VEXLIFE-ANDROID][EVENTS-CONVERGENCE-PRACTICUM][EAP-00][RELAY] -->
