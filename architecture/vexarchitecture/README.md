# VexArchitecture V2 - semantic directory + bounded relational truth

[VXG RealForever]

## Question

A developer usually starts with a human question:

```text
How do I play this song?
Why is Next unavailable?
What happens when Download finishes?
How do I reconnect to my computer?
```

The first VexArchitecture experiment in PR #80 proved several useful ideas: architecture roots can be
IDE navigation indexes, Cases can name current truth, Controls can name human requests, Placements can
name local arrivals/exits, and a map can validate those local declarations without becoming another
workflow owner.

It also exposed pressure. Playback began absorbing Queue truth, multi-concern intentions such as Play
Song felt awkward inside one realm, naming drift appeared as the experiment evolved, and one practicum
file started becoming another object a fresh developer had to mentally decompose.

V2 keeps the useful discoveries but asks a larger question:

> Can code act like a typed semantic directory that keeps narrowing from the human concern until
> another owner - repository, Relay, player, Android service, Media3 - has the better vocabulary?

PR #80 is provenance/quarry for this version, not a branch dependency.

## Directory grammar

```text
System
-> Domain
-> Concern
-> Role
-> Meaning
```

Example:

```text
VexStream
-> Music
-> Player
-> Playback
-> Control
-> Play
```

The absolute walk is for orientation. Local feature code stays relative, such as
`VexStreamPlaybackControl.Play`, just as a filesystem has absolute and relative paths.

## Reserved words

```text
Architecture
    Start here for this concern. Orientation/realm boundary, not manager.

Case
    Which kind of semantic truth currently applies.

Control
    Meaning a consumer may request. Not Android MediaController.

Placement
    Local neighborhood: how this Case can arrive and where it can go.

Transition
    Typed Case -> Case relationship. Never the external effect itself.

Android
    Android translation/realization for that concern.

Observation
    Facts entering architecture from the real owner/platform.

Projection
    Read-only composition of rightful truths for a consumer.

Path
    Feature-local human intention that crosses more than one concern.
```

Path is intentionally not yet a reusable core VexArchitecture primitive. VexStream V2 will test
whether the word remains useful before the foundation promotes it.

## Fractal does not mean identical

```text
Music
|-- Library
|-- Player
|-- Availability
+-- Connection

Player
|-- Playback
+-- Queue
```

Music and Player can be orientation groups. Playback and Queue can independently own
Case/Control/Placement. We do not invent a MusicCase merely to make every layer symmetrical.

The invariant is:

```text
same architecture word -> same expectation
```

not:

```text
every architecture node -> every role
```

## Realm generics

`VexCase<R>` and `VexTransition<R, Before, After>` make the concern a type-level coordinate. Queue
truth cannot accidentally become the source/destination of a Playback Transition.

That generic is infrastructure depth, not developer vocabulary. Feature code should still read:

```text
VexStreamPlaybackCase.PlaybackRequested
VexStreamQueueControl.Next
```

## Relative names, absolute refs

A root owns an absolute identity:

```text
vexstream.music.player.playback
```

Local source declares:

```text
play
pause
play-pause
```

and the foundation derives:

```text
vexstream.music.player.playback.play
```

This removes repetitive prefix typing while keeping semantic identity independent from Kotlin package
movement.

## Parent gating

```text
VexStream disabled
-> Music disabled
-> Player disabled
-> Playback and Queue disabled
```

Keep these truths separate:

```text
ARCHITECTURE_ENABLED
!= RUNTIME_AVAILABLE
!= AUTHORIZED
!= CURRENT_PLACEMENT_ALLOWS_CONTROL
```

## Nodes first, map second

Placements are local source declarations. The map is derived validation/orientation.

```text
WE ARE NOT THE MAP.
WE ARE THE NODES.
```

The map must never become another reducer or effect owner.

## Where the fractal stops

The intended VexStream walk is:

```text
VexStream
-> Music
-> Player
-> Playback / Queue
-> Android realization
-> VexStreamPlayer
-> Media3VexStreamPlayer
-> MediaController / MediaSession
-> ExoPlayer
```

VexArchitecture stops where the real player/platform vocabulary becomes clearer. There is no
VexExoPlayerArchitecture merely because deeper nesting is possible.

## Legacy versus V2

V2 is introduced beside current VexStream rather than replacing it immediately.

Debug Menu selects:

```text
Legacy
    current direct VexStream / Media3 path

V2
    semantic directory -> realm -> Android bridge -> same real Media3 owner
```

Both must use the same underlying player/lifetime owner. The comparison asks whether V2 actually
improves orientation, naming, debugging direction and separation of Playback versus Queue - not
whether a different audio engine happens to behave differently.

<!-- [VXG RealForever][VEX-ARCHITECTURE-V2][SEMANTIC-DIRECTORY] -->
