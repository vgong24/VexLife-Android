# VexMaps Feature Topology — Meaning, Relationship, Effect, Presentation

[VXG RealForever]

## The question

> Can a fresh Android developer look at VexMaps and tell which object owns meaning, which object
> reconciles changing truths, which object talks to a provider, and which object merely presents the
> result — without learning that separation from predecessor chat?

This map records the shape currently earned by VexMaps. It is a feature map, not a requirement that
every Android feature create the same classes.

## Current topology

~~~text
VexMapsScreen
  static semantic identities + actions
  custom render bridge
        |
        v
VexMapsMapSurface
  visible feature composition
        |
        +--> VexMapsRepository
        |      durable Android-local Maps context/preferences
        |
        +--> VexMapsStyleConvergenceRelay
        |      selected/requested/loaded/failed style relationship
        |
        +--> VexMapsNavigationProcess / VexNavigationSession
        |      one process-local journey lifetime
        |             |
        |             +--> VexNavigationConvergenceRelay
        |                    route + route-relative fix -> guidance/progress
        |
        +--> VexMapLibreAdapter
        |      VexMaps rendering/camera/source/layer meaning -> MapLibre SDK
        |
        +--> VexDeviceLocationSource / offline/search/routing stores
        |      Android/provider observations and effects
        |
        +--> planner / navigation overlay / Context Dock
               presentation projections
~~~

The filesystem should let a developer descend through those questions instead of treating one large
Composable as the architecture.

## Screen registration is not always the visual tree

VexMaps is a custom/native Screen.

~~~text
VexMapsScreen.content
  -> static semantic registration
  -> action coverage / registry identity

VexMapsScreen.renderContent(...)
  -> actual visible Maps composition
  -> VexMapsMapSurface
~~~

For an ordinary blueprint Screen those two shapes may be identical. For VexMaps they intentionally
are not. The Maps Preview therefore uses the rendered-Screen preview path rather than pretending the
generic blueprint tree is the native map UI.

~~~text
SEMANTIC_REGISTRATION != CUSTOM_VISUAL_COMPOSITION
CUSTOM_RENDERER != HIDDEN_SECOND_ACTION_REGISTRY
~~~

## Relay shape learned from Events + Maps

Events is the clearest current example of a session-owned Compound Relay:

~~~text
repository Flow + preference Flow + lab Flow + profile Flow + surface facts
  -> EventsConvergenceRelay
  -> one coherent attention/list output
~~~

Navigation is another Compound relationship:

~~~text
route truth + route-relative location fix
  -> VexNavigationConvergenceRelay
  -> progress / cue / arrival meaning
~~~

Style convergence is smaller but still relational:

~~~text
human selection
+ apply request
+ MapLibre loaded/failure observation
  -> VexMapsStyleConvergenceRelay
  -> style application status
~~~

The reusable lesson is not "everything should use Relay."

~~~text
one independently owned truth
  -> keep its owner/projection simple

multiple independently changing truths whose combination has semantic meaning
  -> Compound Relay is a candidate

provider/SDK effect
  -> adapter/source/store at the provider boundary
  -> report observations back into the semantic owner

presentation-only concern
  -> Compose/component projection
  -> do not acquire domain ownership
~~~

VexCompoundRelay.dependencies() is useful when the relationship continuously observes Flow-owned
truth, as Events does. A Compound Relay may also receive explicit observation methods when the
relationship is driven by effect callbacks or feature-local facts. The reason for Compound is the
semantic relationship, not whether every input happens to be a Flow.

## Provider adapters do not become semantic controllers

VexMapLibreAdapter exists to answer:

> How does VexMaps rendering meaning become MapLibre operations?

It does not own the selected style, journey lifecycle, route semantics, persisted context, or Screen
identity. If a second map renderer ever becomes real, that consumer may earn a provider-neutral
renderer contract. Until then, MapLibre remains honestly named and feature-local.

## Source-size rule

The first topology split extracted existing owners rather than inventing a framework:

~~~text
VexMapsMapSurface.kt
  -> composition only

VexMapLibreAdapter.kt
  -> provider mechanics

VexMapLibreLifecycle.kt
  -> Android MapView lifecycle

VexMapsViewport.kt
  -> restorable viewport meaning

VexMapsPlanner.kt
  -> travel-planning presentation

VexMapsNavigationOverlay.kt
  -> prepared/active navigation presentation

VexMapsPlanningState.kt
  -> planner/search/request state

VexMapsLocation.kt
  -> location readiness/presentation
~~~

A file is not split merely because it is long. Split when another human question already exists and
the current file physically hides that owner.

## Persisted location checkpoint is not live location

The structural split exposed one semantic ambiguity that can be resolved without inventing a new
persistence schema: the durable last-location row is an observation checkpoint, not a current Android
fix.

~~~text
VexMapsStoredLocation
  -> durable last accepted precise-device observation
  -> accuracy + capturedAt remain evidence

projectVexMapsStoredMapContext(...)
  -> non-authoritative browse context only
  -> requires precise-location permission to remain active

currentLocation / navigation readiness
  -> fresh Android observation only
~~~

On reopen, a retained checkpoint may center the map while no fresh fix is available, but it is no
longer promoted into `currentLocation`, cannot become a route origin merely by surviving process
history, and cannot satisfy navigation-start readiness.

No age/accuracy cutoff is invented in this pass. Those facts remain attached to the checkpoint so a
future product decision can define when historical context is too stale or imprecise to restore. A
full durable viewport (zoom/bearing/tilt) would require a separate persistence/migration decision and
is not smuggled into the existing location columns.

## Still intentionally unresolved

This topology pass does not silently settle behavioral questions that need their own proof:

- whether the durable checkpoint should graduate into explicit persisted viewport state;
- what age/accuracy policy should govern historical browse-context restoration;
- whether Context Dock depth gestures should move exclusively to the drag handle;
- whether a future second renderer earns a provider-neutral rendering contract.

Those are next-edge questions, not reasons to muddy the structural split.

<!-- [VXG RealForever][VEXLIFE-ANDROID][VEXMAPS][FEATURE-TOPOLOGY] -->
