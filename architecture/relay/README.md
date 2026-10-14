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
alert availability, anchored overlay rendering, deterministic Journey signals, and ScreenCrawler
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
alert preference -----/
~~~

The concrete Relay should be boring to inspect:

1. read `dependencies()` to see what continuously changes around it;
2. read its public intention/surface methods to see what callers may explicitly ask;
3. read `processState / processStates` to see the reconciliation law;
4. read its output state to see what consumers are allowed to depend on.

### Three bounded Relay authoring shapes

The foundation now has three intentionally different paths. Choosing the path answers the developer's
first question before any feature logic is written.

~~~text
VexRelay<State>
  source/current State arrives
  -> send(inputState)
  -> processState(inputState, currentState)
  -> next State

VexActionRelay<Action, State>
  named semantic Action arrives
  -> sendAction(action)
  -> nextStateFor(action, currentState)
  -> next State

VexCompoundRelay<InputState, OutputState>
  several independently changing truths converge
  -> send(partialInputState)
  -> processStates(inputState, currentCompoundState)
  -> coherent InputState + derived OutputState
~~~

The difference is semantic, not threading:

- **VexRelay** answers "how does this incoming State-shaped observation reconcile with current truth?";
- **VexActionRelay** answers "given this named intention and exact current truth, what is next?";
- **VexCompoundRelay** answers "what do these several current truths mean together?".

### VexRelay — state-shaped source input

Use when an outside/current source value arrives and source/current reconciliation matters:

~~~kotlin
fun sourceSnapshotArrived(
    snapshot: SourceSnapshot,
): State =
    send(
        State(
            sourceSnapshots =
                mapOf(
                    snapshot.sourceRef to snapshot,
                ),
        ),
    )

override fun processState(
    inputState: State,
    currentState: State,
): State {
    // One visible input State -> current State -> next State law.
    return ...
}
~~~

Path:

~~~text
source
-> send(inputState)
-> processState(inputState, currentState)
-> next State
~~~

### VexActionRelay — named intention input

Use when the caller should name **what happened/requested**, not manufacture a next State or receive
an anonymous current-state lambda.

~~~kotlin
sealed interface PreferenceAction :
    VexAction {
    data class Acknowledge(
        val eventRef: String,
    ) : PreferenceAction

    data object ResetAll :
        PreferenceAction
}

class PreferenceRelay :
    VexActionRelay<
        PreferenceAction,
        PreferenceState
    >(...) {

    fun acknowledge(
        eventRef: String,
    ) {
        sendAction(
            PreferenceAction.Acknowledge(
                eventRef = eventRef,
            ),
        )
    }

    override fun nextStateFor(
        action: PreferenceAction,
        currentState: PreferenceState,
    ): PreferenceState =
        when (action) {
            is PreferenceAction.Acknowledge ->
                currentState.copy(
                    acknowledgedRefs =
                        currentState.acknowledgedRefs +
                            action.eventRef,
                )

            PreferenceAction.ResetAll ->
                PreferenceState()
        }
}
~~~

Path:

~~~text
named action
-> sendAction(action)
-> nextStateFor(action, exact current State)
-> next State
~~~

There is no feature-authored `current -> next` transition lambda. If a new behavior is added, add one
action and one exhaustive `when` branch. That makes the action family itself an inspectable map of
every allowed state-changing path.

Continuity rule:

~~~text
If current State participates in deciding next State,
the decision belongs inside nextStateFor(action, currentState).
~~~

Do not read `relay.current`, derive something elsewhere, and later send a separate update. That would
split one semantic transition into a read/derive/write race.

For complex calculation, keep the transition visible and extract only the pure domain calculation:

~~~kotlin
override fun nextStateFor(
    action: LabAction,
    currentState: LabState,
): LabState =
    when (action) {
        is LabAction.PlanNext -> {
            val alertCase =
                buildAlertCase(
                    mode = currentState.mode,
                    ordinal = currentState.nextOrdinal,
                    events = action.events,
                )

            currentState.copy(
                nextOrdinal =
                    currentState.nextOrdinal + 1L,
                currentAlertCase = alertCase,
            )
        }
    }
~~~

`buildAlertCase(...)` owns pure calculation. `nextStateFor(...)` owns the one allowed action-to-state
transition. `VexActionRelay` owns serialization, snapshots and commit.

Current real examples:

~~~text
EventsAlertPreferenceRelay
  named disposition action
  -> one exhaustive nextStateFor(...)

EventsDebugMenuAlertRelay
  UseMode / PrepareNextAlertCase / CompleteCurrentAlertCase / Reset
  -> one exhaustive nextStateFor(...)
  -> pure buildEventsDebugMenuAlertCase(...) only where calculation is complex
~~~

### VexCompoundRelay — several current truths

