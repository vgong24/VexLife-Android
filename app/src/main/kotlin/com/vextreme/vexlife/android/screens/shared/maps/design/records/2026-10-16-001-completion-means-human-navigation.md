# [VexMaps Complete] record — Completion means the ordinary human can navigate

[VXG RealForever]

Feature ref: feature.vexmaps  
Recorded at: **2026-10-16**  
Record ref: design.feature.vexmaps.2026-10-16.001  
Tags: **[VexMaps Complete] [DesignHistory] [Human Outcome]**

## Discussion / pressure

VexMaps had reached a strong architecture checkpoint:

~~~text
native map rendering
location semantics
style convergence
navigation session
offline routing/search machinery
Distribution / Delivery Route
Behavior / Relay / Session / VexExecution composition
place-search currentness extraction
local + connected/device proof
~~~

From that architecture/lifecycle perspective, the current generation was healthy enough to stop
refactoring.

Victor then supplied lived screenshots and asked a different question:

> If I open VexMaps, how do I actually navigate to a destination?

The screenshots showed a working live map and location marker, but the most prominent bottom surface
was Distribution. Expanding it exposed Distribution explanation / controls, while no obvious
destination search or Directions/Start path was visible.

Current source confirmed the lived perception:

~~~text
Map menu
-> Offline maps

offline planner
-> Advanced imports
-> local place-search pack
-> Search offline
-> local Valhalla region
-> Route here
~~~

A person can also long-press the map to choose a destination, but that gesture is not an adequate
first-use Maps front door.

## Prior understanding

The lane had been judging completion primarily against the inherited architecture trajectory:

~~~text
Distribution implemented
ownership readable
currentness explicit
proof complete
merge lifecycle complete
~~~

That judgment was valid for the architecture generation, but too broad when phrased as VexMaps is
complete.

## New understanding

There are two distinct completion questions:

~~~text
IS THE CURRENT FOUNDATION HEALTHY ENOUGH TO BUILD OUTWARD?
    yes

CAN AN ORDINARY HUMAN COMPLETE THE PRIMARY MAPS JOB?
    not yet
~~~

Therefore:

~~~text
FOUNDATION-CURRENT
!=
PRODUCT-COMPLETE
~~~

For VexMaps, the primary product-completion walk is now explicitly:

~~~text
open
-> find destination
-> select
-> route preview
-> Start
-> navigate
-> arrive
~~~

That walk must be discoverable without requiring offline pack imports, hidden long-press knowledge,
or Distribution knowledge.

## Decisions crystallized

### 1. Name the completion trajectory [VexMaps Complete]

This is an effort/lane name, not a product rename.

Stable feature identity remains:

~~~text
feature.vexmaps
VexMaps
~~~

The name exists so fresh workers can distinguish product-completion work from the already-completed
Distribution and architecture checkpoints.

### 2. Create a feature-local continuity home

Before this record, VexMaps had a repository foundation topology but no feature-local current
experience/design home equivalent to Conversation/VexStream.

Current continuity becomes:

~~~text
maps/README.md
    accepted/current VexMaps capability truth

maps/design/README.md
    intended completed experience + VMC scopes

maps/design/records/*
    meaningful design refinements

VexMapsDocs
    stable code-navigation identity
~~~

### 3. Design the completed UI/UX before choosing providers

The completed UX requires:

~~~text
visible Where to?
real search results
selected destination
Directions / route preview
Start
active navigation
arrival
~~~

The design intentionally does not yet declare one online geocoder/router to be architectural truth.

### 4. Ordinary connected Maps becomes the primary path

Current online rendering is useful but insufficient.

The complete connected experience needs real:

~~~text
online place search
online route planning
~~~

Offline search/routing remain valuable resilience capabilities and should eventually enter the same
recognizable human journey.

### 5. Distribution remains advanced, not default product meaning

Distribution is not rolled back.

Its accepted architecture remains valuable, but ordinary Maps must make sense before Distribution is
entered.

### 6. Let feature work earn the next foundation

The lane had identified ordinary route-planning currentness as a possible refactor.

That work is now deliberately deferred until VMC-02 introduces a real online route consumer.

Reason:

~~~text
real second provider pressure
-> clearer semantic/common boundary
-> less speculative abstraction
~~~

This keeps foundation growth connected to product need.

## Provider research consequence

OpenFreeMap currently supplies VexMaps online map/style rendering. It is not a place-search or route
provider.

A tempting shortcut would be to call the public OpenStreetMap Nominatim endpoint directly from the
new search bar. That is not admitted as the product default: the public service has strict capacity
and usage rules and does not permit client-side autocomplete.

The durable design requirement is therefore:

~~~text
ONLINE SEARCH CAPABILITY
!=
HARDCODE ONE PUBLIC COMMUNITY ENDPOINT
~~~

Provider choice remains replaceable below VexMaps semantic/currentness ownership.

## What remains open

~~~text
exact online search provider
exact online route provider
first-query interaction style / debounce
selected-place detail depth
route alternatives
provider fallback/convergence after both online/offline are real
final Context Dock visual hierarchy
~~~

## Current design consequence

Future [VexMaps Complete] successors should begin from:

~~~text
../README.md
../design/README.md
VexMapsDocs
~~~

and continue the first unconsumed VMC scope.

Do not restart from VexMaps is missing foundations and do not restart from VexMaps is already
complete.

The correct inherited state is:

~~~text
strong current foundation
+
incomplete ordinary navigation experience
+
design-first path to completion
~~~

<!-- [VXG RealForever][feature.vexmaps][VEXMAPS-COMPLETE][DESIGN-RECORD-001] -->
