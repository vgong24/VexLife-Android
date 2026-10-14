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

This is compiled test source, not pseudocode:

~~~kotlin
vexBehaviorBlueprint<
    PlaybackState,
    PlaybackAct,
    PlaybackEffect
>(
    behaviorRef = "example.playback.transport",
) {
    state<Playing> {
        on(Pause, PausePlayback)
            .to<Paused>()

        on(FastForward, StartFastForward)
            .to<FastForwarding>()

        on(Rewind, StartRewind)
            .to<Rewinding>()
    }

    state<Paused> {
        on(Play, PlayAtNormalSpeed)
            .to<Playing>()

        on(FastForward, StartFastForward)
            .to<FastForwarding>()

        on(Rewind, StartRewind)
            .to<Rewinding>()
    }

    state<FastForwarding> {
        on(Play, PlayAtNormalSpeed)
            .to<Playing>()

        on(Pause, PausePlayback)
            .to<Paused>()

        on(Rewind, StartRewind)
            .to<Rewinding>()
    }

    state<Rewinding> {
        on(Play, PlayAtNormalSpeed)
            .to<Playing>()

        on(Pause, PausePlayback)
            .to<Paused>()

        on(FastForward, StartFastForward)
            .to<FastForwarding>()
    }
}
~~~

Read one State block as a semantic neighborhood:

~~~text
I am FastForwarding.

From here:
    Play   -> attempt normal-speed playback -> Playing
    Pause  -> attempt pause                 -> Paused
    Rewind -> attempt rewind                -> Rewinding
~~~

That makes this sequence explicit:

~~~text
Playing
    -- FastForward / StartFastForward -->
FastForwarding
    -- Play / PlayAtNormalSpeed -->
Playing
~~~

Pressing Play while FastForwarding therefore means **resume normal-speed playback**, not "toggle the
old Playing state and pause".

The blueprint does not inspect a remembered previous Act to decide that. Once FastForwarding is the
observed current State, that State is sufficient causal context.

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
    FastForwarding

Act
    Play

Effect
    PlayAtNormalSpeed

expected destination State
    Playing
~~~

Act and Effect are separate because UI/product vocabulary should not become coupled to one provider
implementation.

## Type families are architecture boundaries

The worked Playback example keeps finite meanings inside their family scope:

~~~text
PlaybackState.Playing
PlaybackState.Paused
PlaybackState.FastForwarding
PlaybackState.Rewinding

PlaybackAct.Play
PlaybackAct.Pause
PlaybackAct.FastForward
PlaybackAct.Rewind

PlaybackEffect.PlayAtNormalSpeed
PlaybackEffect.PausePlayback
PlaybackEffect.StartFastForward
PlaybackEffect.StartRewind

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

From `FastForwarding`:

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
without embedding those State checks in Compose.

The Behavior graph still exposes both Play and Pause from FastForwarding. The Control owns the
choice to prefer Play when both are meaningful. A different Control can intentionally prefer Pause
without changing Playback architecture.

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
FastForwardingAndTransientOccupied
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
