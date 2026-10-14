# Vex Behavior Blueprint — cause / effect / cause architecture experiment

[VXG RealForever]

## Question

Can behavioral source code preserve the **geometry of what a human can do from what is true now**,
the same way ScreenBlueprint preserves the geometry of a screen?

Current UI precedent:

~~~text
ScreenBlueprint
    source nesting
    ~= spatial containment
~~~

This experiment asks for the behavioral twin:

~~~text
VexBehaviorBlueprint
    source State neighborhoods
    ~= behavioral possibility from current truth

VexBehaviorFamily
    compile-time kinship only
    ~= these State / Control / Act / Effect alphabets belong together
~~~

The goal is not a prettier state machine. The goal is to make the source itself explain:

~~~text
What is true now?
What Acts have meaning from here?
What Effect does each Act request?
What State should that Effect lead toward?
What becomes possible from that next State?
~~~

## Comparison point

PR #81 is the predecessor/comparison experiment:

~~~text
VexArchitecture V2
    explicit Architecture
    Case
    Transition
    Control
    Placement
    derived Map
~~~

This branch starts again from accepted main rather than inheriting PR #81 source.

The new hypothesis is that some of those nouns are better **derived representations** than authoring
concepts.

## The four-move grammar

The current experiment deliberately reduces the authoring language to four semantic moves:

~~~text
STATE
    what is actually true now

CONTROL
    which human-facing affordance may express one or more Acts

ACT
    the human/product verb intended from current State

EFFECT
    the runtime verb / attempted transformation
~~~

The causal loop is:

~~~text
observed STATE
    -> CONTROL resolves an ACT
    -> ACT selects an EFFECT
    -> rightful runtime/provider attempts that EFFECT
    -> reality is observed again as STATE
    -> repeat
~~~

A compact reading is:

~~~text
Act asks.
Effect attempts the travel.
State tells us what reality answered.
Control decides which Act one affordance represents from current truth.
~~~

### State is truth, not a requested outcome

An Effect never authoritatively changes State merely because it was selected.

~~~text
State = Disconnected
Act = Connect
Effect = BeginConnection

possible observed States afterward:
    Connecting
    Connected
    Failed
    Disconnected
~~~

If an in-progress condition changes what can happen next, that condition itself is State:

~~~text
Disconnected
    -> Connect / BeginConnection
    -> Connecting

Connecting
    -> Cancel / EndConnection
    -> Disconnected
~~~

Convergence consumes these current truths. It must not treat an Effect request as if the requested
outcome already became true.

### State kind versus State flavor

Do not create another State type merely because one field changes.

Use a distinct State kind when the difference changes the semantic neighborhood: what Acts have
meaning, what relationships exist, or what current truth fundamentally means.

Use data/flavor inside the same State when the same kind of truth can carry the difference.

Playback now demonstrates:

~~~kotlin
sealed interface PlaybackState {
    data object Paused : PlaybackState

    data class Playing(
        val speed: PlaybackSpeed,
    ) : PlaybackState
}
~~~

So these are flavor variants of one Playing truth:

~~~text
Playing(speed = Normal)
Playing(speed = FastForward)
Playing(speed = Rewind)
~~~

The corresponding Effects are attempted transformations:

~~~text
SetSpeed(Normal)
SetSpeed(FastForward)
SetSpeed(Rewind)
PausePlayback
~~~

This avoids multiplying architecture nouns such as FastForwardingState / RewindingState when the
difference can truthfully live as data.

### Hidden history is not current truth

A relationship resolves from current State.

If previous history materially changes what may happen next, preserve only the relevant consequence
as current State data. Do not make a relationship secretly inspect "what Act happened before".

## Cause -> effect -> cause

The core relationship is:

~~~text
CURRENT STATE
    + ACT
        -> EFFECT
            -> NEXT STATE
                -> becomes the causal context for the next ACT
~~~

The architecture resolves from **current truth**, not hidden traversal history and not a destination
it hopes to produce.

Past behavior matters only when its consequence is still present in current State.

## Worked Playback example

The compiled Playback practicum now uses State flavor instead of inventing separate State kinds for
every transport speed:

~~~text
PlaybackState
    Paused
    Playing(speed)

PlaybackSpeed
    Rewind
    Normal
    FastForward
~~~

The relational source reads:

