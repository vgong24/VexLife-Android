# Behavior Blueprint — review checkpoint

[VXG RealForever]

This document is the shortest review entry for PR #82.

Construction pattern:
[`VEX-BEHAVIOR-FAMILY-BLUEPRINT.md`](VEX-BEHAVIOR-FAMILY-BLUEPRINT.md)

Code-navigation roots:

~~~text
VexBehaviorDocs
VexStreamPlaybackDocs
~~~

The architecture is ready for human architecture review when a reviewer can understand the four
pressure shapes below without reconstructing hidden framework ownership.

## 1. Finite behavior — Playback

~~~text
State
    Paused
    Playing(speed)

Control
    Rewind
    PlayPause
    FastForward

Act
    Play
    Pause
    Rewind
    FastForward

Effect
    SetSpeed(speed)
    PausePlayback
~~~

The graph is authored from current State outward and supports both forward resolution and inverse
Control projection.

The real VexStream Play/Pause consumer demonstrates the same grammar against Media3.

In IDE/code navigation, start at `VexStreamPlaybackDocs` rather than reconstructing the package from filenames.

## 2. Data-flavor behavior — same State kind + same Act

The kernel allows several relationships with the same source State KIND + Act when the predicates
are mutually exclusive for one exact current State value.

Example pressure:

~~~text
Queue Positioned(hasNext=true)
    + Next
    -> Advance

Queue Positioned(hasNext=false, wrapAtEnd=true)
    + Next
    -> Wrap
~~~

Both are the same Kotlin State kind and same Act.

The architecture rejects only the ambiguous case where more than one same-Act predicate is true for
the exact current State value.

This is the path for data-heavy domains such as navigation phase without forcing artificial sealed
subclasses merely to satisfy the DSL.

## 3. Async provider behavior — Maps Location pressure example

Pure test example:

~~~text
NoPrecisePermission
    -> UsePreciseLocation unavailable

PrecisePermission(acquisition=Idle)
    + UsePreciseLocation
    -> Act.Locate
    -> Effect.RequestCurrentLocation
    -> admit PrecisePermission(acquisition=Finding)

provider later may observe:
    PrecisePermission(currentFix=...)
    ServicesDisabled
    Failed(...)
~~~

The relationship's admitted State kind is immediate semantic topology after the Effect is accepted
for dispatch. It is NOT a guaranteed final provider outcome.

The concrete Connection owns runtime construction of the admitted State value and may include facts
such as request generation.

The same example carries a latest-generation gate:

~~~text
activate
    -> generation N

later old result from generation N-1
    -> concrete Connection rejects it
~~~

This deliberately proves:

~~~text
VexPort re-resolution
    stale render -> activation protection

latest-request / revision gate
    activation -> stale async completion protection
~~~

They are complementary and must not be collapsed.

Source:

~~~text
architecture/behavior/src/test/.../example/maps/MapsLocationPressureExample.kt
architecture/behavior/src/test/.../example/maps/MapsLocationPressureExampleTest.kt
~~~

This is a pressure example only, not a production VexMaps migration.

## 4. Higher-order convergence stays separate

Do not encode independent domain combinations into one family's State vocabulary.

~~~text
Playback Projection -----\
Location Projection ------+-> higher convergence / attention owner
Events Projection --------/
~~~

The existing EventsConvergenceRelay remains evidence for that layer.

Local Behavior families answer:

~~~text
What is true in this feature?
What Controls have meaning now?
What Act does this Control mean?
What Effect should be attempted?
~~~

Convergence answers:

~~~text
Given several complete local truths/projections, what should win or combine here?
~~~

## Family / role safety

The F-bounded Family is compile-time kinship only.

~~~text
ROLE SAFETY
    State cannot occupy Act / Control / Effect position

FAMILY SAFETY
    QueueControl cannot be paired with PlaybackState
~~~

The same Family coordinate runs through Blueprint, ControlBlueprint, Projection, VexPort and
VexPortFlow.

## Human-facing type threshold

Foundation safety should not dominate ordinary feature code.

VexStream therefore exposes feature-local aliases:

~~~text
VexStreamPlaybackConnection
VexStreamPlaybackConnectionFlow
VexStreamPlaybackProjection
~~~

while those aliases preserve the complete Family-bound generics underneath.

A second feature should follow the same pattern before inventing another generic wrapper.

## Deliberately held

The following are NOT solved by this checkpoint:

~~~text
runtime payload architecture
    RouteTo(currentGps, selectedDestination, WALK)
    SearchPlaces(currentQuery)
    ImportMap(selectedUri)

generic UIElement -> Control binding
generic heterogeneous convergence envelope
generic concurrency/locking semantics
generic async result/failure grammar
~~~

Do not weaken or expand the kernel to pretend these are solved.

## Review questions

A reviewer should be able to answer:

1. Can I find the complete semantic vocabulary for one family?
2. Can I see what each Act means from current State?
3. Can State flavor alter Effect without type explosion?
4. Can an admitted State differ from eventual provider observation without lying?
5. Does VexPort protect activation currentness without claiming async-result currentness?
6. Do concrete Connections retain their provider/threading/request-gate responsibilities?
7. Are Family-heavy generics mostly hidden from ordinary UI code?
8. Is convergence kept above complete local projections rather than cross-multiplied into State?

If those answers are clear from source, the architecture has reached the intended perceptible review
threshold even before every future consumer is implemented.

<!-- [VXG RealForever][VEX-BEHAVIOR-BLUEPRINT][REVIEW-CHECKPOINT] -->
