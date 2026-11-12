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
        | feature-owned mapping
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
-> rightful semantic / request boundary
-> rightful realization/provider
-> later current truth
```

Permanent:

```text
APP_WIDGET != PRODUCT_STATE_OWNER
WIDGET_SURFACE_STATE != DOMAIN_STATE
GLANCE_STATE != DOMAIN_STATE
WIDGET_ACTION_REF != ACT
WIDGET_ACTION_REF != EFFECT
CALLBACK != LONG_RUNNING_WORK_OWNER
SURFACE_STATE_PERSISTED != UPDATE_REQUEST_SUCCEEDED
UPDATE_REQUEST_SUCCEEDED != LAUNCHER_RENDERED
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
: compare exact current durable state to the proposed state using value equality and suppress
duplicate state writes. Current accepted main still couples an update request to a changed write;
PR #255 is the pending runtime correction for preserving failed update-request debt so an exact-state
retry can request Glance update again without rewriting identical SurfaceState.

`transformAll(...)`
: mutate already-persisted surface state for presentation-owned changes outside the main product
observation stream, without inventing product truth. Example: an artwork asset becoming locally
available changes one surface-state field and naturally triggers the same update path.

There is deliberately no product/domain revision counter, rendered-revision acknowledgement,
Boolean invalidation toggle or hash surrogate. If `A -> B -> A`, both SurfaceState transitions are
real because each new value is compared to the exact current durable value. Exact duplicate
`A -> A` suppresses the state write.

PR #255 separately pressures a **foundation-private update-request occurrence fence** for the case
where persisted SurfaceState is current but a prior Glance `update()` request failed. That generation
is not product State identity and does not claim launcher rendering. It remains candidate runtime
source until its currentized head completes review/integration.

## Product / current-truth relationship

The reusable Widget foundation is downstream of rightful product, provider or repository truth. It
does not require one semantic grammar upstream:

```text
rightful current product / provider / repository truth
-> feature-owned mapping
-> VexWidgetSurfaceState

VexWidgetSurfaceState
-> VexWidgetProjection
-> Glance / RemoteViews
```

Interaction returns through the consumer's rightful semantic/request boundary:

```text
widget tap
-> VexWidgetActionRef
-> VexWidgetActionCallback
-> feature-local resolver
-> existing product Control / request boundary
-> rightful realization/provider
-> later current truth
-> later SurfaceState publication
```

A consumer may currently use SCAE/Behavior, Direct Family, an ordinary typed request, or another
lawful product boundary. The Widget foundation does not choose that grammar. SurfaceState remains
remote presentation custody only.

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

VexStream maps current Player/Store truth plus Playback/Queue presentation facts into one typed
widget SurfaceState. Playback, Queue and Repeat are current Direct `VexFamily` owners; Player
participation is modeled through the VexStream Player Partner/Store circuit. Those product semantics
belong to VexStream, not to Widget foundation itself. Artwork availability is presentation truth, so
artwork backfill uses `transformAll` to change that exact field rather than forcing a synthetic
product revision.

VexStream-specific meaning remains with:

```text
vexstream/playback
vexstream/queue
vexstream/repeat
screens/shared/vexstream
media/vexstream
```

## Currentness hold — failed update-request debt

Accepted Widget foundation/currentness provenance remains:

```text
PR #174
    foundation SourceOwner PASS
    Independent Assurance CLEAR_ON_EXACT_HEAD
    tree-exact merge

PR #185
    typed SurfaceState/currentness evolution
    SourceOwner PASS
    Independent Assurance CLEAR_ON_EXACT_HEAD
    lived launcher/currentness closure
    tree-exact merge
```

Open PR #255 owns one narrower runtime correction:

```text
SurfaceState persisted
!= update() request succeeded
!= launcher rendered

same SurfaceState
+ owed failed update request
-> must remain retryable

A -> B -> A update-request occurrences
-> older completion must not settle newer debt
```

Its SourceOwner A002 passed on the exact three-file source candidate and that exact source has now
been recovered onto current main without path-level source drift. Fresh current-head review/proof and
integration remain separate; this README does not treat the generation fence as accepted main before
that lifecycle closes.

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

<!-- [VXG RealForever][ANDROID][APP-WIDGET][GLANCE][SURFACE-STATE] -->
