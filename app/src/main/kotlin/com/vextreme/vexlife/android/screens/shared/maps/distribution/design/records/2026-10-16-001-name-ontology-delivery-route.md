# [Name Ontology] VexMaps Distribution record — Cold-read the route before learning it

[VXG RealForever]

Feature ref: `feature.vexmaps.distribution`  
Recorded at: **2026-10-16**  
Record ref: `design.feature.vexmaps.distribution.2026-10-16.001`  
Tags: **[Name Ontology] [VexMaps Distribution] [Delivery Route]**

## Reader protocol

Do not begin by reading the conclusion of this record.

The point of this record is to let a future colleague catch their **own** naming assumption.

If you are using this record to learn the pattern, follow the sequence in order.

## A — Cold read

Without opening an implementation body or KDoc, read only these historical names:

```text
VexMapsDistributionRun
VexMapsDistributionRunState
VexMapsDistributionRunControl
VexMapsDistributionModeRunConvergenceRelay
CompleteStop
PrepareRun
BeginRun
ResetRun
```

Before continuing, write down what you think each noun or verb means.

Do not search for the explanation yet.

Useful prompts:

```text
What does "Distribution" make you expect?
What is a "Run"?
What would "CompleteStop" do?
What kind of owner would "DistributionRun" be?
What would you expect ModeRun convergence to compare?
What consequences would you expect from changing it?
```

There is no score. Preserve your first interpretation long enough to compare it with the source.

## B — Read the product evidence, not the explanation

Now inspect these current/historical source surfaces in this order:

```text
app/src/main/res/values/strings.xml

app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/maps/
    VexMapsScreen.kt
    VexMapsMapSurface.kt

app/src/main/kotlin/com/vextreme/vexlife/android/data/maps/distribution/
    VexMapsDistributionRepository.kt

app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/maps/distribution/
    planning/
    deliveryroute/
    convergence/
    session/
```

While reading, do not ask "why was the old name chosen?"

Ask only:

```text
What is the human doing?
What truth survives before route preparation?
What changes when preparation succeeds?
What does the Delivered action mean?
What does Navigation own separately?
Which relationship is Mode convergence actually checking?
```

Write the answers in your own words.

## C — Return to the historical names

Now reread the names from section A.

Compare your first prediction with what you just observed.

Mark each one:

```text
PREDICTED CLEANLY
    the cold name gave me approximately the right conceptual neighborhood

NEEDED CONTEXT
    the implementation/KDoc had to teach me what the name meant

MISDIRECTED ME
    my reasonable first interpretation pointed at a materially different question
```

Do this before reading the rename below.

The mismatch you observe is the evidence this record is preserving.

## D — Read the current vocabulary

Only after completing A–C, inspect the renamed source.

Start from:

```text
distribution/deliveryroute/
    VexMapsDeliveryRouteDocs
    VexMapsDeliveryRouteState
    VexMapsDeliveryRouteControl
    VexMapsDeliveryRouteAct
    VexMapsDeliveryRouteEffect
    VexMapsDeliveryRouteBehavior
    VexMapsDeliveryRouteConnection

convergence/
    VexMapsDistributionModeDeliveryRouteConvergenceRelay
    VexMapsDeliveryRouteNavigationConvergenceRelay
```

Then read the calling names:

```text
startPreparedDeliveryRoute()
markCurrentDeliveryComplete()
cancelDeliveryRoutePreservingDraft()
clearDeliveryRoute()
```

Do another cold prediction before opening each body.

Compare the size of the prediction gap with section A.

## E — Notice the pattern you just performed

Do not preserve the lesson as:

```text
"always use DeliveryRoute"
```

That would turn one correction into another naming recipe.

Instead, when a new durable name is proposed:

1. hide its implementation;
2. let a competent colleague predict its question/owner/consequence radius from the name alone;
3. record materially different reasonable interpretations;
4. then reveal the source and compare;
5. if KDoc or implementation history is required to rescue the noun, treat that delta as design evidence.

That repeated exercise is what this record tags as:

```text
[Name Ontology]
```

## Historical search continuity

The old terms remain here deliberately:

```text
Distribution Run
VexMapsDistributionRun
ModeRun
CompleteStop
PrepareRun
BeginRun
ResetRun
```

They are searchable historical coordinates, not current naming authority.

A successor arriving from an old PR/comment should be able to find this record and reproduce the
reasoning rather than merely seeing that symbols were renamed.

## What remains open

Repeat the same cold-read exercise on surrounding nouns as new work touches them.

Do not start a rename campaign from this record. A rename is earned when the prediction gap matters
to a real reader, source-navigation path, behavior question, or consequence boundary.

The current source/home owns present truth. This record preserves the experience that changed it.

See current synthesis: `../README.md`

<!-- [VXG RealForever][feature.vexmaps.distribution][DESIGN-RECORD-001][Name Ontology] -->
