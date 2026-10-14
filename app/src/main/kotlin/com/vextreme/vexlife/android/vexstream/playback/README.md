# VexStream Playback — VexPort

[VXG RealForever]

This package is the first production-shaped consumer of the reusable Behavior Blueprint.

Read the files as one small semantic map:

~~~text
VexStreamPlaybackState.kt
    what Playback truth is observed now

VexStreamPlaybackControl.kt
    finite human affordance vocabulary

VexStreamPlaybackAct.kt
    human/product verbs those Controls may mean

VexStreamPlaybackEffect.kt
    runtime verbs selected by lawful Acts

VexStreamPlaybackBehavior.kt
    State -> Act -> Effect relationships

VexStreamPlaybackPresentation.kt
    semantic Control projection -> shared VexStream icon/enabled meaning

Media3VexStreamPlaybackPort.kt
    Media3 truth projection + Effect realization

VexStreamPlaybackPortHost.kt
    Android configuration lifetime only
~~~

The causal loop is:

~~~text
Media3 Player truth
    -> VexStreamPlaybackState
    -> BehaviorProjection
    -> UI reads Controls / Acts

UI activates Control
    -> Port re-resolves from latest State
    -> Act
    -> Effect
    -> MediaController
    -> Media3 truth changes
    -> repeat
~~~

## First admitted slice

Only Play/Pause transport meaning is migrated in this slice.

The existing MediaController still directly serves queue, seek, shuffle, repeat and media-item
operations. Those are not silently claimed by this package merely because they are nearby.

The old `Player.isPlaying -> icon -> toggle()` duplication is intentionally replaced. Media3
`playWhenReady` plus lifecycle/command facts form the semantic Playback State instead, so buffering
with playback requested can truthfully project Pause even while `Player.isPlaying` is false.

## Synchronous intent emission

The Media3 Port emits admitted intent before dispatching the provider command.

~~~text
activate(PlayPause)
    -> resolve Act.Play
    -> resolve Effect.RequestPlay

synchronously:
    Media(
        playRequested=true,
        requestPending=true,
        ...
    )

then:
    MediaController.play()
~~~

A delayed Media3 observation that still reports playWhenReady=false is folded under the newer local
request rather than flickering the UI back to Play. The pending flag clears only after Media3
acknowledges the requested value.

This is a latency bridge, not another truth owner: Media3 still owns provider playback facts and the
Port explicitly exposes when acknowledgement is pending.

## Presentation seam

Mini Player and Context Dock both consume the same semantic Projection and the same feature-local
presentation mapping:

~~~text
BehaviorProjection
    PlayPause -> Act.Play | Act.Pause | unavailable

VexStreamPlaybackPresentation
    -> Play icon / Pause icon / neutral disabled PlayPause icon
~~~

This mapping is intentionally feature-local rather than part of the reusable Behavior kernel.
Presentation may choose icons, labels, animation, hidden/disabled treatment, or other visual
language without creating another behavior truth.

## Ownership

~~~text
MediaSessionService / ExoPlayer
    actual playback + queue owner

Media3VexStreamPlaybackPort
    projection / Effect adapter only

VexStreamPlaybackPortViewModel
    Android configuration-lifetime host only

Mini Player / Context Dock
    projection consumers + Control initiators only
~~~

None of the UI surfaces own Playback State.

<!-- [VXG RealForever][VEXSTREAM][PLAYBACK][BEHAVIOR-CONNECTION] -->
