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
compact Automatic/Light/Dark map appearance = CURRENT IN VMC-01 SLICE

ordinary destination search      = SOURCE ACCEPTED / LIVED BUSINESS DISCOVERY INSUFFICIENT
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

The accepted VMC-01 source adds Android's best-effort system Geocoder behind a visible
`Where to?` front door. The integration/currentness boundary is accepted, but the first lived
business query (`in n out`) returned no useful result. Geocoder is therefore treated as a
bootstrap/system provider, not the completed ordinary VexMaps discovery provider.

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

The VMC-01 appearance slice removes OpenFreeMap Dark / Liberty from full swipeable Context Dock
pages and exposes one compact map-side control:

~~~text
Automatic
Light
Dark
~~~

Automatic follows VexLife's current app appearance; when the app follows Android system day/night,
the map follows with it. Current provider realization still maps Light -> OpenFreeMap Liberty and
Dark -> OpenFreeMap Dark, but those provider style refs are no longer the human preference model.

The existing nullable Room column is reused as storage encoding, not redefined as product semantics:
legacy provider-style bytes migrate conservatively to Automatic because old source persisted resolved
defaults and therefore cannot prove an explicit historical human choice.

The offline planner contains place search, route inputs, travel mode, offline-map import and routing
region import.

That source shape explains the current lived screenshots: the user can see a map and Distribution /
style surfaces while the basic "Where to?" journey is not visually discoverable.

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

live external/provider usefulness
    bounded opt-in provider reliability probe
    not every ordinary PR

ordinary human "find -> route -> start -> navigate"
    Journey + physical/lived product proof once the UI exists

coverage
    omission/discovery signal, never product-completion authority
~~~

## Current holds

~~~text
HELD
    completed ordinary search provider
WHY
    Android Geocoder is accepted as a bootstrap/system provider boundary but lived business discovery
    is insufficient; the next provider must satisfy partial/business usefulness plus
    policy/privacy/currentness/budget requirements

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

## Current hosted-capability placement decision

The direct paid-API candidate work established useful provider-proof laws, but it is no longer the active product direction.

~~~text
VexLife Android / VexMaps
    owns:
        human search experience
        request/result currentness
        destination selection
        Android fallback truth

        |
        | semantic Maps capability request
        v

VexHome
    owns:
        personal hosted capability realization
        provider/engine custody
        heavyweight indexes/data
        provider credentials when any are needed
        local-vs-remote deployment choice

        |
        +--> Photon / OpenStreetMap candidate engine
        +--> future admitted route engine
        +--> optional external provider fallback when justified
~~~

This does **not** mean the current narrow VEXHOME read-only observation contract already owns Maps. The Home-hosted capability boundary must be admitted explicitly in its rightful VexLife source.

It also does not earn a new VexMaps repository. Maps remains one VexLife capability with separate client and host realizations. The first deployment may be all-in-one on one VexHome machine; a later installation may place the same capability in another process, server or cloud host without changing the Android semantic contract.

The previously qualified Stadia A005 live probe is **SUPERSEDED BEFORE EXECUTION** by this placement decision. No Stadia live request was made and no provider key is required for the current edge.
## Current next edge

The active continuation is [VexMaps Complete].

Start from:

~~~text
design/README.md
~~~

Do not resume by refactoring another coroutine merely because VexMapsMapSurface is large.

VMC-01 source/currentness is accepted, but the lived Geocoder result showed ordinary discovery is
still incomplete.

The active product edge is:

~~~text
VMC-01B — Browse/Search usability

establish the VexHome-hosted Maps search capability boundary
-> realize the first self-hostable search engine candidate behind it
-> consume that capability from Android without provider-specific coupling
-> forgiving partial/business discovery
-> keyboard-safe scrollable candidate list
-> selected destination

plus browse hierarchy:
    compact Distribution entry instead of permanent large idle card
    recenter/current-area behavior that does not make the human recover from a detached world view
~~~

Photon/OpenStreetMap is the first engine candidate because it can be self-hosted and supports the human search pressure already observed. That engine choice remains below the capability contract.

Provider/engine candidates still require bounded reliability evidence before they become accepted ordinary Maps truth.

After ordinary Browse/Search is genuinely usable, the next unfinished product edge is:

~~~text
VMC-02
selected destination
-> truthful route preview
-> Start
-> navigation
~~~

See the current design and provider-reliability record before changing provider/test foundations.

<!-- [VXG RealForever][feature.vexmaps][VEXMAPS-COMPLETE][FEATURE-HOME] -->
