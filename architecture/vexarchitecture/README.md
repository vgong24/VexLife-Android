# VexArchitecture — contract roots and local relational placement

`[VXG RealForever]`

## The developer question

A fresh developer should not have to learn the whole application before answering:

> What is true here, what can happen from here, and where do I go if I am lost?

VexArchitecture therefore uses inheritance for **bounded discoverability**, not as a universal base
class for every state/value in the application.

```text
VexArchitectureContract
  -> architecture roots only

VexStreamPlaybackArchitecture
  -> feature-local Case / Control / Placement / Android contracts

ordinary concrete meaning
  -> QueueEmpty / ReadyToPlay / PlayRequested
  -> Play / Pause / PlayPause
```

"Find implementations" on the global contract should return architecture roots, not every Case in
the institution.

## Reusable language

```text
*Architecture
  orientation root / feature realm / derived map / root feature flag

*Case
  what kind of truth is current

*Control
  what a consumer may request from current truth

*Placement
  how a Case can legitimately arrive and leave

Transition<Before, After>
  one typed doorway inside one architecture realm

*Android
  Android-specific observation and effect realization
```

The names should read like directory organization:

```text
VexStream / Playback / Architecture
VexStream / Playback / Case
VexStream / Playback / Control
VexStream / Playback / Placement
VexStream / Playback / Android
```

Feature words vary. Architecture words should carry the same expectation everywhere.

## Nodes first, map second

The Case is the node. Placements declare local relationships.

```kotlin
val Play =
    VexStreamPlaybackArchitecture
        .transition<ReadyToPlay, PlayRequested>(
            "vexstream.playback.play",
        )

val ReadyToPlay =
    VexStreamPlaybackArchitecture
        .placement<ReadyToPlay> {
            arrivedBy(Pause)
            can(Play)
        }
```

The root map is derived from those local declarations. It validates that the SAME transition object
named as an exit by one Placement is named as an arrival by the destination Placement.

```text
WE ARE NOT THE MAP.
WE ARE THE NODES.

map = derived orientation
map != second workflow owner
```

If a developer is lost, the architecture root is the lobby directory:

```kotlin
VexStreamPlaybackArchitecture.map.describe()
```

## Controls and feature gating

One Control may contain one or several candidate transitions.

```text
PlayPause
  candidates:
    Play
    Pause
```

Current Placement resolves at most one.

The architecture root owns one explicit `enabled` flag. Control resolution returns unavailable when
the root is disabled, and registered Vex Control elements are also rejected by VexRuntime before
activation.

This means a future real feature flag can close the architecture realm without scattering:

```text
if (featureEnabled) ...
```

through every descendant.

Keep these distinct:

```text
architecture enabled
!= runtime available
!= authorized
!= current Placement permits this Control
```

## Question-first refactor discipline

Do not begin with:

> refactor this whole feature

Preserve one ordinary question first:

```text
How do I play a song?
How do I pause/resume?
How do Next/Previous work?
How does offline download work?
How does connection/pairing work?
```

For each question:

```text
question
-> current implementation walk
-> mark conceptual detours
-> find convergent meaning
-> form Case / Control / Placement
-> bind rightful platform realization
-> walk backward from observed result
-> ask the original question again
```

If the new walk still requires unrelated implementation knowledge, the slice is not finished.

## Boundaries

```text
Architecture root != manager
Case != implementation phase
Control != Android Controller
Placement != global state machine
Map != execution authority
Architecture enabled != permission/availability
Compiler-valid relationship != exact live currentness
```

Relay remains the right owner when serialization, convergence and atomic exact-current commit are
required. VexArchitecture makes the semantic relational lattice perceivable; it does not replace
Relay.

## First practicum — VexStream Playback

Start at:

```text
VexStreamPlaybackArchitecture
```

Then choose what you need:

```text
cases
  VexStreamPlaybackCase

controls
  VexStreamPlaybackControl

placements
  VexStreamPlaybackPlacement

android realization
  VexStreamPlaybackAndroid

whole orientation
  VexStreamPlaybackArchitecture.map
```

Play/Pause now reads:

```text
ReadyToPlay
  -- Play -->
PlayRequested

PlayRequested
  -- Pause -->
ReadyToPlay
```

Both the VexStream mini-player and Context Dock consume the same Control. Android realization re-reads
exact Media3 truth before performing the selected transition.

<!-- [VXG RealForever][VEX-ARCHITECTURE] -->
