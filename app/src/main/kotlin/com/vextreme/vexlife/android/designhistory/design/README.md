# VexLife DesignHistory — Current Design

[VXG RealForever]

Feature ref: `feature.vexlife.design-history`  
Current human name: **VexLife DesignHistory**  
Last meaningfully updated: **2026-10-15T23:41-07:00**  
Current design record: `records/2026-10-15-001-fractal-feature-design-continuity.md`

## What we are building

A repository-native continuity system for feature design intent.

It should let a fresh worker answer, without predecessor chat:

~~~text
What feature is this?
What is it called now?
What was it called before?
What source truth exists now?
What experience are we currently trying to build next?
Which meaningful design refinements produced that direction?
What is still open?
~~~

## Why

Implementation history and design history are related but different.

~~~text
Git / PRs
    what implementation changed

feature README
    what source truth exists now

design/README
    what we currently intend to build

design/records/*
    why that intention evolved
~~~

If these roles collapse into one giant document, current truth becomes hard to find. If design stays
only in chat, continuity is fragile.

## Target shape

Design continuity should be:

~~~text
feature-scoped
fractal
appendable
rename-safe
searchable
small at the current-design surface
historically recoverable
non-authoritative over feature source truth
~~~

## Fractal growth rule

Do not require every feature to start with a documentation tree.

~~~text
small feature
    README.md may be enough

active design evolution
    add design/README.md

multiple meaningful refinements
    append design/records/<dated-record>.md

independently addressable recurring decisions
    may later earn design/decisions/
~~~

Growth is earned by continuity pressure, not symmetry.

## Rename continuity

A human/product/package rename should not sever design lineage.

First pattern:

~~~text
Vex<Feature>Docs.kt
    FEATURE_REF
    CURRENT_FEATURE_NAME
    historicalSearchNames
~~~

Example already present:

~~~text
featureRef = feature.vexlife.conversation
currentName = Conversation
historicalSearchNames = [VexConversation]
~~~

This gives DesignHistory a real rename/search pressure case rather than a hypothetical one.

## Current authority order

For one feature:

~~~text
accepted source
    highest implementation truth

feature README
    current source/ownership explanation

feature design/README
    current intended design

dated design records
    historical reasoning / provenance
~~~

An older record never silently overrides current source or current design.

## First proof of concept

The first POC is a navigation/recovery proof using Conversation and Debug Menu.

### POC-A — old-name recovery

~~~text
START
    search repository for "VexConversation"

EXPECT
    find VexConversationDocs
    -> FEATURE_REF = feature.vexlife.conversation
    -> CURRENT_FEATURE_NAME = Conversation
    -> current design path is discoverable
    -> dated design record is discoverable
~~~

Success means a rename did not sever continuity.

### POC-B — current-versus-history separation

~~~text
conversation/README.md
    source truth

conversation/design/README.md
    current intended Conversation Lab design

conversation/design/records/2026-10-15-001-visible-conversation-lab.md
    why that design emerged
~~~

Success means a fresh worker does not need to read the whole historical record to learn the current
design, while the reasoning remains recoverable.

### POC-C — cross-feature non-duplication

~~~text
Debug Menu design
    owns reusable feature-lab doorway pattern

Conversation design
    owns C00-C16 Conversation scenarios

Debug Menu
    links to Conversation design instead of copying those scenarios
~~~

Success means meta/shared design remains composable without centralizing feature semantics.

## What the POC does not prove

~~~text
no automatic design index
no runtime DesignHistory UI
no global feature registry
no migration automation
no design linting framework
no guarantee every feature needs dated records
~~~

Those require additional consumers/pressure.

## Meaningful progression so far

1. Feature authoring already established one durable feature home.
2. Conversation pressure showed that current source truth alone did not preserve current design intent.
3. README / current design / dated record roles were separated.
4. Conversation required stable identity across possible future renames.
5. Debug Menu pressure proved design continuity must stay feature-scoped even when a shared surface
   composes several feature labs.
6. This pressure is now formalized as VexLife DesignHistory rather than remaining an implicit
   Conversation-only convention.

## Current next build

Complete this first repository-native POC and prove its code anchors compile.

Then use the preserved Conversation design as the input to the first visible Conversation Lab
vertical slice.

<!-- [VXG RealForever][feature.vexlife.design-history][CURRENT-DESIGN] -->