The plural in `processStates` does not mean "more threads." It means the Compound reducer reconciles
a family of retained inputs into a separate derived output.
### Copy-paste reducer blueprint

For new Compound Relays, use this order unless the domain genuinely needs something different:

~~~text
1. REFERENCES
   name previous input/output needed for reasoning

2. RECONCILE
   merge the partial patch into one coherent input snapshot

3. DERIVE
   calculate domain meaning from coherent input only

4. PROJECT
   construct exactly the output consumers should observe

5. RETURN
   return currentState.copy(inputState = coherentInput, outputState = output)
~~~

Ownership rule:

~~~text
Reducer author owns meaning.
Relay framework owns ownership snapshots.
~~~

Do **not** add `deepCopy()` inside `processStates()` merely because a value is being returned to
Relay. `send()` already snapshots caller input before reducer execution, isolates retained current
from reducer code, snapshots only changed candidate branches before retention, and snapshots exposed
results for callers/collectors.

Use a local copy inside the reducer only when the domain algorithm itself needs independent mutable
working memory, not as Relay ceremony.

The executable reference is:

~~~text
architecture/relay/src/test/.../VexCompoundRelayBlueprintTest.kt
~~~

That fixture deliberately uses mutable-backed provider/list/set state while keeping **zero
`deepCopy()` calls inside `processStates()`**. Its test proves caller mutation and consumer mutation
still cannot reach retained Relay truth. This makes it the copy-paste authoring blueprint; Events
remains the richer product-semantic example.

### Declarative Compound dependencies

A Compound Relay may declare continuously observed Flows:

