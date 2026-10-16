# VexMaps Distribution — Current Design

[VXG RealForever]

Feature ref: `feature.vexmaps.distribution`  
Current human name: **VexMaps Distribution**  
Current concrete journey noun: **Delivery Route**  
Last meaningfully updated: **2026-10-16**  
Current design record: `records/2026-10-16-001-name-ontology-delivery-route.md`

## Role of this file

This is the current intended design language for VexMaps Distribution.

For accepted implementation truth, start at `../README.md` and `VexMapsDistributionDocs`.

```text
../README.md
    current source / ownership truth

design/README.md
    current intended design vocabulary and next pressure

design/records/*
    meaningful refinements that changed how the feature is understood
```

## Current design vocabulary

```text
Distribution Mode
    whether Maps is currently operating in the broader Distribution context

Delivery Route
    the finite route through admitted delivery obligations

Delivery Draft
    durable obligations before route preparation

Delivery Plan
    derived recommendation from one exact preparation snapshot

Delivery Route + Navigation
    independently-owned truths related through convergence
```

The current source should let a colleague discover those distinctions from names and paths before
reading implementation bodies.

## Name Ontology

Current design uses the searchable continuity tag:

```text
[Name Ontology]
```

The tag names a design practice, not an approved-word registry.

The current record is intentionally structured as a cold-reading exercise. A future worker should
experience their own prediction gap before reading the vocabulary change as history.

## Current architectural intent

Preserve the existing ownership decomposition while making the language more predictive:

```text
durable obligations
-> preparation snapshot
-> derived plan
-> Delivery Route semantics
-> exact Navigation leg
-> visible Distribution UI
```

No new semantic owner is introduced by the naming refinement.

The former Mode/Run relationship is now named from the two questions it actually relates:

```text
Distribution Mode
+
Delivery Route
-> ModeDeliveryRoute convergence
```

The former broad internal Behavior `Experience` wrappers are not retained merely for symmetry.
Session consumes the concrete Mode and Delivery Route Connections directly. The broad Compose-facing
feature gateway is named `VexMapsDistributionUi` so its role is visible without borrowing the
Behavior-family Experience term.

## Proof / experience hold

The rename itself must receive fresh repository proof.

The separate lived interaction question remains open:

```text
How does a Journey honestly select the Distribution Context Dock page
through the same host-owned interaction a person uses?
```

Do not answer that by injecting presentation state through a repository fixture.

## Next design edge

Use the renamed source during the next proof/review pass and watch where a fresh reader still needs
implementation context to understand a name.

If another prediction gap appears, apply the same cold-reader method before adding or renaming a
durable concept.

<!-- [VXG RealForever][feature.vexmaps.distribution][CURRENT-DESIGN][Name Ontology] -->
