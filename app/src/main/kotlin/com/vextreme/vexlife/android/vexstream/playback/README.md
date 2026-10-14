# VexStream Playback — behavior connection

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

Media3VexStreamPlaybackConnection.kt
    Media3 truth projection + Effect realization

VexStreamPlaybackConnectionHost.kt
    Android configuration lifetime only
~~~

The causal loop is:

~~~text
Media3 Player truth
    -> VexStreamPlaybackState
    -> BehaviorProjection
    -> UI reads Controls / Acts

UI activates Control
    -> Connection re-resolves from latest State
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

## Ownership

~~~text
MediaSessionService / ExoPlayer
    actual playback + queue owner

Media3VexStreamPlaybackConnection
    projection / Effect adapter only

VexStreamPlaybackConnectionViewModel
    Android configuration-lifetime host only

Mini Player / Context Dock
    projection consumers + Control initiators only
~~~

None of the UI surfaces own Playback State.

<!-- [VXG RealForever][VEXSTREAM][PLAYBACK][BEHAVIOR-CONNECTION] -->
