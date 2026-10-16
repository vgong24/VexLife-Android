# VexMaps Distribution — Feature Home

[VXG RealForever]

## Feature identity

```text
featureRef = feature.vexmaps.distribution
currentName = VexMaps Distribution
current concrete journey noun = Delivery Route
historical/search names = Distribution Run / VexMapsDistributionRun
code navigation = VexMapsDistributionDocs
```

Current design:

```text
design/README.md
```

Meaningful design refinements:

```text
design/records/
```

## Human question map

```text
WHEN IS MAPS OPERATING FOR DISTRIBUTION WORK?
    distribution/mode/
    -> Distribution Mode Behavior

WHAT DELIVERY OBLIGATIONS DID THE HUMAN ENTER?
    data/maps/distribution/
    -> durable Draft + delivery stops + input order + return policy

WHAT ROUTE IS RECOMMENDED FOR THOSE OBLIGATIONS?
    distribution/planning/
    -> immutable preparation snapshot + derived Plan + optimizer

WHAT IS TRUE ABOUT CARRYING OUT THAT ROUTE?
    distribution/deliveryroute/
    -> Delivery Route State / Control / Act / Effect / Behavior

DO DISTRIBUTION MODE AND THE DELIVERY ROUTE AGREE?
    distribution/convergence/
    -> ModeDeliveryRoute convergence

DOES NAVIGATION REALIZE THE EXACT DELIVERY-ROUTE LEG?
    distribution/convergence/
    -> DeliveryRouteNavigation convergence

WHO OWNS THE LIVED RELATIONSHIP?
    distribution/session/
    -> Distribution Session

WHO OWNS COROUTINE CUSTODY?
    VexMapsDistributionExecution

WHAT DOES COMPOSE USE?
    VexMapsDistributionUi + VexMapsDistributionContextDock
```

## Current language boundary

```text
Distribution
    the broader Maps operating context

Delivery Route
    the concrete multi-stop delivery journey being prepared/carried out

Draft
    durable human-entered delivery obligations

Plan
    one recommendation derived from one exact preparation snapshot

Navigation
    the independently-owned realized journey/leg
```

These names are not synonyms. Keep them separate so a fresh developer can predict the question before
opening implementation.

## Current source map

```text
distribution/
├── README.md
├── VexMapsDistributionDocs.kt
├── VexMapsDistributionUi.kt
├── VexMapsDistributionContextDock.kt
├── mode/
├── deliveryroute/
├── planning/
├── convergence/
├── session/
└── design/
    ├── README.md
    └── records/
        └── 2026-10-16-001-name-ontology-delivery-route.md
```

Durable Draft storage remains under:

```text
data/maps/distribution/
```

The feature home explains current source truth. The design record preserves why the vocabulary changed.

## Ownership laws

```text
DRAFT != PLAN
PLAN != DELIVERY_ROUTE_STATE
DELIVERY_ROUTE_STATE != NAVIGATION_STATE
DISTRIBUTION_MODE != DELIVERY_ROUTE
CONVERGENCE != FOREIGN_EFFECT_EXECUTION
EXECUTION_CUSTODY != PRODUCT_MEANING
UI_BOUNDARY != SEMANTIC_TRUTH_OWNER
```

The Session composes current relationships without replacing the independently complete owners above.

## Naming / source navigation

The current naming pressure is preserved under the searchable tag:

```text
[Name Ontology]
```

Do not treat that tag as a dictionary of approved words. Use the dated record to reproduce the
cold-reader test against the code before carrying the lesson into another feature.

Historical source/search language remains intentionally discoverable:

```text
Distribution Run
VexMapsDistributionRun
```

A historical name is continuity evidence, not current source vocabulary.

## Proof status after the Delivery Route rename

This rename changes source identity on the active PR head, so prior exact-head proof must not be
relabeled as proof of the renamed candidate.

Preserved historical evidence still matters, but the renamed head must freshly earn the affected
repository checks before lifecycle continuation.

Separately:

```text
connected Android aggregate proof
!=
end-to-end Distribution human-walk proof
```

The current Journey catalog still lacks an honest host-owned Context Dock page-selection step, so a
full Delivery Route human walk remains explicit proof debt rather than a fabricated test shortcut.

## Current next edge

```text
fresh renamed-head source proof
-> connected Android proof on the renamed head
-> fresh Independent Assurance
-> currentize lifecycle only from newly earned evidence
```

A future Journey/Context Dock change should be earned by the host traversal question itself, not
added solely to make semantic coverage appear complete.

<!-- [VXG RealForever][feature.vexmaps.distribution][FEATURE-HOME] -->