~~~kotlin
state<PlaybackState.Paused> {
    on(
        PlaybackAct.Play,
        PlaybackEffect.SetSpeed(
            PlaybackSpeed.Normal,
        ),
    ).to<PlaybackState.Playing>()

    on(
        PlaybackAct.FastForward,
        PlaybackEffect.SetSpeed(
            PlaybackSpeed.FastForward,
        ),
    ).to<PlaybackState.Playing>()
}

state<PlaybackState.Playing> {
    on(
        act = PlaybackAct.Play,
        effect =
            PlaybackEffect.SetSpeed(
                PlaybackSpeed.Normal,
            ),
        conditionName = "not-normal-speed",
    ) { current ->
        current.speed != PlaybackSpeed.Normal
    }.stay()

    on(
        PlaybackAct.Pause,
        PlaybackEffect.PausePlayback,
    ).to<PlaybackState.Paused>()

    on(
        act = PlaybackAct.FastForward,
        effect =
            PlaybackEffect.SetSpeed(
                PlaybackSpeed.FastForward,
            ),
        conditionName = "not-fast-forward-speed",
    ) { current ->
        current.speed != PlaybackSpeed.FastForward
    }.stay()
}
~~~

So the earlier question becomes:

~~~text
STATE
    Playing(speed = Normal)

ACT
    FastForward

EFFECT
    SetSpeed(FastForward)

expected State kind
    Playing

then runtime reports:
    Playing(speed = FastForward)

next ACT
    Play

next EFFECT
    SetSpeed(Normal)

then runtime reports:
    Playing(speed = Normal)
~~~

The Effect is the attempted travel; the data-bearing State is the observed answer.

## State / Act / Effect

The prototype deliberately keeps semantic families separate and compiler-bounded:

~~~text
State
    what is semantically true now
    sealed feature-owned family
    may carry data

Act
    what the human/control means to do from current State
    sealed feature-owned family
    may later carry request data

Effect
    provider-facing meaning that runtime should attempt
    sealed feature-owned family
    may later carry provider parameters

Control
    finite identity-only affordance vocabulary
    enum in this experiment so every member can be exhaustively linked
~~~

Example:

~~~text
State
    Playing(speed = FastForward)

Act
    Play

Effect
    SetSpeed(Normal)

expected destination State kind
    Playing
~~~

Act and Effect are separate because UI/product vocabulary should not become coupled to one provider
implementation. State flavor can change without inventing another State type.

## Family-bound grammar

The four semantic roles are now locked together by one compile-time family identity.

~~~kotlin
interface VexBehaviorFamily<
    Self : VexBehaviorFamily<Self>,
> {
    interface State<
        Family : VexBehaviorFamily<Family>,
    >

    interface Control<
        Family : VexBehaviorFamily<Family>,
    >

    interface Act<
        Family : VexBehaviorFamily<Family>,
    >

    interface Effect<
        Family : VexBehaviorFamily<Family>,
    >
}
~~~

A feature must form a concrete self-bound family:

~~~kotlin
sealed interface VexStreamPlaybackFamily :
    VexBehaviorFamily<
        VexStreamPlaybackFamily
    >
~~~

Then each semantic alphabet explicitly belongs to that family:

~~~text
VexStreamPlaybackState
    : VexBehaviorFamily.State<VexStreamPlaybackFamily>

VexStreamPlaybackControl
    : VexBehaviorFamily.Control<VexStreamPlaybackFamily>

VexStreamPlaybackAct
    : VexBehaviorFamily.Act<VexStreamPlaybackFamily>

VexStreamPlaybackEffect
    : VexBehaviorFamily.Effect<VexStreamPlaybackFamily>
~~~

The recursive bound prevents generic code from collapsing the family coordinate back to an
unqualified base interface. The role bounds prevent putting a State in the Act slot. Carrying the
same Family parameter through Blueprint / ControlBlueprint / Projection / VexPort prevents a
correctly-role-typed value from another family from being silently mixed in.

~~~text
ROLE SAFETY
    PlaybackState cannot occupy Act / Control / Effect positions

FAMILY SAFETY
    QueueControl cannot be paired with PlaybackState / PlaybackAct
~~~

The family is a phantom compiler coordinate only. It is not a runtime owner, service, singleton,
state store, or replacement for the four-move grammar.

### Why VexPort is invariant

The two-way Port both produces a Projection and consumes Control through `activate(Control)`.

Do not make Control covariant with `out` and then bypass Kotlin using `@UnsafeVariance`. That
would disable the exact compiler protection this family contract exists to provide.

Generic downstream code can still remain swappable by being generic over the exact family:

