# POC — Android Navigation / Deterministic Guidance

`[VXG RealForever]`

## Why this branch exists

The model should help understand destination/context and answer questions about the trip.

It should **not** be responsible for deciding the millisecond or distance at which the driver is told
to turn.

The core question is:

> Can Android Navigation own the route, maneuver stream, location/lifecycle, and deterministic cue
> thresholds while Vex receives a smaller situational projection and may reuse the same speech
> infrastructure without becoming the guidance timer?

## Platform seam under investigation

The primary candidate is Google Navigation SDK for Android:

~~~text
route request
-> Navigator
-> navigation-state / maneuver / remaining-time-distance observations
-> deterministic guidance policy
-> speech/presentation
~~~

The branch must explicitly account for the Navigation SDK's relationship to the Maps SDK rather than
assuming both can be composed independently in one app.

Location permission, foreground/background execution, and physical-device behavior remain
Android-owned facts.

## Truth owners

~~~text
NavigationCapabilityState
  Unsupported
  PermissionMissing
  Ready

NavigationSessionState
  Idle
  RoutePreview(...)
  Navigating(routeRef, routeRevision)
  Arrived
  Failed

ManeuverState
  maneuverRef
  instructionProjection
  distanceToManeuver
  timeToManeuver
  laneProjection
~~~

Raw high-frequency location is not automatically model context.

## Deterministic cue engine

A small policy layer consumes current navigation truth and emits idempotent guidance cues:

~~~text
routeRevision + maneuverRef + cueBand
-> at most one GuidanceCue

Far
Approaching
TurnNow
Passed / replaced
~~~

Exact thresholds should be tested against Navigation SDK behavior and driving speed rather than
invented as permanent product constants in this document.

Reroute replaces the relevant route/maneuver identity. Old pending cues must therefore become stale
without the model needing to notice.

## Voice identity

One speech engine may be reused without collapsing meanings.

~~~text
SpeechRole.Companion
  conversational / user-requested Vex response

SpeechRole.Navigation
  deterministic maneuver cue

SpeechRole.SystemConfirmation
  short effect acknowledgment
~~~

The same synthesized voice can use a different earcon, phrase grammar, or subtle prosody for
Navigation. A later lived test may justify a distinct voice. The important invariant is that the user
can tell deterministic navigation guidance from conversational Vex.

The model never authors the turn-timing event.

## Model context

Prefer a bounded situational projection:

~~~text
navigation = {
  phase,
  destinationLabel,
  etaBucket,
  nextManeuverKind,
  maneuverProximityBucket,
  rerouting
}
~~~

Vex can answer "how much longer?", "find coffee along the way", or "take me home" without receiving a
new GPS coordinate every second.

## First source slice

1. initialize the Navigation SDK behind a replaceable adapter;
2. request the minimum location permission needed for foreground navigation;
3. create one simulated route;
4. observe maneuver / remaining-distance-time / route-change state;
5. run deterministic cue emission with deduplication;
6. keep SDK voice guidance silent or otherwise controlled during custom-cue proof so two owners do
   not speak the same turn;
7. revoke location permission / end guidance and prove state/actions fail closed.

Background location or location foreground-service expansion is added only when a demonstrated
navigation lifetime actually requires it.

## Proof

Use the SDK simulator/emulator where available for repeatable threshold tests, then a physical-device
drive for the Android/lived claim.

Important cases:

~~~text
normal route
high/low approach speed
reroute before pending maneuver
two near maneuvers
temporary location degradation
permission loss
arrival
model unavailable while navigation continues
~~~

Navigation must continue safely without the LLM.

<!-- [VXG RealForever][VEXLIFE-ANDROID][POC][NAVIGATION] -->
