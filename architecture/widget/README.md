# Android Widget Foundation

`[VXG RealForever]`

## Human purpose

Let VexLife expose compact Android home-screen surfaces without turning an App Widget into another
product State owner or effect runtime.

The first real consumer is VexStream playback, but this module does not contain music vocabulary.

## Current architecture

```text
product-owned State / provider truth
        |
        | feature-owned pure mapping
        v
typed VexWidgetSurfaceState
        |
        | foundation-owned persistence + structural equality
        v
Glance Preferences
        |
        | currentState() inside the active composition
        v
SurfaceState -> suspend Projection transform
        |
        v
VexWidgetProjection
        |
        v
Jetpack Glance / RemoteViews host
```

Interaction returns through a separate bounded doorway:

```text
widget tap
-> VexWidgetActionRef
-> VexWidgetActionCallback
-> feature-local resolver
-> existing product Control / request boundary
-> current State -> Act -> Effect
-> existing Connection/provider
```

Permanent:

```text
APP_WIDGET != PRODUCT_STATE_OWNER
WIDGET_SURFACE_STATE != DOMAIN_STATE
GLANCE_STATE != DOMAIN_STATE
WIDGET_ACTION_REF != ACT
WIDGET_ACTION_REF != EFFECT
CALLBACK != LONG_RUNNING_WORK_OWNER
UPDATE_REQUESTED != LAUNCHER_RENDERED
```

## Why Glance

The Android platform still hosts App Widgets through AppWidget/RemoteViews mechanics. Jetpack Glance
is the Compose-first authoring layer over that platform and keeps this repository aligned with its
existing Compose-first UI direction.

Current source pins stable Glance 1.2.0. Its active composition remains alive across update requests;
therefore durable widget state must be observed inside `provideContent` rather than captured once
before composition.

The foundation intentionally does not wrap every Glance primitive. Product widgets use ordinary
Glance Row/Column/Image/Text/action APIs unless a repeated lifecycle/currentness invariant earns a
shared helper.

## Surface-state lifecycle

`VexWidgetSurfaceState`
: typed, structurally comparable remote-surface snapshot. It is the smallest durable value needed to
reproduce a widget after process/session loss. It is not SCAE/domain State.

`VexWidgetPreferenceCodec<State>`
: feature-owned mapping between typed surface state and Glance Preferences keys.

`VexGlanceAppWidget<State, Projection>`
: foundation-owned Preferences persistence, active-composition observation, suspendable
State-to-Projection recomputation and bounded update fan-out.

`publish(...)` / `publishAll(...)`
: compare exact current durable state to the proposed state using value equality, persist only real
changes, and request Glance update only for changed instances.

`transformAll(...)`
: mutate already-persisted surface state for presentation-owned changes outside the main product
observation stream, without inventing product truth. Example: an artwork asset becoming locally
available changes one surface-state field and naturally triggers the same update path.

There is deliberately no revision counter, Boolean invalidation toggle or hash surrogate. If
`A -> B -> A`, both transitions are real because each new value is compared to the exact current
surface value. Exact duplicate `A -> A` is suppressed.

## SCAE relationship

The widget foundation sits downstream of SCAE rather than becoming another family:

```text
CURRENT product State + Controls
-> existing Behavior Projection

provider/read truth + existing Projection(s)
-> feature-owned widget SurfaceState

SurfaceState
-> widget Projection
-> RemoteViews
```

Controls still return through the rightful product family. SurfaceState is remote presentation
custody only.

## Source roles

`VexWidgetSurfaceState`
: marker for typed durable widget surface snapshots.

`VexWidgetPreferenceCodec`
: persistence adapter for one feature-owned surface-state shape.

`VexWidgetPublishResult`
: bounded receipt for persistence/update requests; never claims launcher rendering.

`VexWidgetProjection`
: already-derived render-facing facts.

`VexGlanceAppWidget<State, Projection>`
: reusable state observation/transformation/update lifecycle.

`VexWidgetActionRef`
: stable identity for which widget doorway was activated.

`VexWidgetActionCallback`
: validates/extracts that identity before feature-local code maps it to existing product semantics.

## Callback lifetime

Glance App Widget callbacks are receiver-bound Android work. Keep them short.

Long I/O, network fetches or durable background work must be delegated to the rightful service,
WorkManager/job owner, or product repository. A callback may request a bounded product operation; it
does not become the operation lifetime.

## Patient zero — VexStream

The first consumer remains deliberately compact:

```text
artwork | title/artist | previous | play/pause | next
```

VexStream maps Media3/provider truth + existing Playback/Queue projections into one typed widget
SurfaceState. Artwork availability is presentation truth, so artwork backfill uses `transformAll`
to change that exact field rather than forcing a synthetic revision.

VexStream-specific meaning remains with:

```text
vexstream/playback
vexstream/queue
vexstream/repeat
screens/shared/vexstream
media/vexstream
```

## Proof

```text
:architecture:widget:test
    action identity + surface-state equality laws

vexVerifyJvm
    includes :architecture:widget:test

vexVerifyLocal
    proves module + app integration/source build
```

A launcher-hosted appearance/update claim still needs Android/device or lived proof.

<!-- [VXG RealForever][ANDROID][APP-WIDGET][GLANCE][SCAE] -->