~~~kotlin
fun <
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Act : VexBehaviorFamily.Act<Family>,
> consume(
    port: VexPort<
        Family,
        State,
        Control,
        Act
    >,
) {
    // ...
}
~~~

This accepts any correctly formed family while preserving its internal type relationships.

If a future convergence surface needs a heterogeneous collection of unrelated families, give that
specific read-only/erased boundary its own contract rather than weakening the two-way Port.

### Why Control still has two bounds in one place

`VexPort` only requires:

~~~text
Control : VexBehaviorFamily.Control<Family>
~~~

The finite Control Blueprint additionally requires:

~~~text
Control : Enum<Control>
~~~

because only that builder needs to enumerate the entire vocabulary and prove every Control member is
deliberately linked. The two bounds answer different questions and therefore belong only where both
are needed.

## Type families are architecture boundaries

The worked Playback example keeps finite meanings inside their family scope:

~~~text
PlaybackState.Playing(speed)
PlaybackState.Paused

PlaybackSpeed.Normal
PlaybackSpeed.FastForward
PlaybackSpeed.Rewind

PlaybackAct.Play
PlaybackAct.Pause
PlaybackAct.FastForward
PlaybackAct.Rewind

PlaybackEffect.SetSpeed(speed)
PlaybackEffect.PausePlayback

PlaybackControl.Rewind
PlaybackControl.PlayPause
PlaybackControl.FastForward
~~~

This is not cosmetic namespacing. It gives a human IDE-completion boundaries and gives the compiler
different types for different semantic families.

The intended pressure is:

~~~text
too much generic freedom
    -> accidental cross-domain combinations

bounded family + typed relationship
    -> illegal crossover becomes difficult or impossible to express
~~~

Controls use an enum in this prototype because they are finite identity-only possibilities and the
Control blueprint can therefore require every member to be deliberately linked. State / Act / Effect
remain sealed families because those concepts may legitimately carry data later.

## Inverse read — current State -> available Acts -> Controls

The authored graph can be read in both directions.

Forward:

~~~text
State + Act
    -> Effect
    -> expected next State
~~~

Inverse:

~~~text
current State
    -> currently available Acts
    -> Controls that can resolve one of those Acts
~~~

The reusable blueprint now derives `availableActs(current)` from the same State relationships.
Feature/UI code therefore does not need another `when(state)` table merely to decide whether Play,
Pause, Rewind, or FastForward currently has meaning.

Controls are not detached `val` objects. Playback declares a finite Control vocabulary:

~~~kotlin
enum class PlaybackControl {
    Rewind,
    PlayPause,
    FastForward,
}
~~~

and binds the entire vocabulary to one exact Playback Behavior instance:

~~~kotlin
behavior.controls<PlaybackControl> {
    control(
        PlaybackControl.Rewind,
        PlaybackAct.Rewind,
    )

    control(
        PlaybackControl.PlayPause,
        PlaybackAct.Play,
        PlaybackAct.Pause,
    )

    control(
        PlaybackControl.FastForward,
        PlaybackAct.FastForward,
    )
}
~~~

Construction rejects a missing enum Control, a duplicate Control, and any candidate Act that never
appears in the bound Behavior graph. Kotlin's generic `PlaybackAct` type also prevents a different
domain's Act family from being supplied without an explicit unsafe cast.

Argument order inside one Control is explicit conflict priority, not Behavior policy.

From `Playing`:

~~~text
available Acts:
    Pause
    Rewind
    FastForward

PlayPause Control candidates:
    Play > Pause

resolved Control meaning:
    Pause
~~~

From `Playing(speed = FastForward)`:

~~~text
available Acts:
    Play
    Pause
    Rewind

PlayPause Control candidates:
    Play > Pause

resolved Control meaning:
    Play
~~~

That lets a shared Play/Pause affordance reflect Play while fast-forwarding and Pause while playing
normally without embedding those State checks in Compose.

The Behavior graph still exposes both Play and Pause from the fast-forward flavor of Playing. The
Control owns the choice to prefer Play when both are meaningful. A different Control can
intentionally prefer Pause without changing Playback architecture.

A Control resolving to no Act means semantic unavailability only. Presentation still owns whether
that appears hidden, disabled, replaced, animated, or otherwise rendered.

## Derived connective tissue

Feature code authors each relationship once, in the State from which it has meaning.

The blueprint derives:

~~~text
outgoing relationships
incoming relationships
available Acts from current State
Act resolution
Control resolution
readable relationship graph
~~~

There is no separately authored Placement mirror and no separately maintained Map.

