# VexArchitecture V2 — human experience to rightful provider

[VXG RealForever]

## Question

A fresh developer usually starts with ordinary questions:

~~~text
What whole thing am I working on?
What part of that experience owns this meaning?
What is currently true?
What can be requested?
Who actually realizes it?
~~~

VexArchitecture exists to make those questions walkable in source without turning the app into one giant manager/state-machine framework.

For fresh feature/experience formation, use
[`docs/foundation/FEATURE-EXPERIENCE-LANGUAGE.md`](../../docs/foundation/FEATURE-EXPERIENCE-LANGUAGE.md)
before permanent nouns harden. The template turns ordinary Who / Why / What / Where / How / Now /
Control / Provider questions into source and acceptance language without requiring one class per
question.

A useful architectural smell test is:

~~~text
if two peer nouns answer the same human question,
or a derived fact / technical mechanism is promoted beside human product concepts,
resolve the category boundary before adding another abstraction
~~~

PR #80 was the first VexArchitecture practicum. It established useful ideas such as root-only architecture discovery, Case / Control / Placement language and maps derived from local relationship nodes. PR #81 V2 keeps those lessons while correcting category/depth pressure discovered by walking VexStream from a non-engineer's vocabulary.

## The naming ladder

The current ladder is intentionally small:

~~~text
Experience
    whole integrated product experience

Architecture
    reusable structural meaning inside that experience

Capability
    one recognizable part of an Architecture

Case
    which kind of semantic truth currently applies

Control
    what semantic meaning may be requested

Placement
    how the current Case can arrive / where it can go

Provider
    runtime implementation boundary

SDK / native owner
    actual third-party/platform machinery
~~~

The words should answer different questions. Do not use Architecture merely as another word for "folder".

## VexStream example

~~~text
VexStreamExperience
│
├── Library
├── Sources
└── VexStreamPlayerArchitecture
    ├── Playback
    └── Queue
~~~

This shape is deliberately flatter than MusicArchitecture -> PlayerArchitecture -> PlaybackArchitecture.

### Experience

VexStreamExperience means the whole thing the human is living through.

It integrates/orients:
- what music VexStream knows;
- where usable media may come from;
- how playing media is structured.

Experience is broader than Architecture and must not become a God object. It connects/orients; it does not reimplement repositories, source resolution, Media3, downloads, networking or player lifetime.

The package carries the candidate version:

~~~text
...vexstream.experience.v2
~~~

Ordinary class names remain production-shaped:

~~~text
VexStreamExperience
VexStreamPlayerArchitecture
VexStreamPlayback
VexStreamQueue
~~~

### Library

Library answers: What music does VexStream know about?

It means song / album / artist / playlist / catalog identity.

It does not mean:
- where media bytes physically live;
- a filesystem;
- a network source;
- a player provider.

The existing VexStream repository remains the current data/state owner.

### Sources

Sources answers: Where can this media actually come from?

Current examples include verified local bytes and the paired VexStream desktop source. Future partner sources can join the same concept.

This collapses two previously over-promoted nouns:

~~~text
Connection
    = state of a remote Source

Availability
    = answer derived from which Sources can satisfy the media
~~~

Neither needs to be a peer Architecture merely because the facts matter.

### Player Architecture

VexStreamPlayerArchitecture describes the familiar human concept of a media player.

It currently exposes two implemented capabilities:

~~~text
Playback
    what is current media being asked to do?

Queue
    what media surrounds the current item and where are we in that order?
~~~

Capability objects deliberately omit the Architecture suffix:

~~~text
VexStreamPlayback
VexStreamQueue
~~~

They still implement the generic Vex realm contracts underneath so typed Case/Transition validation continues to work.

This lets a newcomer read:

~~~text
VexStream Experience
-> Player Architecture
-> Playback capability
~~~

instead of decoding Architecture -> Architecture -> Architecture.

## Reusable Media Player structure

The project tree starts with **human feature language**. Technical roles live inside the area a
person recognizes:

~~~text
mediaplayer/                         "I'm working on the Media Player"
├── VexMediaPlayerArchitecture.kt
├── VexMediaPlayerProvider.kt
│
├── playback/                        "I'm working on Playback"
│   ├── VexMediaPlaybackContract.kt
│   ├── VexMediaPlaybackProvider.kt
│   └── VexMediaPlaybackStatus.kt
│
└── queue/                           "I'm working on the Queue"
    ├── VexMediaQueueContract.kt
    ├── VexMediaQueueProvider.kt
    └── VexMediaQueueStatus.kt
