# VexMaps — Feature Home

[VXG RealForever]

## Feature identity

~~~text
featureRef = feature.vexmaps
currentName = VexMaps
current completion effort = [VexMaps Complete]
code navigation = VexMapsDocs
~~~

Current intended experience:

~~~text
design/README.md
~~~

Meaningful design refinements:

~~~text
design/records/
~~~

## Human outcome

> A person opens VexMaps because they want to understand where they are, find somewhere they care
> about, choose how to get there, preview the route, and navigate there — without first learning
> provider setup, importing data packs, or knowing hidden map gestures.

Offline travel, Distribution, style choice, and provider preparation are important capabilities, but
they are secondary to that ordinary Maps outcome.

## Current status

~~~text
architecture / ownership foundation = CURRENT / ACCEPTED
map rendering                    = CURRENT
precise device location          = CURRENT
navigation session/guidance      = CURRENT
offline place-search machinery   = CURRENT
offline Valhalla routing         = CURRENT
Distribution / Delivery Route    = CURRENT
place-search currentness owner   = CURRENT

ordinary connected-user search   = NOT YET PRESENT
ordinary connected-user routing  = NOT YET PRESENT
obvious "find -> route -> start"  = NOT YET PRESENT

product experience state =
FOUNDATION-CURRENT / HUMAN-NAVIGATION-INCOMPLETE
~~~

The distinction is deliberate:

~~~text
FOUNDATION-CURRENT
!=
PRODUCT-COMPLETE
~~~

PR #86 established Distribution architecture and proof. PR #117 established place-search currentness
outside Compose. Those are valid completed effects, but neither means the ordinary first-use Maps
journey is complete.

## What a human can do now

### Map / location

~~~text
open VexMaps
-> live MapLibre/OpenFreeMap map renders
-> browse without location permission
-> request precise device location when needed
-> current fix may be projected on the map
~~~

A stored historical checkpoint may seed browse context but is not promoted into fresh current
location or navigation readiness.

### Destination selection

Current ordinary destination selection is limited:

~~~text
long-press map
-> choose/pin route destination
~~~

Local place search exists, but it is an offline capability:

~~~text
hamburger
-> Offline maps
-> Advanced imports
-> import local place-search SQLite pack
-> "Search offline"
-> select result
~~~

There is no accepted online place-search provider in VexMaps today.

### Routing

Current route planning is local/offline only:

~~~text
import local Valhalla routing region
+ choose origin
+ choose destination
+ choose travel mode
-> calculate route on device
~~~

There is no accepted online route provider in VexMaps today.

### Navigation

Once a route is prepared, VexMaps already owns meaningful navigation infrastructure:

~~~text
prepared route
-> VexMapsNavigationProcess / VexNavigationSession
-> exact route-relative location matching
-> guidance / off-route / location-held / arrival truth
-> foreground Android Navigation Service
~~~

The missing product front door is therefore not "build navigation from zero." It is the ordinary
human path that gets a destination and route into that accepted navigation owner.

### Distribution

Distribution is an accepted advanced Maps capability:

~~~text
Distribution Mode
-> durable delivery Draft
-> optimized Plan
-> Delivery Route
-> exact Navigation legs
~~~

Feature home:

~~~text
distribution/README.md
~~~

Distribution must not become the default explanation of what Maps is for.

## Current UI truth

The current rendered screen emphasizes:

~~~text
map
+ current-location affordance
+ Context Dock pages for Distribution / online styles / other contextual surfaces
+ hamburger -> Offline maps
~~~

The offline planner contains place search, route inputs, travel mode, offline-map import and routing
region import.

That source shape explains the current lived screenshots: the user can see a map and Distribution
surface while the basic "Where to?" journey is not visually discoverable.

## Current source / ownership map

~~~text
VexMapsScreen
    stable Screen/action identity

VexMapsMapSurface
    native/custom Maps composition

VexMapsRepository
    durable Android-local Maps preferences/checkpoint

VexMapsStyleConvergenceRelay
    selected/requested/loaded/failed style relationship

VexMapsPlaceSearchStore
    local SQLite place-pack custody/provider mechanics

VexMapsPlaceSearchSession
    current local pack/search attempt/result admission

VexMapsOfflineRoutePlanner
    local Valhalla provider lifetime + route execution

VexNavigationSession / VexMapsNavigationProcess
    one prepared/active journey and navigation currentness

VexMapsNavigationService
    Android foreground lifetime + continuous device-location delivery

VexMapLibreAdapter
    Maps rendering meaning -> MapLibre operations

VexMapsDistributionSession
    Distribution lived relationship/currentness

VexMapsDistributionExecution
    Distribution coroutine custody only
~~~

Repository-wide ownership topology:

~~~text
docs/foundation/VEXMAPS-FEATURE-TOPOLOGY.md
~~~

## Existing foundations reused

~~~text
Elements / VexCompose
    stable semantic UI identity

Behavior
    local State / Control / Act / Effect questions where earned

Relay / Compound Relay
    relationships among independently owned truths

VexExecution
    explicit coroutine custody

Journey / Test Foundation
    human walk + focused/render/native proof at rightful owner

DesignHistory / Name Ontology
    preserve why names/owners/experience changed
~~~

No foundation is added merely because VexMaps needs another screen state. Product pressure must earn
the abstraction.

## Provider boundary

Current online capability:

~~~text
OpenFreeMap
    map vector/style delivery only
~~~

Do not infer from a live online map that VexMaps also has online:

~~~text
place search
routing
navigation backend
~~~

Those are separate capabilities.

Current local/offline capability:

~~~text
place search -> prepared local SQLite pack
routing      -> local Valhalla region pack
map offline  -> imported MapLibre offline database
~~~

The [VexMaps Complete] design requires ordinary online search/routing when connected, but the exact
provider is an implementation/provider decision. Do not hardcode a community public endpoint into
VexMaps architecture merely to make a demo work.

## Current proof map

~~~text
pure state/currentness
    focused JVM tests

provider translation / route matching
    focused JVM/provider tests where pure

Compose semantics
    render/instrumentation proof when changed

MapLibre / permissions / Service / physical location
    Android/device proof

ordinary human "find -> route -> start -> navigate"
    Journey + physical/lived product proof once the UI exists

coverage
    omission/discovery signal, never product-completion authority
~~~

## Current holds

~~~text
HELD
    exact online place-search provider
WHY
    completed UX requires the capability, but provider choice must satisfy availability,
    policy/privacy/currentness requirements rather than being baked into UI architecture

HELD
    exact online route provider
WHY
    current Navigation should consume an admitted route without depending on one provider;
    first real online provider must earn any shared route-provider contract

HELD
    automatic online/offline fallback abstraction
WHY
    one local provider family exists today; wait until a real online consumer exposes the repeated
    semantic promise before creating a generic provider selector

HELD
    full lived Distribution Journey automation
WHY
    deterministic routing/location + honest Context Dock traversal are not yet available
~~~

## Current next edge

The active continuation is [VexMaps Complete].

Start from:

~~~text
design/README.md
~~~

Do not resume by refactoring another coroutine merely because VexMapsMapSurface is large.

The next implementation should make measurable progress on the human completion path:

~~~text
open Maps
-> find destination
-> select destination
-> preview route
-> start navigation
~~~

and let the real feature need decide which route/search currentness or provider abstractions are
actually earned.

<!-- [VXG RealForever][feature.vexmaps][VEXMAPS-COMPLETE][FEATURE-HOME] -->