## Data-changing self relationships

Not every Act changes the State type.

A Queue can remain Positioned while its data changes:

~~~kotlin
state<Positioned> {
    on(
        act = Next,
        effect = Advance,
        conditionName = "has-next",
    ) { current ->
        current.hasNext
    }.stay()
}
~~~

This keeps discrete semantic modes separate from ordinary data variation.

## Presentation remains a derived boundary

The first real VexStream consumer exposed a second kind of duplication after Behavior migration:
both Mini Player and Context Dock still mapped the same semantic Act to the same icon.

That mapping now has one feature-local owner:

~~~text
BehaviorProjection
    Control -> Act / unavailable

feature presentation mapping
    Act / unavailable -> icon + enabled visual meaning

UIElement / Composable
    renders that presentation
~~~

This is deliberately **not** a fifth Behavior move. State / Control / Act / Effect remain the
semantic grammar. Presentation is a consumer projection.

The reusable Behavior module therefore does not know about ElementIcon, resources, Compose, labels,
animation, loading spinners, hidden/disabled treatment, or other visual language.

## Convergence boundary — do not cross-multiply domain States

The repository already has a real multi-source convergence consumer:
`EventsConvergenceRelay`.

Its current job is to combine independently owned facts such as feed truth, attention preferences,
current Screen, reachable event targets, and transient presentation occupancy into one current
Events projection.

That existing pressure teaches an important boundary for this experiment:

~~~text
Playback State
    -> Playback available Acts
    -> Playback Controls

Events convergence truth
    -> Events attention affordances

higher presentation/convergence owner
    -> may arbitrate between those projections
~~~

Do **not** encode combinations such as:

~~~text
PlayingAndIncomingEvent
PausedAndRelayUnavailable
PlayingFastForwardAndTransientOccupied
~~~

inside the Playback graph merely because presentation priority depends on several domains.

Likewise, do not append `.show()` / `.hide()` clauses to Behavior transitions as another authored
truth. The local graph should remain true even when another domain temporarily wins attention.

This PR intentionally stops before inventing a generic cross-domain convergence kernel. The current
Events consumer is evidence that convergence is real; one Playback Control projection is enough to
establish the seam without generalizing the arbitration policy prematurely.

## ScreenBlueprint relationship

The experiment intentionally does not merge behavior and UI yet.

First prove:

~~~text
ScreenBlueprint
    source structure mirrors spatial containment

VexBehaviorBlueprint
    source State neighborhoods mirror behavioral possibility
~~~

If both remain readable independently, a later practicum can test whether a Screen element/control
can resolve a Behavior Act without growing another feature switchboard.

That integration must be earned; it is not claimed by this first prototype.

## Construction / lifetime

Blueprints are normal constructed immutable classes:

~~~text
composition owner
    constructs blueprint

feature/provider/runtime
    owns current observed State and Effect realization

blueprint
    resolves lawful relational meaning
~~~

No global object is required.

## Held pressure

Not generalized yet:

~~~text
parameterized Acts / payloads
generic cross-domain Control arbitration
provider execution
async convergence / failure
authorization
ScreenBlueprint binding
persistence
cross-behavior composition
~~~

For example, "play queue index 7" carries request data. This experiment deliberately does not invent
a generic payload architecture before the relational grammar proves useful.

## Reusable VexPort

The common two-way feature boundary is named `VexPort`.

A Port is intentionally smaller than a client/service/controller concept. It names the reusable
semantic crossing point between a consumer and one isolated Behavior.

It deliberately exposes only:

~~~text
READ
    currentProjection

WRITE
    activate(Control)
~~~

The stronger optional contract `VexPortFlow` adds:

~~~text
projections: StateFlow<BehaviorProjection>
~~~

because a live Port publishes current truth, not a fire-and-forget event stream. The Flow suffix
therefore describes the observation form rather than creating another semantic owner.

This separates semantic contract from implementation position:

~~~text
VexPort
    reusable relationship

VexStreamPlaybackPort
    one feature/provider implementation of that relationship

ViewModel / Session / Service
    possible lifetime hosts for the concrete connection
~~~

A local feature that does not need an observable lifetime does not have to implement the StateFlow
contract. It may pass one immutable Projection and one Control callback directly.

### Activation is intentionally Control-shaped

The reusable write boundary is:

~~~kotlin
activate(control)
~~~

not:

~~~kotlin
activate(act)
execute(effect)
~~~

because the consumer is allowed to know what affordance was used, but must not freeze a previously
rendered Act or reach into provider execution.

