# Vex Behavior Blueprint — relational architecture experiment

[VXG RealForever]

## Question

Can behavioral source code preserve the **geometry of the human intention** the same way
ScreenBlueprint preserves the geometry of a screen?

Current UI precedent:

~~~text
ScreenBlueprint
    source nesting
    ~= spatial containment
~~~

This experiment asks for the behavioral twin:

~~~text
VexBehaviorBlueprint
    source relationships
    ~= behavior relationships
~~~

The goal is not a prettier state machine. The goal is to remove hand-authored connective tissue that
exists only because implementation objects were declared separately.

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

## Authoring example

Playback:

~~~kotlin
vexBehaviorBlueprint<
    PlaybackState,
    PlaybackIntention,
    PlaybackEffect
>(
    behaviorRef = "vexstream.playback",
) {
    state<NoMedia>()
    state<NotRequested>()
    state<Requested>()

    intention(PlayPause) {
        cycle<NotRequested, Requested>(
            forward = Play,
            backward = Pause,
        )
    }
}
~~~

A human can see in one neighborhood:

~~~text
PlayPause

NotRequested --Play--> Requested
Requested --Pause--> NotRequested
~~~

Play and Pause are not isolated vals that require a reader to hunt for the inverse relationship.

Queue-style data-changing behavior can remain one state kind:

~~~kotlin
intention(Next) {
    stay<Positioned>(
        effect = Next,
        conditionName = "has-next",
    ) { current ->
        current.hasNext
    }
}
~~~

## Type roles

The prototype deliberately uses ordinary feature types:

~~~text
State
    preferably a sealed feature-owned family
    gives IDE/compiler a bounded possibility space

Intention
    human request / UI-facing meaning
    preferably a sealed feature-owned family

Effect
    meaning that a provider/reducer/service may realize
    preferably a sealed feature-owned family

VexBehaviorBlueprint
    constructed immutable relationship graph
    NOT a singleton
    NOT the runtime state owner
    NOT the provider
~~~

The generic foundation does not invent Realm marker interfaces. The feature's own sealed State /
Intention / Effect types are the compiler coordinates.

## Derived connective tissue

The feature authors relationships once.

The blueprint derives:

~~~text
incoming relationships
outgoing relationships
intention resolution
readable/debug graph
~~~

There is no authored Placement mirror and no separately maintained Map.

## ScreenBlueprint relationship

The experiment intentionally does not merge behavior and UI yet.

First prove:

~~~text
ScreenBlueprint
    spatial source graph

BehaviorBlueprint
    relational source graph
~~~

If both remain readable independently, a later practicum can test whether a Screen element's
declared intention can address a BehaviorBlueprint intention directly without a second action
switchboard.

That later integration must be earned; it is not claimed by this first prototype.

## Construction / lifetime

Blueprints are normal constructed immutable classes:

~~~text
composition owner
    constructs blueprint

feature/provider/runtime
    owns current state/effects

blueprint
    resolves legal relationship meaning
~~~

No global object is required.

## Held pressure

Not generalized yet:

~~~text
parameterized intention payloads
provider execution
async convergence
authorization
ScreenBlueprint binding
persistence
cross-behavior composition
~~~

For example, "play queue index 7" carries request data. This experiment deliberately does not invent
a generic payload architecture before the finite-intention grammar proves useful.

## Acceptance threshold

This experiment earns deeper integration only if a reviewer can answer from source:

~~~text
What states can exist?
Which human intentions exist?
Which effects are related?
What cycles exist?
What can happen from this state?
What arrives at this state?
~~~

without reconstructing separate Placement / Map / singleton objects.

Current proof target:

~~~text
:architecture:behavior:test
-> vexVerifyJvm
~~~

No Android/device/product acceptance is implied by a JVM-only architecture prototype.

<!-- [VXG RealForever][VEX-BEHAVIOR-BLUEPRINT][RELATIONAL-ARCHITECTURE] -->
