# VexMaps Feature Topology — Meaning, Relationship, Effect, Presentation

[VXG RealForever]

## The question

> Can a fresh Android developer look at VexMaps and tell which object owns meaning, which object
> reconciles changing truths, which object talks to a provider, and which object merely presents the
> result — without learning that separation from predecessor chat?

This map records the shape currently earned by VexMaps. It is a feature map, not a requirement that
every Android feature create the same classes.

It is also **not a product-completion claim**.

~~~text
ownership topology current
!=
ordinary Maps human journey complete
~~~

For current VexMaps capability truth and the active [VexMaps Complete] experiential trajectory, start
at:

~~~text
app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/maps/README.md
app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/maps/design/README.md
VexMapsDocs
~~~

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
        +--> VexMapsDestinationSearchSession
        |      visible "Where to?" request/result currentness
        |             |
        |             +--> VexMapsAndroidGeocoderPlaceSearchSource
        |                    best-effort Android system place/address observation
        |
        +--> VexMapsDistributionSession
        |      lived Distribution / Delivery Route relationship + currentness
        |             |
        |             +--> Distribution Mode + Delivery Route Behavior families
        |             |      independently complete local semantic owners
        |             |
        |             +--> Mode <-> Delivery Route + Delivery Route <-> Navigation Relays
        |             |      higher-order relationship owners
        |             |
        |             +--> VexMapsDistributionExecution
        |                    coroutine custody only
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
human appearance/style intent
+ apply request
+ MapLibre loaded/failure observation
  -> VexMapsStyleConvergenceRelay
  -> style application status
~~~

The VMC-01 appearance slice presents ordinary map appearance as a compact Automatic / Light / Dark
map control. OpenFreeMap Dark/Liberty remain provider realizations below the human-facing preference
and no longer occupy ordinary Context Dock pages.

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
  -> current planning/import presentation state + bounded request-gate primitive

VexMapsPlaceSearchSession.kt
  -> local place-search pack/attempt/result currentness

VexMapsDestinationSearchSession.kt
  -> ordinary submitted destination-search attempt/result currentness

VexMapsAndroidGeocoderPlaceSearchSource.kt
  -> best-effort Android system place/address provider boundary

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


## VexMaps Distribution and Delivery Route

Feature home:

`app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/maps/distribution/README.md`

VexMaps Distribution adds a second scale beneath the broader Maps Screen without transferring
Navigation ownership:

~~~text
Distribution Mode
    is Maps operating in the Distribution context?

durable Distribution Draft
    which delivery stops did the human enter, in what input order, with what return policy?

Preparation snapshot
    which exact Draft revision + origin + travel mode + routing region is being prepared?

Plan
    what route order is recommended for that exact snapshot?

Delivery Route
    what is truthfully happening while those deliveries are carried out?

Navigation
    what exact journey/route is the Navigation owner realizing?
~~~

The important separation is:

~~~text
DISTRIBUTION_MODE != DELIVERY_ROUTE
DRAFT != PLAN
PLAN != DELIVERY_ROUTE_STATE
DELIVERY_ROUTE_STATE != NAVIGATION_STATE
~~~

The process-lifetime `VexMapsDistributionSession` owns the active relationship/currentness among
those owners. `VexMapsDistributionExecution` owns coroutine custody only.

Relationship owners remain explicit:

~~~text
VexMapsDistributionModeDeliveryRouteConvergenceRelay
    Distribution Mode + Delivery Route -> coherence / bounded Mode Control request

VexMapsDeliveryRouteNavigationConvergenceRelay
    Delivery Route + Navigation + expected leg/attempt facts
    -> exact relationship / bounded request
~~~

Naming history and the cold-reader exercise that produced the current `Delivery Route` vocabulary
are preserved under the searchable tag `[Name Ontology]` in the feature-local design record.

## Accepted Distribution composition checkpoint

PR #86 has now crossed its full source lifecycle and is accepted on current `main`:

~~~text
PR = #86
merge = 28c80a8b2c89e0225427086685b6ceea8870e10c
tree = 179a34d2b43c670ec99b65b0d0dbf00acb5e648e

Fresh Independent Assurance = CLEAR_ON_EXACT_HEAD
currentized vexVerifyLocal = PASS
currentized physical-device preflight = PASS
currentized vexVerifyDevice = PASS
post-merge exact-tree verification = PASS
~~~

That checkpoint is important beyond the Distribution feature itself. It is the first substantial
product composition on `main` where these accepted roles coexist without being collapsed:

~~~text
Behavior
    local semantic question

Compound Relay / convergence
    relationship among independently-owned truths

Session
    lived relationship/currentness/failure/retry owner

VexExecution
    asynchronous execution custody

Projection / Compose
    presentation of already-resolved meaning
~~~

This is evidence for the repository feature-authoring grammar, not a requirement that every feature
create all five roles.

The full lived Delivery Route human walk remains a separate explicit proof debt. Connected Android
aggregate proof does not silently become end-to-end lived acceptance.

## Still intentionally unresolved

This topology pass does not silently settle behavioral questions that need their own proof:

- whether the durable checkpoint should graduate into explicit persisted viewport state;
- what age/accuracy policy should govern historical browse-context restoration;
- whether Context Dock depth gestures should move exclusively to the drag handle;
- whether a future second renderer earns a provider-neutral rendering contract.

Those are next-edge questions, not reasons to muddy the structural split.

<!-- [VXG RealForever][VEXLIFE-ANDROID][VEXMAPS][FEATURE-TOPOLOGY] -->