~~~kotlin
override fun dependencies() =
    vexDependencies<ExampleInputState> {
        observe(
            dependencyRef = "feed",
            flow = repository.states,
        ) { feed ->
            ExampleInputState(
                feed = feed,
            )
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
    alertPreferences = null,
    debugMenuAlert = null,
    profile = null,
    visibleEventRefsBySurface = null,
    transientBlocked = null,
)
~~~

The scaffold is an IDE authoring aid, not the intended final patch shape. Delete the fields this
particular patch does not supply:

~~~kotlin
EventsConvergenceInputState(
    feed = feed,
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

Immutability is the default `VexState` contract; there is no separate immutable marker to choose.
A normal state therefore inherits `deepCopy() = this`.

If a state intentionally contains a `MutableList`, `MutableMap`, array, mutable SDK object, or
another externally mutable value, that state is an explicit exception and must override
`deepCopy()` to detach those descendants. Prefer adapting provider-owned mutable objects before they
enter Relay rather than making mutable-backed state common.

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

Normal authoring therefore stays simple:

~~~kotlin
data class ExampleInput(
    val screenId: String? = null,
    val selectedRef: String? = null,
) : VexInputState<ExampleInput>

data class ExampleOutput(
    val ready: Boolean = false,
) : VexOutputState<ExampleOutput>
~~~

No immutable marker and no boilerplate `deepCopy()` are required.

A mutable-backed exception makes the exception visible in the type itself:

~~~kotlin
data class MutableBackedInput(
    val refs: MutableList<String>,
) : VexInputState<MutableBackedInput> {
    override fun deepCopy(): MutableBackedInput =
        copy(
            refs = refs.toMutableList(),
        )
}
~~~

### Serialization, waiting, and staleness

`synchronized(lock)` and the `processing` flag solve different problems.

~~~text
concurrent caller A enters send()
-> owns lock
-> processes one transition

concurrent caller B calls send()
-> waits for the lock
-> enters after A commits/releases
-> reconciles its patch against A's newly retained current truth
~~~

So Relay does not intentionally drop a sibling patch merely because another transition is already
processing. There is no explicit Relay event queue, however: JVM monitor waiting is not a FIFO/fair
ordering contract, and StateFlow remains a current-state projection rather than an event ledger.

The `processing` flag exists because `synchronized` monitors are re-entrant on the same thread.
Without the flag, `processStates()` could recursively call `send()` and begin a nested transition
while the first reducer is unfinished. That recursive semantic re-entry fails closed.

This gives Relay **local serialization**, not universal source freshness. If an older provider result
arrives after a newer result, the lock cannot know which one is semantically stale. The rightful
source must carry/order that meaning when needed, for example:

~~~text
sourceRevision
attemptId
requestGeneration
sourceEpoch
~~~

Relay then serializes those facts; the source/reducer decides whether an admitted fact is still
authoritative.

### One transition, step by step

The copy sites in Relay are intentionally named ownership boundaries, not repeated attempts to clone
the same thing. Read one Compound transition like this:

~~~text
0. RETAINED
   Relay owns current canonical truth.

1. ADMIT
   caller patch -> deepCopy() -> admittedPatch
   Why: caller may still own/mutate what it passed.

2. ISOLATE FOR REDUCER
   retainedCurrent -> deepCopy() -> processingCurrent
   Why: processStates() is overridable code and must never mutate retained canonical truth.

3. REDUCE
   admittedPatch + processingCurrent -> processStates() -> reducerCandidate
   Why: candidate is temporary proposed meaning, not another truth owner.

4. COMPARE
   reducerCandidate.input/output vs retainedCurrent.input/output
   Why: a temporary no-op candidate should be discarded before any ownership copy.

5A. NO CHANGE
   discard reducerCandidate
   retain retainedCurrent exactly
   retainedCurrent -> deepCopy() -> exposedCurrent
   Why: send() returns outside Relay ownership, so mutable-backed current cannot be handed out.

5B. CHANGE / CLAIM
   deepCopy() only the changed candidate branch(es)
   preserve unchanged retained branch(es) by identity
   Why: only values entering canonical Relay truth need a new ownership snapshot.

6. BUILD
   changed owned branch(es)
   + unchanged retained branch(es)
   + revision + 1
   -> nextRetained

7. COMMIT
   mutableCurrentState.value = nextRetained
   This is the only canonical truth move.

8. EXPOSE
   nextRetained -> deepCopy() -> exposedNext
   Why: caller of send() must not be able to mutate retained truth.
~~~

For a transitively immutable Vex state, every `deepCopy()` above returns the same instance. The
protocol still documents the ownership boundary, but it performs no allocation.

For a mutable-backed state, the copies at ADMIT, ISOLATE, changed-branch CLAIM, and EXPOSE are
different copies for different owners. A no-op candidate is now compared and discarded before any
CLAIM copy happens. In particular, the processing copy and the no-change exposure copy are not
interchangeable: `processingCurrent` was handed to reducer code, while `exposedCurrent` is handed to
the caller of `send()`.

#### Copy budget: main vs this PR

The source can look copy-heavier because formerly chained calls are now named, but the framework
transition performs fewer ownership snapshots.

~~~text
VexCompoundRelay.send()

semantic NO-OP
  main: 5 top-level deepCopy() calls
  PR77: 3 top-level deepCopy() calls

semantic CHANGE
  main: 8 top-level deepCopy() calls
  PR77: 2 fixed snapshots
        + 1 snapshot per changed input/output branch
        + 1 outward exposure snapshot

Examples:
  only input changed  -> 4 top-level/branch snapshots
  only output changed -> 4 top-level/branch snapshots
  both changed        -> 5 top-level/branch snapshots
~~~

Those counts describe framework call sites, not necessarily allocations. For an ordinary
transitively immutable `VexState`, every inherited snapshot returns the same instance, so the
immutable path allocates only the genuinely new state objects created by the reducer/wrapper.

Current Events state explicitly overrides `deepCopy()` because its Lists/Maps/domain children are
still treated as mutable-backed ownership boundaries. It benefits immediately from the removed
whole-state recopying and unchanged-branch retention. A future Events cleanup may remove those
overrides only after the nested values are proven transitively immutable.

#### Events-shaped example

Suppose retained Events input is:

~~~kotlin
val retainedInput = EventsConvergenceInputState(
    currentScreenId = "Home",
    feed = feed41,
    alertPreferences = alertPreferences12,
    debugMenuAlert = debugMenuAlert3,
    profile = profile8,
    visibleEventRefsBySurface = visible5,
    transientBlocked = false,
)
~~~

One caller admits two related changes together:

~~~kotlin
val patch = EventsConvergenceInputState(
    currentScreenId = "Events",
    transientBlocked = true,
)
~~~

The reducer forms one coherent candidate:

~~~kotlin
val coherent =
    retainedInput.copy(
        currentScreenId = patch.currentScreenId ?: retainedInput.currentScreenId,
        transientBlocked = patch.transientBlocked ?: retainedInput.transientBlocked,
    )
~~~

With immutable children the new root may structurally share:

~~~text
coherent.feed                       === retainedInput.feed
coherent.alertPreferences           === retainedInput.alertPreferences
coherent.debugMenuAlert             === retainedInput.debugMenuAlert
coherent.profile                    === retainedInput.profile
coherent.visibleEventRefsBySurface  === retainedInput.visibleEventRefsBySurface

coherent.currentScreenId             != retainedInput.currentScreenId
coherent.transientBlocked            != retainedInput.transientBlocked
~~~

So one admitted patch can change several facts atomically without cloning every unchanged branch.
The Compound reducer still runs once and the framework still commits at most one next state.

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
-> alert candidate = none

Events becomes current
-> same retained event becomes eligible

acknowledge first event
-> event remains in truth
-> next unacknowledged event becomes candidate

screen/feed/alert-preference arrive concurrently
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