The implementation must resolve:

~~~text
CURRENT State
    + Control
        -> CURRENT Act
            -> Effect
~~~

at activation time.

A test fixture deliberately proves this:

~~~text
rendered earlier:
    PlayPause -> Pause

reality changes:
    Playing(FastForward)

activate:
    PlayPause

re-resolved now:
    Play
    -> SetSpeed(Normal)
~~~

So the two-way Port is safe for delayed UI interaction as well as non-UI callers.

## Synchronous intent emission

A live Port may sit between immediate human input and a provider whose observable state arrives
later. The Port must not force UI latency merely to preserve provider authority.

The VexStream Media3 consumer now uses a Port-local shock absorber:

~~~text
current semantic State

activate(Control)
    -> resolve current Act
    -> resolve Effect

BEFORE provider dispatch
    -> record newest pending request
    -> synchronously publish pending semantic State

dispatch Effect to provider

provider observation arrives
    -> reconcile with pending request
    -> stale observation cannot erase newer local intent
    -> pending clears when provider acknowledges
~~~

This is not optimistic UI and it does not claim that the provider has already converged.

Example:

~~~text
provider observed:
    Media(playRequested=false, Buffering)

human:
    activate(PlayPause)

Port synchronously publishes:
    Media(
        playRequested=true,
        requestPending=true,
        Buffering
    )

then:
    Media3.play()

stale Media3 callback:
    playWhenReady=false

Port keeps:
    playRequested=true
    requestPending=true

later Media3 acknowledgement:
    playWhenReady=true

Port publishes:
    playRequested=true
    requestPending=false
~~~

The semantic truth is therefore two-part while latency exists:

~~~text
what this system has admitted/requested
!=
what the external provider has acknowledged so far
~~~

Both facts can coexist in State without turning Effect into fake outcome truth.

## Sharing one Behavior with one or many UI consumers

The semantic kernel now produces one immutable `VexBehaviorProjection`:

~~~text
Projection
    current State
    every finite Control
        -> resolved Act
        OR unavailable
~~~

Effects are deliberately absent from this value.

The Projection is safe to hand to:

~~~text
one Composable
many sibling Composables
preview fixtures
JVM tests
a later Convergence owner
~~~

without transferring provider/runtime ownership.

### UI reads Projection; UI sends Control

A UI should not send back the Act it happened to render.

State may change between composition and interaction.

Use:

~~~text
render:
    Projection says PlayPause -> Pause

time passes
State may change

click:
    send Control.PlayPause back to feature owner
    owner resolves Control again from CURRENT State
    -> current Act
    -> current Effect
~~~

This avoids stale rendered meaning becoming stale execution.

### ViewModel is a lifetime mechanism, not part of the behavior language

The four-move architecture does not require Android ViewModel.

A ViewModel is useful when Android configuration recreation should retain the feature client /
observation lifetime and there is no stronger owner already providing that lifetime.

Examples:

~~~text
small local Composable-only behavior
    immutable Projection parameter + callback may be enough
    no ViewModel required

feature owner already lives in a Service / repository / session
    that owner remains truth/lifetime owner
    UI may observe a projection directly or through a retained client
    ViewModel is optional glue, not semantic truth

Activity/session-scoped feature client
    ViewModel can retain the client across configuration recreation

several Composables on one surface
    nearest common owner may collect one observable Projection
    and pass the same immutable value downward
~~~

Multiple collectors of one hot StateFlow can also be valid when surfaces have independent lifetimes.
The semantic requirement is not "collect exactly once"; it is "do not recompute competing truth in
each consumer".

### Observable transport remains outside the kernel

This module intentionally does not prescribe `StateFlow`, Compose State, LiveData, or Relay as the
only transport.

A concrete feature may expose:

~~~kotlin
StateFlow<VexBehaviorProjection<State, Control, Act>>
~~~

because StateFlow is appropriate to its lifetime and platform.

Another feature may receive an immutable Projection from its parent.

A convergence feature may consume another owner's Flow.

Those are transport/lifetime decisions. The semantic payload remains the same
`VexBehaviorProjection`.

### Real VexStream Port

The target is now implemented for the first Play/Pause slice:

~~~text
MediaSession / Player
    owns actual playback truth

Media3VexStreamPlaybackConnection : VexPortFlow
    observes provider truth
    -> forms semantic State
    -> publishes BehaviorProjection
    -> accepts Control activation
    -> resolves latest Act / Effect
    -> emits pending intent synchronously
    -> asks MediaController to realize Effect
    -> reconciles delayed provider observations

