# Topology + ID protocol — readable construction and trace coordinates

[VXG RealForever]

> **BRIDGE DESIGN NOTE**
>
> This document captures the current intentionality behind the readable dependency topology and the
> UI-ID -> VexLogger -> possible VexSync/server projection. It is not a final wire protocol or a
> claim that current VexSync already implements the described server resolver.

## 1. Comparison lens: Koin and Vex are not aiming at the same center

Koin is useful comparison pressure, not architecture authority.

Current Koin guidance favors constructor injection for ordinary classes, while Android/framework
entry points may resolve dependencies through `inject()` / `get()`. Koin also supplies root,
Activity, Fragment, ViewModel, Service and custom scope behavior.

That solves the ordinary DI problem well:

~~~text
definition
-> scope/container resolution
-> constructor/object
~~~

The Vex direction overlaps mechanically:

~~~text
definitions/providers
-> dependency prerequisites
-> lifetime
-> memoized/reused instance
-> consumer
~~~

but the intended center is different:

~~~text
Koin-style priority
    resolve the correct object conveniently

Vex topology priority
    preserve the construction relationship as readable architecture
    while still resolving the correct object
~~~

Do not treat this as "Koin is bad" or "Vex must replace every DI capability".

The useful question is:

> Can a human or fresh instance understand why an object exists, what it depends on, who owns its
> lifetime and where it is consumed by reading the relationship declarations, without mentally
> executing a hidden container?

If yes, the Vex topology is providing something materially different from ordinary DI convenience.

Official Koin comparison references used while forming this note:
- Koin Core Injection: https://insert-koin.io/docs/reference/koin-core/injection/
- Koin Core Scopes: https://insert-koin.io/docs/reference/koin-core/scopes/
- Koin Android Entry Points: https://insert-koin.io/docs/reference/koin-android/entry-points/
- Koin Android Scopes: https://insert-koin.io/docs/reference/koin-android/scope/

## 2. Scale pressures already have separate rightful coordinates

Do not solve "scale" with one universal container ID.

The emerging Vex architecture already places different scaling questions at different owners:

~~~text
DEPENDENCY TOPOLOGY
    Which live object exists?
    What did it require?
    Which lifetime owns/reuses it?

    -> dependency relationship / lifetime coordinate


UI BLUEPRINT
    Which visible element acted?
    Which Screen owns it?
    Where is it structurally placed?

    -> screenId / elementId


FAMILY
    What semantic request was made?
    What State / Act / Effect meaning is lawful?

    -> Control / State / Effect


EXECUTION
    Which active semantic work relationship owns current Job(s)?

    -> RelationshipId -> Job(s)


LOGGER
    What accepted/observed facts happened and in what order?

    -> stable fact coordinates + chronology
~~~

This decomposition may remove many reasons a general DI container needs increasingly broad
qualifiers/scopes/lookup conventions: the semantic architecture is not asking the dependency system to
also become UI identity, runtime identity or historical traceability.

The dependency topology still needs correct lifetime/reuse behavior. It simply should not absorb
questions already answered elsewhere.

## 3. Existing Elements foundation already provides protocol-shaped UI identity

Current repository source already establishes:

~~~text
ScreenElement.screenId
    stable semantic Screen identity

UIElement.elementId
    stable semantic address used by:
        registry relationships
        logging
        test tags
~~~

The Elements foundation also derives nested IDs from Screen Blueprint structure.

Example from the current Elements README:

~~~text
Screen:
    screen.home

nested structure:
    element.home.body
    element.home.body.preview
    element.home.body.preview.title
    element.home.body.preview.message
~~~

This is important: the screen coordinate is already embedded in the ordinary nested element address.

`UIBlueprintRegistry` additionally indexes:

~~~text
screenId -> ScreenElement

elementId -> UIElement
elementId -> ElementPlacement
elementId -> parent UIElement
elementId -> owner Screen
elementId -> children
~~~

So the app already has a readable structural map. Logger does not need to mint another "button trace
ID" merely to identify which registered element was involved.

## 4. Click ID -> Logger: preserve source coordinate, do not collapse meaning

Victor's sketch pressure is:

~~~text
UI ButtonClicked
-> retrieve button ID
-> Logger
-> protocol/server interpretation
~~~

That is strongly compatible with the existing Elements identity law.

Refined interpretation:

~~~text
element.home.body.actions.save
    identifies WHERE / WHICH UI source acted

Control.Save(...)
    identifies WHAT semantic request was made

