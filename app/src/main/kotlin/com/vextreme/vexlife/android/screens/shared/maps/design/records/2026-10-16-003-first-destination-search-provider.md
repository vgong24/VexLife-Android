# [VexMaps Complete] record — First destination search uses Android system geocoding

[VXG RealForever]

Feature ref: feature.vexmaps  
Recorded at: **2026-10-16**  
Record ref: design.feature.vexmaps.2026-10-16.003  
Tags: **[VexMaps Complete] [Destination Search] [Provider Boundary]**

## Pressure

The accepted VMC-01 design requires an obvious ordinary Maps path:

~~~text
Where to?
-> real place/address results
-> select destination
~~~

The first provider should make that path real without hardcoding one community demo endpoint or
requiring VexMaps UI/domain code to know a commercial provider contract.

## Provider choice

The first provider is Android's platform Geocoder.

Why:

~~~text
device/platform provider boundary already exists
no new API key or account required for the first vertical slice
supports named places and addresses
provider implementation can differ by Android device
VexMaps can observe availability through Geocoder.isPresent()
~~~

Android explicitly treats Geocoder as best-effort. A present implementation does not guarantee one
lookup will succeed, and results are not canonical place truth.

Therefore the product states remain explicit:

~~~text
Unavailable
Idle
Searching
NoResults
Results
Error
~~~

## Interaction choice

The first human interaction is explicit submit:

~~~text
type query
-> Search
-> one bounded provider request
~~~

Do not infer autocomplete semantics from a text field.

Autocomplete/search-as-you-type may be earned later by a provider that actually supplies that promise
with suitable usage/currentness behavior.

## Ownership

~~~text
Android Geocoder
    provider observation

VexMapsDestinationSearchSession
    submitted request/result currentness

VexMapsDestinationSearchOverlay
    Where to? query/result presentation

VexMapsMapSurface
    selected result becomes existing destination input
~~~

The session does not own the selected destination or route.

## Relationship to offline search

Current local SQLite search remains independently complete:

~~~text
VexMapsPlaceSearchStore
+ VexMapsPlaceSearchSession
~~~

Do not immediately merge the two sessions into a generic provider selector.

The second real provider is now enough to compare their repeated promises, but online/offline fallback
policy remains a later VMC-04 question.

## Permission boundary

Destination search does not request precise location permission.

Current location may later become ranking/bias input. Search itself must remain usable before a person
shares precise device location.

## Current consequence

VMC-01 may now be proven as:

~~~text
open VexMaps
-> see Where to?
-> submit ordinary place/address query
-> receive real system-provider results when available
-> select destination
-> existing destination marker/focus changes
~~~

This does not yet claim route preview or online routing.

<!-- [VXG RealForever][feature.vexmaps][VEXMAPS-COMPLETE][DESIGN-RECORD-003] -->