Mini Player
Context Dock
    read Projection
    + activate(Control)
~~~

The concrete Port is retained by a ViewModel only because Android configuration lifetime currently
needs a host. ViewModel is not part of the VexPort contract or the four-move grammar.

## First production-shaped consumer — VexStream Play/Pause

The generic VexPort/projection contract now has one real Android consumer under:

~~~text
app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/
~~~

The first admitted slice is deliberately only Play/Pause transport meaning.

~~~text
State
    NoMedia
    Media(
        playRequested,
        phase = Idle | Buffering | Ready,
        playPauseAvailable,
    )
    Ended(restartAvailable)

Control
    PlayPause

Act
    Play
    Pause

Effect
    RequestPlay
    RequestPause
    RestartCurrent
~~~

Media3's `playWhenReady` is used as the requested-playback fact. `Player.isPlaying` is not used as
a Paused alias because Media3 only reports isPlaying=true when playback is READY, playWhenReady is
true, and playback is not suppressed.

That distinction means:

~~~text
Buffering + playRequested=true
    -> PlayPause resolves Pause

Buffering + playRequested=false
    -> PlayPause resolves Play
~~~

The old duplicated UI rule:

~~~text
if (playback.isPlaying) Pause else Play
~~~

and the old MediaController toggle helper are removed from the migrated surfaces.

### Production ownership

~~~text
VexStreamPlaybackService / ExoPlayer
    actual playback + queue truth/lifetime

Media3VexStreamPlaybackConnection
    observes Player facts
    -> semantic State
    -> BehaviorProjection

    activate(Control)
    -> latest State
    -> Act
    -> Effect
    -> MediaController command

VexStreamPlaybackConnectionViewModel
    Android configuration lifetime only

Mini Player
Context Dock
    consume Projection
    activate Control
~~~

The UI imports the reusable `VexPort` contract, not the Media3 implementation.

Queue, seeking, shuffle, repeat, media-item loading and progress rendering intentionally remain on
their existing routes. This is one vertical seam, not a claim that all VexStream playback work has
been migrated.

## Current VexStream integration seam

The repository's current VexStream implementation was inspected before wiring this experiment into
production UI.

Current ownership is:

~~~text
VexStreamPlaybackService
    owns MediaSession + ExoPlayer lifetime

VexStreamPlaybackViewModel
    retains one UI-side MediaController across Screen composition

rememberVexStreamPlaybackUiState(controller)
    converts MediaController/Player facts into Compose-local VexStreamPlaybackUiState

Mini Player
    reads playback.isPlaying
    chooses Play/Pause icon
    calls controller.toggleVexStreamPlayback()

Context Dock
    reads playback.isPlaying
    chooses Play/Pause icon
    calls controller.toggleVexStreamPlayback()
~~~

This exposes exactly the duplication the Behavior Blueprint is meant to remove, but it also exposes
a more important owner boundary.

Do **not** integrate the blueprint by simply placing it inside each Composable. That would make
behavior/current-State interpretation presentation-owned and would create multiple observers of the
same Media3 truth.

That seam is now exercised by `Media3VexStreamPlaybackConnection` for Play/Pause only. The
connection remains a projection/adapter and does not become another player or queue truth owner.

The existing code also reveals one unresolved semantic question that should be answered at this
boundary rather than hidden in Compose:

~~~text
Player.isPlaying == false
can mean more than "the human is Paused"
(for example preparing/buffering or another non-playing condition)
~~~

The current UI already collapses those meanings through `isPlaying`. The new architecture should
not silently canonize that coarse projection. The next design slice must decide which Media3 facts
form truthful VexStream Playback State before the two UI surfaces are migrated.

This is the present integration seam; queue, seek, shuffle, repeat, MediaSession lifetime, and source
resolution remain intentionally untouched.

## Acceptance threshold

This experiment earns deeper integration only if a reviewer can answer from source:

~~~text
What States can exist?
What can I do from this State?
What Effect does that Act mean here?
Where is that Effect expected to lead?
What becomes possible from the resulting State?
~~~

without reconstructing separate Placement / Map / singleton objects.

Current proof target:

~~~text
:architecture:behavior:test
-> vexVerifyJvm
~~~

No Android/device/product acceptance is implied by a JVM-only architecture prototype.

<!-- [VXG RealForever][VEX-BEHAVIOR-BLUEPRINT][CAUSE-EFFECT-CAUSE] -->