RelationshipId(...)
    identifies WHICH active runtime relationship when runtime work exists

VexLogger timestamp/order
    identifies WHEN / IN WHAT ORDER facts were accepted or observed
~~~

Do not force one of those coordinates to impersonate the others.

A useful causal sequence could therefore be represented conceptually as:

~~~text
source = element.settings.body.theme.dark
fact   = Clicked

semantic owner = ThemeModeFamily
control        = Select(Dark)
fact           = Accepted

effect         = Save(Dark)
fact           = Requested

fact           = SaveSucceeded

presentation   = Dark
fact           = Arrived
~~~

The exact VexLogger record schema is still held by #207. This document settles only the relationship
between the coordinates.

## 5. Do not parse English meaning out of strings when the Blueprint already owns structure

The dot-delimited `elementId` is useful protocol shape, but the app should not need a pile of ad-hoc
string parsing to rediscover relationships its Blueprint/registry already owns.

Preferred direction:

~~~text
UI action
-> stable elementId

local registry / exported blueprint map
-> exact owner Screen
-> exact structural path
-> exact element kind
-> stable symbolic identity
~~~

Then Logger may transmit/store the stable coordinate.

A downstream consumer can resolve it against the corresponding blueprint map.

This keeps:

~~~text
transport
    compact stable IDs

human view
    resolved names / labels / paths
~~~

instead of storing localized English text as the canonical trace.

## 6. VexSync/server-side interpretation opportunity

If VexSync can carry/export the same Blueprint identity map on both sides, a downstream service could
conceptually resolve:

~~~text
element.settings.body.theme.dark
~~~

into something like:

~~~text
Screen:
    Settings

Path:
    body -> theme -> dark

Kind:
    Toggle / Button / relevant registered kind

Symbolic owner:
    the Blueprint element declaration

Human projection:
    "Dark theme"
~~~

The exact mapping source is TO DISCUSS.

Possible sources include:

~~~text
shared Blueprint manifest
VexSync-exported registry projection
generated ID -> semantic-name map
versioned application/Blueprint description
~~~

Do not assume the server can safely infer the current English label from the ID string alone.

If human wording/localization is needed, resolve it from the matching blueprint/resource projection.

This gives a useful split:

~~~text
APP / PROTOCOL
    stable semantic coordinates

SERVER / DEBUG / VEXBOARD
    human-readable interpretation
    localization
    richer causal projection
~~~

The app does not need to bake English prose into every Logger event merely for remote readability.

## 7. Cross-version correctness must be explicit

A stable ID is only useful remotely if the receiver knows which identity map defines it.

Therefore a future VexSync wire design must answer:

~~~text
How does the receiver know the matching Blueprint/application identity map?
What happens if an element keeps its ID but its human label changes?
What happens if an ID is removed or moved?
What happens across localization?
~~~

Do not solve this prematurely by inventing a universal TraceId.

The likely need is **mapping currentness/provenance**, not execution occurrence identity.

Exact version/currentness representation remains TO DISCUSS and should be grounded in existing
VexSync/Blueprint contracts before implementation.

## 8. Human-readable reconstruction can remain downstream

The strongest consequence of this topology is that the Logger record does not need to contain every
human-readable field redundantly.

Conceptually:

~~~text
stored/transmitted fact
    timestamp
    stable source coordinate
    stable semantic fact coordinate
    fact-specific payload where actually needed

+

Blueprint / semantic maps

=

human projection
    "On Settings, Victor selected Dark theme"
    "ThemeMode accepted Select(Dark)"
    "Save succeeded"
    "Dark presentation arrived"
~~~

The wording above is projection, not canonical fact storage.

This is compatible with VexBoard and server-side interpretation without making Logger a giant
presentation schema.

## 9. Intentionality for future instances

When extending this design, preserve these questions separately:

~~~text
CONSTRUCTION
    what builds/owns/reuses the object?

UI SOURCE
    which registered element acted?

SEMANTIC MEANING
    what Control / State / Effect does it mean?

ACTIVE RUNTIME
    which RelationshipId owns current Job(s)?

CHRONOLOGY
    what accepted/observed facts happened?

HUMAN INTERPRETATION
    how should those stable coordinates be displayed here?
~~~

The clean architecture is the relationships between those answers.

Do not create one universal ID, metadata object, dependency container or Logger schema that tries to
own all six.

<!-- [VXG RealForever][TOPOLOGY][ID-PROTOCOL][VEXLOGGER][VEXSYNC-BRIDGE] -->
