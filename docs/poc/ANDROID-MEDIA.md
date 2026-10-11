# POC — Android Media Sessions / VexStream Music

`[VXG RealForever]`

## Why this branch exists

"Music integration" contains at least two different relationships:

> VexStream owns playback.

and:

> Another app already owns playback and Vex can perceive/control the current media session when the
> platform permits it.

Those should share a small playback vocabulary without sharing source ownership.

## Platform seams under investigation

### VexStream-owned playback

Use AndroidX Media3 with a `MediaSessionService` so playback/session lifetime is not owned by a
Compose Screen or by the model conversation.

### External-session control

Use Android's media-session/controller surfaces only when the exact device/app relationship permits
access.

Notification-listener or privileged media-control eligibility is a platform capability boundary, not
something to bypass through Accessibility.

## Truth owners

~~~text
MediaCapabilityState
  Unsupported
  OwnPlaybackReady
  ExternalSessionUnavailable
  ExternalSessionReady

PlaybackState
  sessionRef
  sourceOwner
  Idle | Buffering | Playing | Paused | Ended | Failed
  currentItemProjection
~~~

The underlying player/session remains with its rightful owner. Vex receives a bounded projection.

"Find music" is a catalog/search concern. It is not the same state owner as "what is playing now".
A future provider/AppFunction/catalog adapter may satisfy search without changing the media-session
contract.

## Available actions

Examples:

~~~text
Playing(sessionRef)
  -> Pause(sessionRef)
  -> SkipNext(sessionRef) when supported

Paused(sessionRef)
  -> Play(sessionRef)

No controllable session
  -> no playback-control action
~~~

Every action binds the same session identity that was advertised. A newly active media app/session
must invalidate stale action requests.

## Audio relationship

This POC should expose state that a later deterministic audio coordinator can consume. It should not
put audio priority decisions in the LLM.

The likely relationship to prove is:

~~~text
media
  lower-priority continuous audio

navigation cue
  transient deterministic guidance

companion speech
  transient conversational speech

phone call
  platform/highest-priority communication audio
~~~

Android audio focus and Telecom behavior should do as much native coordination as possible. Vex adds
only the product policy that Android cannot infer.

## First source slice

1. bind a minimal Media3 Player + MediaSessionService;
2. prove playback survives navigation away from VexStreamMusicScreen;
3. project current playback into a bounded owner state;
4. derive Play/Pause/Skip availability from that state;
5. observe/control one external test media session when exact platform capability exists;
6. request/release audio focus correctly and observe transient duck/pause behavior;
7. remove/replace the active session and prove stale controls fail closed.

Do not add recommendation algorithms, music-provider credentials, playlists, remote Home streaming,
or a universal audio mixer merely to complete the POC.

## Proof

~~~text
own session:
start item
-> leave Screen / app UI changes
-> playback/session remains correctly owned
-> pause/resume through typed action
-> service/session cleanup

external session, when admitted:
test player active
-> session discovered
-> bounded metadata/state projected
-> supported control executed
-> session replaced/stopped
-> stale control rejected

audio:
media playing
-> transient guidance/companion focus event
-> expected duck/pause/resume behavior
-> active phone call does not leave competing ordinary media speech
~~~

The exact call/media interaction ultimately needs physical-device proof.

<!-- [VXG RealForever][VEXLIFE-ANDROID][POC][MEDIA] -->
