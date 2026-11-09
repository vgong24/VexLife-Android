# Android Widget Foundation

`[VXG RealForever]`

## Human purpose

Let VexLife expose compact Android home-screen surfaces without turning an App Widget into another
product State owner or effect runtime.

The first real consumer is VexStream playback, but this module does not contain music vocabulary.

## Rooted Widget coordinate

Widget has earned its own reusable root because a remotely hosted interactive surface owns a distinct
responsibility: durable presentation custody, remote-host realization and stable interaction
doorways over truth owned elsewhere.

```text
VexWidget
├─ Relay
├─ SurfaceState
├─ Projection
└─ Action
   └─ Ref
```

The root does not make Widget a product-State owner. A concrete `Widget.Relay` may reuse
`VexCompoundRelay` when that Widget genuinely needs to converge bounded CURRENT from independent
owners. Input origins should remain explicit when ordering matters; the VexStream patient zero uses
separate post-Family `ProviderRoundTrip` and `FamilyCurrent` admissions rather than racing passive
owner subscriptions. Relay remains the relationship mechanism; Widget remains the remote-surface owner.

## Current architecture

```text
product-owned State / provider truth
        |
        | concrete Widget.Relay when independent owners must converge
        v
bounded Widget Relay output
        |
        | feature-owned surface mapping
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

`VexWidget`
: recursive root coordinate for the remote-surface responsibility. Concrete Relay output,
SurfaceState, Projection and Action vocabularies carry the exact Widget coordinate so ownership is
compiler-visible rather than inferred from package or naming, without absorbing Family, Partner,
Service, Execution or Relay ownership.

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