~~~

The package/folder answers:

> **What part of the feature am I looking for?**

The files inside answer:

> **What kind of code is this?**

That is why architecture/ and provider/ are not the primary navigation folders here. A newcomer
should find Playback or Queue before they need to know our implementation taxonomy.

Status means the provider's current report for that human area. It is translated into the feature's
semantic Case; it is not a second truth owner.

The root architecture contract does not require Queue. A video/live player can still be a media
player without claiming capabilities it does not have.

Do not add Timeline, Output, Tracks, Presentation, Speed, Chapters, etc. merely because modern media
players can have them. Add a human feature area when an actual consumer earns it.

## Contract versus Provider

These are different axes.

~~~text
Architecture Contract
    describes semantic/source organization

Provider
    promises runtime capability
~~~

Runtime contracts currently include:

~~~text
VexMediaPlayerProvider
VexMediaPlaybackProvider
VexMediaQueueProvider
~~~

VexStream combines the capabilities it actually needs as VexStreamPlayerProvider.

The current implementation is Media3VexStreamPlayerProvider.

A future adapter could be VlcVexStreamPlayerProvider, PartnerSdkVexStreamPlayerProvider or RemoteVexStreamPlayerProvider without changing Playback/Queue meaning.

### Why no I prefix?

Prefer role-bearing names such as:

~~~text
VexMediaPlaybackProvider
VexMediaPlaybackContract
~~~

over IVexMediaPlaybackProvider or IPlaybackContract.

I only says "Kotlin interface". Contract and Provider tell the maintainer why the interface exists.

## Playback

~~~text
VexStreamPlayback
├── Case
├── Control
├── Placement
└── Android
~~~

Current Cases:

~~~text
NoMedia
PlaybackNotRequested
PlaybackRequested
~~~

Current Control relationships:

~~~text
PlaybackNotRequested --Play--> PlaybackRequested
PlaybackRequested --Pause--> PlaybackNotRequested
~~~

PlayPause is the human Control that resolves whichever exact relationship is lawful now.

The wording intentionally translates Media3 playWhenReady without overclaiming that audio is already "Playing" while buffering/preparing.

## Queue

~~~text
VexStreamQueue
├── Case
├── Control
├── Placement
└── Android
~~~

Current Cases:

~~~text
Empty
Positioned(itemCount, currentIndex, hasPrevious, hasNext)
~~~

Current controls:

~~~text
Previous
Next
PlaySelected
~~~

Next/Previous are conditional self-transitions of Positioned. Playback truth is not duplicated into Queue relationship names.

## Hidden realm generics

~~~text
VexCase<R>
VexTransition<R, Before, After>
~~~

make the concern a type-level coordinate. Queue Cases cannot accidentally become Playback Before/After types.

That generic is infrastructure depth, not normal feature vocabulary.

## Nodes first, map second

Placements remain local source declarations. Architecture maps are derived validation/orientation.

~~~text
WE ARE NOT THE MAP.
WE ARE THE NODES.
~~~

A map must never become another reducer, workflow owner or provider.

## Where VexArchitecture stops

For VexStream:

~~~text
VexStreamExperience
-> VexStreamPlayerArchitecture
-> Playback / Queue
-> Android translation
-> VexStreamPlayerProvider
-> Media3VexStreamPlayerProvider
-> MediaController
-> MediaSessionService
-> ExoPlayer
~~~

Once Media3/provider vocabulary is clearer, VexArchitecture stops.

## Legacy versus V2

Debug Menu keeps both routes available for lived comparison.

~~~text
Legacy
    current direct VexStream / Media3 behavior

V2
    VexStreamExperience
    -> PlayerArchitecture
    -> Playback / Queue
    -> same Media3 provider
~~~

The selected version is process-local and resets to Legacy after app restart.

The version/experimental status belongs in the package, PR and debug router—not in every normal class name.

## What is deliberately NOT present

V2 currently does not create:
- VexStreamAvailabilityArchitecture;
- VexStreamConnectionArchitecture;
- a reusable Path abstraction;
- a universal giant media-player interface;
- unsupported capability stubs;
- an I-prefix convention.

If cross-node use cases such as Play Song need a reusable composition abstraction later, they must earn it from repeated real consumers rather than being named in advance.

<!-- [VXG RealForever][VEX-ARCHITECTURE-V2][EXPERIENCE][MEDIA-PLAYER] -->
