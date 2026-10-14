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

The prototype deliberately keeps three meanings separate:

~~~text
State
    what is semantically true now
    preferably a sealed feature-owned family

Act
    what the human/control means to do from current State
    preferably a sealed feature-owned family

Effect
    provider-facing meaning that runtime should attempt
    preferably a sealed feature-owned family
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

## Control is a neighboring question

A physical Play/Pause button is not automatically the same thing as the semantic Act.

For example a UI control may choose:

~~~text
Paused
    Play/Pause button -> Act.Play

Playing
    Play/Pause button -> Act.Pause

FastForwarding
    Play/Pause button -> Act.Play

Rewinding
    Play/Pause button -> Act.Play
~~~

That contextual Control-to-Act mapping is deliberately not generalized in this first kernel. This
PR first proves the State -> Act -> Effect -> State relationship.

## Derived connective tissue

Feature code authors each relationship once, in the State from which it has meaning.

The blueprint derives:

~~~text
outgoing relationships
incoming relationships
Act resolution
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
physical Control -> Act mapping
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
