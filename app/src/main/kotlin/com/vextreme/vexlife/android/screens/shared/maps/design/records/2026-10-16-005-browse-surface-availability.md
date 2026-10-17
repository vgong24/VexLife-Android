# [VexMaps Complete] record — Browse surface uses availability, not permanent feature weight

[VXG RealForever]

Feature ref: feature.vexmaps  
Recorded at: **2026-10-16**  
Record ref: design.feature.vexmaps.2026-10-16.005  
Tags: **[VexMaps Complete] [Browse UX] [Distribution] [Location] [Search]**

## Pressure

Lived VexMaps screenshots after the first VMC-01 slices showed several individually valid features
competing for first-class visual weight:

~~~text
Where to? search
large persistent Distribution card
location/recenter action
map appearance action
map itself
~~~

The result felt less like an ordinary map and more like several proof surfaces sharing one canvas.

A second pressure was map orientation: a person should not repeatedly recover their own area by
manually pressing location after opening Maps.

## New understanding

~~~text
FEATURE_IMPORTANCE
!=
PERMANENT_SURFACE_SIZE

FEATURE_AVAILABLE
!=
FEATURE_ALREADY_ENTERED
~~~

The default Maps screen should optimize for the ordinary human question:

> Where am I, and where do I want to go?

Specialty workflows remain quickly available without occupying the canvas before intent exists.

## Decisions

### Search owns the primary discovery surface

~~~text
top / primary:
    Where to?

typing/searching:
    suggestions/results below the field
    keyboard-safe bounded height
    internally scrollable
~~~

The completed provider/UI should support a useful breadth of candidates, with a product direction of
up to roughly twenty results when the provider truthfully supports that many.

Do not force a provider to manufacture twenty results.

### Map utilities remain lightweight

Candidate map-side utility stack:

~~~text
appearance
recenter/current location
future layer/utility controls when earned
~~~

These controls are visually available without becoming workflow panels.

### Distribution becomes an available workflow entry

Distribution remains a first-class feature, but idle browse should not permanently display its large
workflow card.

Candidate entry:

~~~text
delivery-truck floating feature action
~~~

Tap enters/expands the Distribution workflow.

Once entered, its Draft / Plan / Delivery Route controls may use a substantial bottom sheet because
the human has explicitly chosen that workflow.

This is an earned feature action, not a generic "+" FAB.

### Map opening should preserve local relevance

Preferred opening precedence:

~~~text
durable last meaningful browse viewport when available
-> otherwise retained non-authoritative browse context / fresh current location when permitted
-> otherwise sane regional/default context
~~~

Explicit recenter requests/focuses current device location.

Stored historical location remains browse context only and cannot satisfy navigation readiness.

Durable viewport restoration across process death may require an explicit persistence/schema
decision. Do not overload the current stored-location row with hidden viewport semantics.

### Search does not require precise location permission

A person can search for a destination before sharing precise current location.

Location may later improve ranking/proximity when the accepted search-provider contract supports it.

## Consequence

Ordinary browse should tend toward:

~~~text
VexMaps
[ Where to? ]

                map

          appearance
          recenter

                         Distribution entry
~~~

rather than:

~~~text
map
+ permanently expanded specialty workflow
+ giant provider/configuration surfaces
~~~

Selected-place, route-preview, active-navigation and explicitly entered Distribution state may each
earn larger contextual surfaces.

## Relationship to reliability work

A future suggestion/business-search provider may consume an effective location bias.

That does not authorize hardcoding Victor's private location into tests or source.

Provider reliability and location-testing rules live in:

~~~text
2026-10-16-004-provider-reliability-and-location-lab.md
~~~

## Current consequence

VMC-01B is the active ordinary Maps usability pressure:

~~~text
forgiving search/suggestions
keyboard-safe scrollable results
compact Distribution entry
sane local/last-viewed opening behavior
~~~

VMC-02 starts only after selecting a destination feels like a normal Maps action rather than a
developer/provider setup exercise.

<!-- [VXG RealForever][feature.vexmaps][VEXMAPS-COMPLETE][BROWSE-AVAILABILITY][DESIGN-RECORD-005] -->
