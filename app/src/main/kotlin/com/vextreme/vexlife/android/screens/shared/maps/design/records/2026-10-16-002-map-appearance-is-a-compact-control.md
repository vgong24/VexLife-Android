# [VexMaps Complete] record — Map appearance is a compact control

[VXG RealForever]

Feature ref: feature.vexmaps  
Recorded at: **2026-10-16**  
Record ref: design.feature.vexmaps.2026-10-16.002  
Tags: **[VexMaps Complete] [DesignHistory] [Map Appearance] [Human Outcome]**

## Discussion / pressure

Victor's lived screenshots showed that VexMaps currently gives each online map style a full
swipeable Context Dock page.

The current provider styles are:

~~~text
Dark
Liberty
~~~

The source is internally coherent:

~~~text
VexMapsOnlineStyle
-> Dark / Liberty provider style refs
-> VexMapsStyleConvergenceRelay
-> MapLibre apply/load/failure relationship
~~~

and the default style already maps the current app dark/light Boolean to Dark or Liberty.

The UX problem is different: provider style identity is being presented as if it were a primary
human Maps context.

## Prior design

The first [VexMaps Complete] design said only:

~~~text
Styles / offline preparation
    secondary configuration
~~~

That was directionally correct but not specific enough to prevent a successor from keeping Dark /
Liberty as swipeable Context Dock pages.

## New understanding

The human question is:

> How should the map look right now?

not:

> Which provider style ref should MapLibre load?

Therefore:

~~~text
MAP_APPEARANCE_PREFERENCE
!=
PROVIDER_STYLE_REF
~~~

The primary appearance choices should be:

~~~text
Automatic
Light
Dark
~~~

### Automatic

Automatic follows VexLife's current app appearance.

If VexLife is following Android system day/night, VexMaps changes with the app.

VexMaps should not independently invent a time-of-day scheduler when the app already owns appearance.

### Light / Dark

Light and Dark are explicit VexMaps overrides.

The current OpenFreeMap realization may map:

~~~text
Light -> Liberty
Dark  -> Dark
~~~

but that mapping remains replaceable provider detail.

## Presentation decision

Ordinary map appearance should use a compact overlay map control:

~~~text
small translucent icon
near a map edge/corner
peer to recenter / compass-style controls
tap -> compact appearance chooser
~~~

It should not:

~~~text
occupy a full Context Dock page
look like a primary FAB
require swiping contextual cards
expose provider style names as the human preference model
~~~

## Relationship to Context Dock

Context Dock remains useful for contextual product meaning:

~~~text
selected place
route preview
Distribution after explicit entry
other earned contextual surfaces
~~~

Map appearance is not that kind of context. It is a lightweight map-view preference.

## Future cartographic styles

This decision does not claim that all future map-style choices collapse into Light/Dark.

A future capability such as:

~~~text
terrain
satellite
transit
accessibility-focused cartography
~~~

may answer a different human question and earn its own map-layer/style surface.

Do not pre-encode that future into the current appearance control.

## Current design consequence

VMC-01 now owns the first visible Browse shell as well as online destination search:

~~~text
Where to?
recenter
compact Automatic / Light / Dark appearance control
real online search results
~~~

The existing Style Relay may remain the apply/load/failure relationship, but the product-facing
appearance preference should be introduced above provider style refs rather than turning Dark /
Liberty into permanent UX vocabulary.

<!-- [VXG RealForever][feature.vexmaps][VEXMAPS-COMPLETE][DESIGN-RECORD-002] -->
