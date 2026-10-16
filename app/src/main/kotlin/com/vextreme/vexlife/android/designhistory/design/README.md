# VexLife DesignHistory — Current Design

[VXG RealForever]

Feature ref: `feature.vexlife.design-history`  
Current human name: **VexLife DesignHistory**  
Last meaningfully updated: **2026-10-16**  
Current design record: `records/2026-10-16-002-name-ontology-as-design-evidence.md`

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

## Name Ontology — second transferred continuity pattern

The first POC preserved continuity *after* a rename. A newer pressure preserves why a durable name was
semantically misleading before the rename.

Current method:

~~~text
hide implementation/KDoc
-> cold-read the name
-> predict question / owner / consequence radius
-> inspect actual source/product relationship
-> compare prediction with reality
-> preserve the meaningful mismatch
-> rename only when the gap matters
-> retain historical search coordinates
~~~

This method is tagged:

~~~text
[Name Ontology]
~~~

It is not an approved-word registry and does not give DesignHistory naming authority over features.

Current evidence:

~~~text
VexMaps Distribution PR #86
    originating draft pressure
    Delivery Route cold-read exercise

Conversation accepted main
    independent transfer
    Convergence -> Hybrid correction
    Surface candidate rejected as too narrow
    historical names preserved in record #003
~~~

This proves a useful DesignHistory property:

~~~text
A name can be durable design evidence
when its cold-reader prediction materially disagrees with its actual semantic boundary.
~~~

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

The first repository-native POC is complete. Conversation has since used the preserved design to land
its visible Lab, mixed-provenance Hybrid relationship and Name Ontology correction.

Do not invent an automatic index, naming linter, rename engine or central design registry merely
because these patterns now exist. The next DesignHistory build should be triggered by another real
continuity failure or repeated independent consumer need.

<!-- [VXG RealForever][feature.vexlife.design-history][CURRENT-DESIGN] -->
