# VexMaps — Current Design

[VXG RealForever]

Feature ref: feature.vexmaps  
Current human name: **VexMaps**  
Current completion effort: **[VexMaps Complete]**  
Last meaningfully updated: **2026-10-16**  
Current design record: records/2026-10-16-003-first-destination-search-provider.md

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
-> online place-search source
-> current search relationship
-> results
~~~

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

## Context Dock direction

The current Context Dock is useful infrastructure, but the lived screenshots show that a large
Distribution/style card can occupy more attention than the basic Maps task.

Direction:

~~~text
Browse
    dock may stay compact / contextual

Place selected
    dock may project selected-place context

Route preview
    dock may project route controls/summary

Navigation
    navigation-specific surface owns the active journey

Distribution
    dock expands after explicit Distribution intent

Map appearance
    compact overlay control; not a swipe page

Provider/cartographic style details
    secondary map options only when the human question is genuinely broader than appearance

Offline preparation
    secondary configuration, not the primary first-use explanation
~~~

This is a semantic hierarchy, not a pixel specification.

## Online capability direction

Current online rendering uses OpenFreeMap styles through MapLibre.

That does not provide online search or routing.

[VexMaps Complete] requires two new product capabilities:

~~~text
ONLINE PLACE SEARCH

ONLINE ROUTE PLANNING
~~~

The UI should depend on their semantic results, not one vendor.

### Provider-selection law

Do not choose a public community endpoint merely because it is easy to call.

Provider choice must account for:

~~~text
usage policy / rate limits
privacy
availability / switchability
attribution / licensing
request identity/currentness
failure truth
VexHome/self-hosted future
~~~

A first real online provider may be hosted/self-hosted and still remain replaceable below VexMaps
semantic meaning.

Public OpenStreetMap Nominatim is not assumed to be the production answer: its public usage policy
forbids client-side autocomplete and imposes strict capacity limits. That policy pressure is evidence
for keeping provider choice below the VexMaps experience contract.

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
the first online route provider.

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
exact online place-search provider
exact online route provider
search result ranking / debounce behavior
whether route alternatives are needed in first complete generation
whether selected-place details deserve their own semantic owner
exact compact Context Dock behavior after primary search/route surfaces exist
exact appearance-control icon/menu treatment after the semantic Automatic/Light/Dark contract is implemented
~~~

These are implementation questions, not excuses to delay VMC-01.

## Current VMC-01 progress

~~~text
DONE IN FIRST SLICE
    compact map-side appearance control
    Automatic / Light / Dark human preference
    Automatic follows VexLife app appearance
    Dark / Liberty removed from ordinary Context Dock pages
    legacy provider-style storage does not invent explicit human preference

CURRENT VMC-01 CANDIDATE
    visible Where to? front door
    Android system Geocoder provider
    explicit Search submission
    provider availability / failure truth
    real place/address results
    destination selection into existing Maps planning input

STILL HELD
    autocomplete/search-as-you-type
        provider promise not yet earned
    hosted/self-hosted provider
        Android Geocoder is the first system-provider slice, not permanent provider lock-in
~~~

## Current next edge

~~~text
VMC-01
    prove the current Where to? / Android Geocoder / destination-selection vertical slice
    on source + connected Android + lived device behavior

then
VMC-02
    use selected destination to earn route-preview + online route/currentness architecture
~~~

Do not begin with another architecture-only refactor unless VMC-01 itself exposes the missing
foundation.

<!-- [VXG RealForever][feature.vexmaps][VEXMAPS-COMPLETE][CURRENT-DESIGN] -->
