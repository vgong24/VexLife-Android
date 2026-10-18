# Debug Menu — Developer Surface Home

[VXG RealForever]

## Question

> How can developers create controlled diagnostic or synthetic conditions for product features
> without turning Debug Menu into another semantic owner, state store, or test framework?

## Current role

Debug Menu is a developer-facing **composition surface**.

It may expose:

- diagnostics;
- reset/inspection controls;
- deterministic synthetic condition controls;
- feature-lab entry points.

It must not absorb the feature semantics behind those controls.

~~~text
DEBUG MENU
    doorway / composition surface

FEATURE LAB
    feature-owned simulation/controller

FEATURE REPOSITORY / STATE
    normal feature truth

PRODUCT SCREEN
    normal consumer
~~~

## Existing precedent

Events Simulator already follows this direction:

~~~text
Debug Menu action
-> SyntheticExternalEventController
-> EventsRepository / attention owners
-> Events UI reacts
~~~

The synthetic controller exists to create external conditions; it does not become the Events
screen's truth owner.

## Feature Lab pattern

When another feature needs lived synthetic pressure:

~~~text
Debug Menu control
-> feature-owned Lab/fixture/controller
-> same repository/state contract used by product surface
-> ordinary reactive rendering
~~~

Feature-specific scenario names, participants, provider semantics and currentness belong with that
feature's source/design—not in Debug Menu.

This lets Debug Menu scale to future features without becoming a god-object.

## Current / planned consumers

~~~text
Events Simulator
    implemented

Conversation Lab
    current design
    feature owner = feature.vexlife.conversation
~~~

Future features may consume the pattern only when they have real synthetic-condition pressure.

## Ownership boundaries

Debug Menu does not own:

~~~text
Conversation participants/messages
Family membership/currentness
Events canonical truth
Maps provider truth
VexStream provider truth
Home/session authority
model runtime
Journey semantics
~~~

It only composes developer controls to the rightful owner.

## Design continuity

Current Debug Menu design: `design/README.md`

Meaningful refinements: `design/records/`

<!-- [VXG RealForever][DEBUG-MENU][DEVELOPER-SURFACE] -->
