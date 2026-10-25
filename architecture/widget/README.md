# Android Widget Foundation

`[VXG RealForever]`

## Human purpose

Let VexLife expose compact Android home-screen surfaces without turning an App Widget into another
product State owner or effect runtime.

The first real consumer is VexStream playback, but this module does not contain music vocabulary.

## Current architecture

```text
product-owned State / provider read truth
        |
        v
feature-owned widget Projection
        |
        v
VexGlanceAppWidget<Projection>
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
APP_WIDGET != PLAYER
GLANCE_STATE != DOMAIN_STATE
WIDGET_ACTION_REF != ACT
WIDGET_ACTION_REF != EFFECT
CALLBACK != LONG_RUNNING_WORK_OWNER
```

## Why Glance

The Android platform still hosts App Widgets through AppWidget/RemoteViews mechanics. Jetpack Glance
is the Compose-first authoring layer over that platform and keeps this repository aligned with its
existing Compose-first UI direction.

Current source pins the stable Glance 1.2.0 line. Do not move to an alpha merely for novelty.

The foundation intentionally does not wrap every Glance primitive. Product widgets should use normal
Glance Row/Column/Image/Text/action APIs directly unless a repeated semantic invariant earns another
shared helper.

## Source roles

`VexWidgetProjection`
: marker for already-derived widget-facing facts; it is not another State machine.

`VexGlanceAppWidget<Projection>`
: requires projection to be obtained before the Glance composable receives it.

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

The first intended consumer is deliberately compact:

```text
artwork | title/artist | previous | play/pause | next
```

One horizontal row is the visual reference. No stacked mini-app card, queue list, lyrics/comments,
or music-specific controls belong in this foundation.

VexStream-specific meaning remains with:

```text
vexstream/playback
vexstream/queue
vexstream/repeat
screens/shared/vexstream
media/vexstream
```

The VexStream widget should consume the existing Playback/Queue projections, use optional canonical
artwork metadata, and deep-link into the VexStream full-player Screen.

## Proof

```text
:architecture:widget:test
    stable callback/action identity

vexVerifyJvm
    includes :architecture:widget:test

vexVerifyLocal
    proves module + app integration/source build
```

A launcher-hosted widget appearance/update claim still needs Android/device or lived proof.

<!-- [VXG RealForever][ANDROID][APP-WIDGET][GLANCE][SCAE] -->
