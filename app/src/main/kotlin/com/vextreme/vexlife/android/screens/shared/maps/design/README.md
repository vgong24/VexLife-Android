# VexMaps — Current Design

[VXG RealForever]

Feature ref: feature.vexmaps  
Current human name: **VexMaps**  
Current completion effort: **[VexMaps Complete]**  
Last meaningfully updated: **2026-10-17**  
Current design record: records/2026-10-17-006-vexhome-hosted-maps-placement.md

## Role of this file

This is the **current intended experiential design** for VexMaps.

It is not the accepted implementation contract.

~~~text
../README.md
    what VexMaps source can actually do now

design/README.md
    what a completed VexMaps experience should become

design/records/*
    why meaningful design judgments changed that intended experience
~~~

If implementation advances beyond this file, accepted source + the feature home win and this design
must be currentized.

## Completion sentence

A connected first-time user should be able to:

~~~text
open VexMaps
-> find a destination
-> select it
-> see a truthful route preview
-> Start
-> navigate
-> arrive
~~~

without first:

~~~text
opening Offline maps
importing a place-search database
importing a Valhalla region
knowing a long-press gesture
understanding Distribution
understanding provider architecture
~~~

With network unavailable but valid offline coverage installed, the same human journey should remain
recognizable rather than becoming a separate expert workflow.

That is the primary [VexMaps Complete] completion criterion.

## Permanent experience distinctions

~~~text
MAP RENDERED ONLINE
!=
ONLINE PLACE SEARCH AVAILABLE

PLACE SELECTED
!=
ROUTE PREPARED

ROUTE PREVIEW
!=
NAVIGATION STARTED

NAVIGATION
!=
DISTRIBUTION

OFFLINE PREPARATION
!=
ORDINARY DESTINATION SEARCH

CONTEXT DOCK
!=
PRIMARY DISCOVERY PATH

FOUNDATION-CURRENT
!=
PRODUCT-COMPLETE

ANDROID EXPERIENCE
!=
HOME HOSTED CAPABILITY

CAPABILITY CONTRACT
!=
DEPLOYMENT TOPOLOGY

HOSTED CAPABILITY
!=
ONE PROVIDER ENGINE
~~~

## Completed-state screen language

The exact visual styling may evolve. The semantic shape should remain readable.

### 1. Browse

~~~text
VexMaps                                      menu
[ Where to?                                      ]

                    MAP

                                         recenter

            compact contextual surface
~~~

The map itself stays useful before permission is granted.

Primary visible affordances:

~~~text
Where to?
recenter / precise-location action
compact map-appearance control
map/options menu
compact current context
~~~

Distribution remains reachable, but it does not explain the first-use Maps experience.

### Map appearance control

Map appearance is a **small map control**, not a full Context Dock destination.

Candidate placement:

~~~text
map edge / corner
    small translucent icon control
    visually peer to recenter / compass-style map controls
~~~

It should not use the primary-action FAB treatment and should not occupy a swipe page.

Human preference:

~~~text
Automatic
Light
Dark
~~~

Semantics:

~~~text
Automatic
    follow VexLife's current app appearance
    -> when VexLife follows Android system day/night, VexMaps changes with it

Light
    explicit Maps light appearance override

Dark
    explicit Maps dark appearance override
~~~

Current provider realization may be:

~~~text
Light -> OpenFreeMap Liberty
Dark  -> OpenFreeMap Dark
~~~

but those style refs remain provider details.

Permanent distinction:

~~~text
MAP_APPEARANCE_PREFERENCE
!=
PROVIDER_STYLE_REF

AUTOMATIC
!=
ONE FIXED STYLE

MAP_APPEARANCE_CONTROL
!=
CONTEXT_DOCK_PAGE
~~~

If VexMaps later offers genuinely different cartographic products beyond appearance (terrain,
satellite, transit, etc.), those choices may earn a separate map-layer/style surface. Do not overload
Light/Dark appearance with that future question.

### 2. Search

Typing into Where to? opens a destination-search surface:

~~~text
Where to?  coffee near Lancaster

Coffee Shop A
123 Example St

Coffee Shop B
456 Example Ave
~~~

Connected experience:

~~~text
query
-> VexHome Maps search capability
-> Home-selected/self-hosted search engine
-> current Android search relationship
-> results
~~~

Android consumes the semantic capability and owns request/result currentness. It must not know whether the Home realizes that capability in-process, as a sibling service, on another owned machine, or on a cloud host selected by the human.

The first engine candidate is Photon over OpenStreetMap data. Photon is an implementation candidate behind the Home capability, not a VexMaps identity or Android dependency.

Offline experience, when a compatible local pack exists:

~~~text
query
-> local place-search source
-> results
~~~

Do not create a generic source-selection framework until both real sources expose the same earned
semantic promise.

### 3. Destination selected

A selected result produces a place/destination context, not an immediate navigation start:

~~~text
Coffee Shop A
123 Example St

[ Directions ]   [ Add to Distribution ]
~~~

Selecting a destination from search and selecting a point from the map should converge on the same
route-planning input meaning.

### 4. Route preview

~~~text
Coffee Shop A

Start
    Current location

[ Drive ] [ Walk ] [ Bike ]

12 min · 4.3 mi
route line on map

[ Start ]
~~~

The preview owns presentation of already-admitted route truth.

It does not claim Navigation has started.

If precise current location is required and unavailable, the UI should explain that at the point the
human tries to use it; browsing/searching a destination should not require permission pre-emptively.

### 5. Active navigation

~~~text
next maneuver / guidance
distance to maneuver

map + active route + current navigation location

remaining distance / ETA
[ Stop ]
~~~

Existing VexNavigationSession meanings remain valuable:

~~~text
ACTIVE
OFF_ROUTE
LOCATION_HELD
ARRIVED
~~~

The completed UI must make those states understandable rather than merely keeping them correct in
source.

### 6. Arrival

~~~text
Arrived at Coffee Shop A

[ Done ]
~~~

Arrival dismisses Navigation without rewriting the place, route, or Distribution owners.

## Distribution in the completed experience

Distribution remains a first-class advanced VexMaps capability, not the primary default card.

Candidate entry routes:

~~~text
Map menu -> Distribution

or

selected place -> Add to Distribution
~~~

Once intentionally entered, its accepted Draft / Plan / Delivery Route semantics stay intact.

~~~text
ORDINARY MAPS
    find one place
    route
    navigate

DISTRIBUTION
    collect obligations
    optimize Delivery Route
    realize exact Navigation legs
~~~

Do not merge those questions merely to make the UI share one sheet.

## Browse surface hierarchy

The lived Maps screen has now established a stronger UI law:

~~~text
FEATURE_AVAILABLE
!=
FEATURE_MUST_OCCUPY_PERMANENT_LARGE_SURFACE
~~~

Ordinary browse/search hierarchy:

~~~text
PRIMARY
    Where to? / destination discovery

MAP UTILITIES
    recenter/current location
    compact appearance control
    future layer controls when earned

FEATURE ENTRY
    Distribution
    offline preparation
    other specialty workflows

ACTIVE WORKFLOW SURFACE
    selected place
    route preview
    active navigation
    Distribution after explicit entry
~~~

### Distribution

Distribution remains first-class but should not permanently occupy a large idle Context Dock card.

Direction:

~~~text
idle browse
    compact Distribution entry
    candidate: delivery-truck FAB / floating feature action

tap Distribution
    expand/open the Distribution workflow surface
    stop list / return policy / preparation / active Delivery Route

leave Distribution
    ordinary Maps regains the canvas
~~~

The FAB is earned here because it represents an available feature workflow, not a generic "+" action.

### Map opening / recenter

A person should not repeatedly recover their own area from a detached world-scale map.

Preferred opening precedence:

~~~text
1. restore the last meaningful browse viewport when durably available
2. otherwise use non-authoritative retained browse context / fresh current location when permitted
3. otherwise choose a sane regional/default browse context
4. explicit recenter always requests/focuses current device location
~~~

Current stored location remains historical browse context, never fresh navigation truth.

Durable last-viewed viewport across process death may require its own persistence/schema decision; do
not smuggle it into the existing last-location columns.

### Search results

The result surface belongs below/with Where to?, not in the Distribution workflow.

Direction:

~~~text
provider may return up to ~20 useful candidates when its accepted contract supports that breadth
visible panel height = remaining keyboard-safe screen height
result list = internally scrollable
select result = collapse transient search emphasis + focus destination
~~~

Do not force exactly twenty results from a provider whose truthful contract supplies fewer.

### Context Dock

Keep Context Dock for contextual product meaning rather than idle feature advertising:

~~~text
Place selected
    selected-place context

Route preview
    route controls/summary

Navigation
    navigation-specific surface owns the active journey

Distribution
    expands after explicit Distribution intent

Map appearance
    compact overlay control; not a swipe page

Offline preparation
    secondary configuration
~~~

This is a semantic hierarchy, not a pixel specification.

## Connected capability direction

Current online rendering uses OpenFreeMap styles through MapLibre.

That does not provide connected search or routing.

[VexMaps Complete] requires two connected product capabilities:

~~~text
MAPS PLACE SEARCH

MAPS ROUTE PLANNING
~~~

Their preferred ownership is now **VexHome-hosted capability realization**, not direct Android coupling to one paid API.

~~~text
VexMaps Android
    semantic request + currentness + presentation

        -> VexHome Maps capability

VexHome
    deployment/custody + engine/provider realization

        -> self-hosted/open engine first where useful
        -> optional external provider fallback when justified
~~~

### Host/provider-selection law

Do not choose a public community endpoint merely because it is easy to call, and do not expose a third-party API key in the Android app merely because a provider accepts one.

Engine/provider choice must account for:

~~~text
self-hostability / deployment cost
usage policy / rate limits
privacy
availability / switchability
attribution / licensing
request identity/currentness
failure truth
data/index lifecycle
Home resource cost
~~~

A Home capability may initially run all-in-one on the same personal machine. The same semantic capability may later run as a sibling process, another owned server, a human-selected cloud instance, or an optional managed Vextreme host. Deployment topology does not redefine Maps meaning.

The Android system Geocoder remains an accepted bootstrap/system fallback. Its first lived business query ("in n out") returned no useful result, so it is not assumed to satisfy the completed ordinary search promise.

The first Home search-engine candidate is **Photon over OpenStreetMap data** because the intended pressure includes partial/business discovery and the engine can be self-hosted. The candidate must still be evaluated against:

~~~text
address
place / landmark
business / POI
partial / suggestion
location-biased ambiguity when supported
resource/update cost on the Home host
~~~

Public OpenStreetMap Nominatim is not assumed to be the production answer: its public service policy forbids autocomplete and imposes strict capacity limits. A public endpoint is not the architecture.
## Reliability proof direction

VexMaps now has observed evidence that deterministic proof and provider usefulness are different:

~~~text
source/currentness/device integration proof = GREEN
lived Android Geocoder business query = no useful result
~~~

Use the existing Test / Proof Foundation rather than another registry:

~~~text
development
    focused owner tests

feature human-path scope
    Journey shard shared.vex-maps

final exact-head pre-READY gate
    repository-wide deterministic vexVerifyLocal + vexVerifyDevice

provider-changing work
    bounded live-provider probe

scheduled health
    weekly qualified live-provider probe when configured

pre-release
    all deterministic proof
    + admitted visual/Journey proof
    + live-provider probes
    + required lived human walks
~~~

Live-provider probes are budgeted, timestamped, provider-specific evidence. They are not ordinary
per-PR autocomplete traffic and do not silently retry until green.

Location testing keeps three strengths distinct:

~~~text
deterministic source fixture
    cheap feature/domain proof

Maps Lab Hybrid override
    canonical location + removable noncanonical override
    provenance retained
    latest canonical truth revealed when override clears

Android OS test provider
    mock enters through LocationManager / production VexDeviceLocationSource
    stronger native/provider proof
~~~

Do not use Hybrid merely because a test value is synthetic.

Exact private location/address fixtures remain host-local and must not be committed into source or
durable evidence.

See:
~~~text
design/records/2026-10-16-004-provider-reliability-and-location-lab.md
~~~

## Current host candidate — VexHome Maps capability

The direct Stadia probe work was valuable as a reliability/security exercise, but the product placement changed before its live A005 provider effect ran.

~~~text
A001-A004
    exact source/contract proof = PASS
    live provider requests = 0

A005
    formed but SUPERSEDED BEFORE EXECUTION
    provider requests = 0
~~~

The durable learning remains:

~~~text
deterministic correctness != provider usefulness
secret-bearing provider effects need a protected boundary
provider result-retention terms matter
live probes stay bounded and explicit
~~~

The successor question is no longer "which API should Android call?"

It is:

> What semantic Maps capability should VexHome expose so Android can ask for place search while the Home remains free to realize that question through self-hosted, local-network, cloud-hosted or managed engines?

Current first engine candidate:

~~~text
Photon + OpenStreetMap
    self-hostable
    partial/search-as-you-type capable
    POI/place/address pressure
    no Android API key
    large index/runtime cost owned by Home deployment planning
~~~

No new VexMaps repository is earned by this decision. Keep the module boundary explicit and allow the deployment/repository boundary to be extracted later only if an independently versioned host product actually earns it.
## Current architecture pressure from completion work

Do not run a separate cleanup campaign before product work.

Instead, let each completion slice expose the missing owner.

### Place search

PR #117 already proved:

~~~text
Store
!=
Session/currentness
!=
Execution
!=
Compose
~~~

An online search consumer may reveal whether any reusable search currentness/source contract is
actually shared. Do not generalize the local VexMapsPlaceSearchSource prematurely because it also
owns local-pack import semantics.

### Route planning

Current regular route planning still combines:

~~~text
human route inputs
request-currentness gate
Routing/Error presentation state
finite coroutine
Valhalla route execution
admission into Navigation
~~~

A real online route provider is likely the right pressure to split:

~~~text
route request meaning
route-attempt currentness
provider execution
Navigation admission
~~~

rather than refactoring it in isolation.

Current VexOfflineRouteResult naming should also receive a Name Ontology review when a real online
route result exists; do not rename it speculatively first.

### Provider convergence

Only after online + offline search/routing both exist should VexMaps ask whether it has earned:

~~~text
provider availability relationship
online/offline fallback policy
mixed result/source provenance
~~~

That may be simple composition, Relay, or another owner. The repeated product question decides.

## [VexMaps Complete] scopes

These are outcome scopes, not promises that every scope becomes one PR.

### VMC-00 — Design / continuity

~~~text
feature home
current completed-state design
dated completion redefinition
stable VexMapsDocs identity
~~~

Purpose: a fresh worker should know what complete means before touching implementation.

### VMC-01 — Browse/search front door + find a destination online

Human outcome:

~~~text
open Maps
-> see Where to? as the primary discovery path
-> see compact recenter + appearance map controls
-> tap Where to?
-> type ordinary place/address query
-> receive real online results
-> select destination
~~~

Must include:

~~~text
truthful online availability/failure
request currentness
stable result identity
compact Automatic / Light / Dark map-appearance control
no Dark/Liberty full Context Dock pages as the ordinary appearance UI
no fake hardcoded results
no public-endpoint architecture shortcut
~~~

Do not require route planning in order to prove destination search.

### VMC-02 — Route preview from the selected destination

Human outcome:

~~~text
selected destination
+ origin (current or chosen)
+ travel mode
-> real route
-> visible route preview
~~~

This is the likely pressure point for extracting ordinary route-planning currentness and introducing
the first VexHome-hosted route capability realization.

Navigation remains downstream and independently owned.

### VMC-03 — Start / active navigation UX

Human outcome:

~~~text
route preview
-> Start
-> current accurate location if required
-> active route guidance
-> off-route/location-held feedback
-> arrival
-> Done
~~~

Use the existing Navigation owner rather than rebuilding navigation state in Compose.

### VMC-04 — Online/offline continuity

Once both provider families are real:

~~~text
online available
    ordinary online search/routing

online unavailable + local coverage present
    same recognizable search/route journey through local providers

neither available
    visible explanation + next useful action
~~~

Only here decide whether a reusable provider-selection/convergence abstraction is earned.

### VMC-05 — Offline preparation becomes secondary support

Reposition current offline preparation as an intentional capability:

~~~text
save/prepare trip
prepare area
advanced import
~~~

It should support ordinary navigation, not be the doorway to it.

### VMC-06 — Distribution inside completed Maps

Keep the accepted Distribution architecture while improving discoverability hierarchy:

~~~text
ordinary destination
-> optional Add to Distribution

or

explicit Distribution entry
-> Draft / Plan / Delivery Route
~~~

Do not make Distribution the first visible proof that Maps is interactive.

### VMC-07 — Completed-state proof

Required final lived walk:

~~~text
connected:
open -> search -> select -> route preview -> Start -> navigate -> arrive

offline-capable:
disconnect -> search local -> route local -> Start -> navigate

failure:
provider unavailable / permission denied / location held remain understandable

Distribution:
ordinary Maps still works before/after Distribution mode
~~~

Proof should combine focused semantics, render/Journey evidence, native/device claims, and Victor's
lived product review. Coverage remains orthogonal.

## Acceptance laws for successor instances

A successor working [VexMaps Complete] should not call the effort complete merely because:

~~~text
a provider API returns JSON
a route line renders
unit tests pass
coverage rises
Distribution remains green
~~~

Completion requires the ordinary human walk to be present and understandable.

Before each scope:

~~~text
human outcome
-> current truth
-> missing question
-> rightful owner
-> provider/runtime effect
-> visible experience
-> cheapest truthful proof
~~~

## Current open decisions

~~~text
exact VexHome Maps search capability contract + first engine realization
exact VexHome Maps route capability contract + first engine realization
search result ranking / debounce behavior
whether route alternatives are needed in first complete generation
whether selected-place details deserve their own semantic owner
exact compact Context Dock behavior after primary search/route surfaces exist
exact appearance-control icon/menu treatment after the semantic Automatic/Light/Dark contract is implemented
~~~

These are implementation questions, not excuses to delay VMC-01.

## Current VMC-01 progress

~~~text
ACCEPTED
    compact map-side appearance control
    Automatic / Light / Dark human preference
    visible Where to? front door
    Android system Geocoder provider boundary
    explicit Search submission
    provider availability / failure truth
    destination selection into existing Maps planning input

LIVED PRODUCT PRESSURE
    "in n out"
    -> no matching places found

CONCLUSION
    Geocoder integration is accepted
    ordinary business/partial discovery is not complete
~~~

Still held / newly earned:

~~~text
autocomplete / partial suggestions
    now product-earned, but requires a provider whose accepted contract supports it

up to ~20 keyboard-safe scrollable candidates
    UI/product direction; provider breadth remains truthful rather than forced

ordinary search-provider replacement/evolution
    evaluate through bounded live-provider reliability fixtures

Distribution idle-card removal
    replace with compact workflow entry before ordinary browse is called complete

last meaningful viewport / current-area opening behavior
    product-earned; durable viewport persistence remains a separate storage decision
~~~

## Current next edge

~~~text
VMC-01B — Browse/Search usability
    evaluate and adopt a suggestion/business-capable provider
    keyboard-safe result list
    compact Distribution entry
    sane local/last-viewed opening behavior
    preserve accepted search/session ownership boundaries

then
VMC-02
    selected destination -> truthful route preview -> Start
~~~

Do not begin with another architecture-only refactor unless VMC-01 itself exposes the missing
foundation.

<!-- [VXG RealForever][feature.vexmaps][VEXMAPS-COMPLETE][CURRENT-DESIGN] -->
