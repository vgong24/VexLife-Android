# [Name Ontology] VexMaps Distribution record — Cold-read the relationship before naming it

[VXG RealForever]

Feature ref: `feature.vexmaps.distribution`  
Recorded at: **2026-10-16**  
Record ref: `design.feature.vexmaps.distribution.2026-10-16.002`  
Tags: **[Name Ontology] [VexMaps Distribution] [Delivery Route] [Convergence]**

## Reader protocol

Do not jump to section D.

This record exists so a future colleague can experience the naming/relationship mistake before being
shown the correction.

## A — Cold read the old relationship vocabulary

Without opening implementation bodies, read only:

```text
VexMapsDeliveryRouteNavigationLayer
VexMapsDeliveryRouteNavigationRequest

layer = AtDelivery
```

Write down what you think each means.

Use these questions:

```text
In a Maps codebase, what would you expect a "NavigationLayer" to own?
If something is named "NavigationRequest", which owner would you expect to receive it?
If the Delivery Route is ReturningToStart and Navigation says ARRIVED, what would "AtDelivery" tell you?
```

Preserve your first interpretation.

## B — Read relationship evidence, not this record's conclusion

Now inspect:

```text
distribution/convergence/VexMapsDeliveryRouteNavigationConvergenceRelay.kt
distribution/deliveryroute/VexMapsDeliveryRouteBehavior.kt
distribution/session/VexMapsDistributionSession.kt
```

Trace two cases separately:

```text
1. Delivery Route = Delivering
   expected target = Stop(...)
   Navigation = ARRIVED

2. Delivery Route = ReturningToStart
   expected target = ReturnToStart
   Navigation = ARRIVED
```

Then inspect every variant that the relationship's outbound request can carry.

Do not ask which rename this record wants. Ask:

```text
What fact is the enum actually classifying?
Can the emitted request target only Navigation?
What does current target truth contribute to ARRIVED?
Which owner ultimately consumes each request?
```

## C — Compare prediction with observed ownership

Return to the names from section A.

Mark each:

```text
PREDICTED CLEANLY
NEEDED IMPLEMENTATION CONTEXT
MISDIRECTED ME
```

Especially notice whether "Layer" suggested rendering/architecture rather than a derived relationship
classification, and whether "NavigationRequest" predicted one target owner even when the request can
legitimately carry a Delivery Route Control.

Also compare the two ARRIVED cases. If both produce the same human classification, ask what truth
was lost.

## D — Inspect the current source vocabulary

Only now inspect the current types:

```text
VexMapsDeliveryRouteNavigationRelationshipStatus
VexMapsDeliveryRouteNavigationReconciliationRequest
```

Inspect the target-aware arrival statuses:

```text
Stop(...) + ARRIVED
-> AtDelivery

ReturnToStart + ARRIVED
-> AtStart
```

And inspect the reconciliation variants before deciding whether the current names predict their
consequence radius.

Do not treat these words as a reusable naming recipe. They are the current answer to this exact
relationship.

## E — Repeat the test elsewhere

The reusable practice is still:

```text
hide implementation
-> predict owner / question / consequence from the name
-> reveal source truth
-> measure the prediction gap
-> change only when that gap is materially misleading
```

This record adds one pressure to the earlier Delivery Route exercise:

```text
A domain rename can succeed while a relationship name still lies about
what kind of fact it is or which owner a request targets.
```

The relationship classification itself must also remain truthful. A downstream Control eventually
repairing state does not make a false intermediate label acceptable.

## Historical search continuity

These names remain here intentionally:

```text
VexMapsDeliveryRouteNavigationLayer
VexMapsDeliveryRouteNavigationRequest
AtDelivery-for-return-arrival
```

They are historical coordinates, not current source authority.

See current synthesis: `../README.md`

<!-- [VXG RealForever][feature.vexmaps.distribution][DESIGN-RECORD-002][Name Ontology] -->
