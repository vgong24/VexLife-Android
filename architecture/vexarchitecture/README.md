# VexArchitecture — readable local relationships

`[VXG RealForever]`

## The question

A fresh developer should be able to start from a human question such as:

> What does Play/Pause mean right now, what can happen from here, and who performs it?

and walk one constrained path instead of reconstructing behavior from booleans and callbacks.

```text
observed current truth
-> ordinary domain Case
-> relationships available from that Case
-> semantic control/slot
-> named intention
-> one rightful effect owner
-> platform/service
-> newly observed truth
```

VexArchitecture owns the **relationship language** in that path. It does not own serialization,
platform execution, service lifetime, or current-state convergence.

## Core vocabulary

```text
Case
  ordinary domain condition/value

Transition<Before, After>
  one typed relationship: Before -> After

Placement<Current>
  relationships that arrive at or leave this exact Current Case

TransitionSlot
  one semantic control/location that may represent different transitions from different Cases

Current
  one Case bound to its exact Placement
```

The source should read like the human relationship, not like an implementation map.

## Authoring pattern

Prefer domain names that answer **when** the relationship is true:

```kotlin
val Play =
    vexTransition<ReadyToPlay, PlayRequested>(
        "playback.play",
    )

val Pause =
    vexTransition<PlayRequested, ReadyToPlay>(
        "playback.pause",
    )

val WhenReadyToPlay =
    vexPlacement<ReadyToPlay> {
        arrivedBy(Pause)
        can(Play)
    }

val WhenPlayIsRequested =
    vexPlacement<PlayRequested> {
        arrivedBy(Play)
        can(Pause)
    }

val PlayPauseControl =
    vexTransitionSlot(
        "playback.play-pause",
        Play,
        Pause,
    )
```

A developer can now read:

```text
ReadyToPlay
-> can Play
-> PlayRequested

PlayRequested
-> can Pause
-> ReadyToPlay
```

without first discovering a separate state-machine diagram.

## Current meaning is read-only

`VexCurrent.availableTransition(slot)` answers:

> Which transition in this semantic slot is meaningful from what is current?

It does **not** perform the transition.

```kotlin
val available =
    currentPlaybackControl.availableTransition(
        PlayPauseControl,
    )
```

Presentation may use that answer for icon/enabled state. Execution must continue to a named rightful
owner.

```text
UI observes transition
!=
UI owns transition execution
```

## First practicum — VexStream Play/Pause

The VexStream path deliberately reads in both directions.

### Observation / presentation

```text
Media3
-> VexStreamPlaybackObservation
     queueHasItems
     playRequested
-> VexStreamPlaybackArchitecture.currentControl(...)
-> QueueEmpty / ReadyToPlay / PlayRequested
-> available PlayPauseControl transition
-> VexStream Screen + Context Dock render the same meaning
```

### Human intention / execution

```text
human presses Play/Pause
-> VexStreamPlaybackControlAction.TogglePlayPause
-> VexStreamPlaybackControlOwner
-> owner re-reads exact Media3 truth
-> currentControl(...)
-> availableTransition(PlayPauseControl)
-> MediaController play() / pause()
-> Media3 reports new truth
```

The owner re-reads Media3 at execution time so a stale UI snapshot cannot become execution authority.

## Fresh developer walk

1. Start at the human-facing control.
2. Follow its `transitionSlot`.
3. Read the named Transition's `Before -> After` Cases.
4. Read the `When...` Placement to understand arrivals and legal exits.
5. For presentation, follow `availableTransition(...)`.
6. For execution, follow the named action to its one effect owner.
7. Follow platform realization only when the question becomes "how is this performed here?"
8. Follow current observation only when the question becomes "why are we in this Case now?"

## Boundaries

```text
Placement != global workflow manager
Transition != command execution
TransitionSlot != execution authority
Case != implementation phase
Current != durable event ledger
Compiler-valid relationship != exact live currentness
```

Relay remains the owner when exact currentness, serialized state transitions, dependency convergence,
or atomic commit are required. VexArchitecture describes lawful local relationships; it does not
replace Relay.

<!-- [VXG RealForever][VEX-ARCHITECTURE] -->
