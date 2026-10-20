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
    -> NavigationRelationshipStatus + bounded ReconciliationRequest

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
        ├── 2026-10-16-001-name-ontology-delivery-route.md
        └── 2026-10-16-002-name-ontology-relationship-truth.md
```

Durable Draft storage remains under:

```text
data/maps/distribution/
```

The feature home explains current source truth. The dated design records preserve the naming
experiences that changed it without becoming competing current-source authority.

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

## Accepted current checkpoint

VexMaps Distribution / Delivery Route is accepted on current `main` through PR #86.

~~~text
merge = 28c80a8b2c89e0225427086685b6ceea8870e10c
tree = 179a34d2b43c670ec99b65b0d0dbf00acb5e648e

Fresh Independent Assurance = CLEAR_ON_EXACT_HEAD

currentized aggregate proof:
    vexVerifyLocal = PASS
    physical-device preflight = PASS
    vexVerifyDevice = PASS
    connected tests = 10 executed / 0 skipped / 0 failed

post-merge exact-tree verification = PASS
~~~

The accepted composition preserves the ownership laws above:

~~~text
Behavior families
    own Distribution Mode / Delivery Route local semantics

Convergence Relays
    own relationships among independently complete truths

Distribution Session
    owns lived currentness / failure / retry meaning

VexMapsDistributionExecution
    owns coroutine custody only

UI / Context Dock
    presents already-resolved truth
~~~

This feature is therefore a worked production consumer of the accepted Behavior, Relay and
VexExecution foundations rather than a separate VexMaps-only architecture.

## Remaining proof debt

~~~text
connected Android aggregate proof
!=
full Distribution human-walk acceptance
~~~

The Journey catalog still lacks an honest host-owned Context Dock page-selection + deterministic
routing/location path for the complete Delivery Route workflow. That remains explicit non-blocking
proof debt. Do not create a fake product/test shortcut merely to make coverage or Journey accounting
look complete.

## Current next edge

There is no automatic architecture rewrite or proof replay owed for the accepted PR #86 generation.

Future VexMaps Distribution work should begin only from real product/proof pressure, for example:

~~~text
honest full Delivery Route lived-Journey capability
OR
new Distribution product behavior
OR
coverage evidence that reveals a hidden rightful owner
OR
a native/provider claim that needs its own proof
~~~

Coverage is discovery evidence, not authority to split this feature or add tests around an accidental
shape. If a large under-covered source area contains another human question, extract that rightful
owner and prove it cheaply. If it is legitimate composition or native/provider machinery, use the
proof surface that owns that claim.

<!-- [VXG RealForever][feature.vexmaps.distribution][FEATURE-HOME] -->
