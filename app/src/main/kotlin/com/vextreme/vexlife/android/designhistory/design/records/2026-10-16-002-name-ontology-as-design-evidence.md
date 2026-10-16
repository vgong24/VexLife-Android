# VexLife DesignHistory record — Name Ontology as recoverable design evidence

[VXG RealForever]

Feature ref: `feature.vexlife.design-history`  
Recorded at: **2026-10-16**  
Record ref: `design.feature.vexlife.design-history.2026-10-16.002`  
Tags: **[Name Ontology] [DesignHistory]**

## Discussion / pressure

The first DesignHistory POC proved that a feature can preserve:

~~~text
current source truth
current intended design
dated design refinements
stable identity across rename/search
~~~

That solved continuity after a name changed.

A newer pressure appeared one level earlier:

> How do we preserve the *reason a durable name changed* when the problem is not spelling, but that a
> competent fresh reader predicts the wrong question, owner or consequence radius from the old name?

That pressure first became explicit in the active VexMaps Distribution design work and was tagged:

~~~text
[Name Ontology]
~~~

VexMaps did not define an approved-word dictionary. It preserved a cold-reading exercise around
historical names such as `DistributionRun` and the more predictive `DeliveryRoute` vocabulary.

Conversation then independently transferred the same method against accepted source:

~~~text
VexConversationConvergenceRelay
-> cold-read prediction gap
-> test VexConversationSurfaceHybridRelay
-> reject Surface as too narrow
-> accept VexConversationHybridRelay
~~~

The Conversation correction was proved and landed through PR #107.

## Prior DesignHistory understanding

DesignHistory record #001 primarily preserved:

~~~text
feature rename continuity
current design vs historical reasoning
feature-scoped records
stable featureRef
cross-feature non-duplication
~~~

A future worker could recover *that a name changed*.

It did not yet explicitly preserve a reusable method for answering:

> Why was the old name misleading to a fresh reader?

## New understanding

A durable name can itself be design evidence.

The important artifact is not a global naming rule. It is the reproducible comparison between:

~~~text
cold prediction from the name alone

vs

actual question / owner / consequence radius in source
~~~

When that gap materially misdirects a competent reader, the gap can earn a feature-owned design
record.

That practice is now named:

~~~text
[Name Ontology]
~~~

## Decisions crystallized

### 1. Name Ontology is a method, not a word registry

Do not centralize approved nouns.

A Name Ontology record should let a future worker reproduce the reasoning:

~~~text
1. hide implementation/KDoc;
2. cold-read the current/historical name;
3. predict the question, owner and consequence radius;
4. inspect the actual source/product relationship;
5. compare prediction with reality;
6. preserve materially different reasonable interpretations;
7. rename only when the gap matters;
8. retain historical names as searchable continuity coordinates.
~~~

### 2. The feature still owns its names

DesignHistory owns the continuity grammar.

It does not decide that:

~~~text
DeliveryRoute
Hybrid
Surface
Experience
Session
Connection
...
~~~

are globally correct words.

VexMaps owns its Delivery Route vocabulary.
Conversation owns its Hybrid vocabulary.
Their source/current design remains authoritative.

### 3. Historical names stay searchable without remaining current authority

A successful rename should produce:

~~~text
current source
    predictive current name

current design
    current vocabulary and question

design record
    old name + prediction gap + reasoning

historical search
    old term still reaches the explanation
~~~

This is stronger than deleting old terminology everywhere and forcing Git archaeology.

### 4. Cold-read pressure is a design test, not automatic rename authority

Do not start repository-wide naming campaigns.

A rename is earned only when the prediction gap matters to a real:

~~~text
reader
source-navigation path
owner boundary
behavior question
consequence radius
~~~

A merely different aesthetic preference is not enough.

### 5. Name Ontology can reject proposed specificity as well as broadness

Conversation supplied an important second lesson.

~~~text
VexConversationConvergenceRelay
    too broad by cold-read consequence radius

VexConversationSurfaceHybridRelay
    too narrow because Surface named one consumer of a wider relationship

VexConversationHybridRelay
    matched the one currently earned Hybrid relationship
~~~

The method therefore does not reward longer names automatically.

It asks for the smallest name whose cold prediction matches the actual semantic boundary.

## Current consumers

### VexMaps Distribution — originating pressure

Active draft PR #86 carries:

~~~text
design/records/2026-10-16-001-name-ontology-delivery-route.md
~~~

That source is still draft/current-to-its-branch, not accepted Android-main authority.

Its value to DesignHistory is the method it exposed:
make the reader experience the prediction gap before revealing the rename.

### Conversation — independent accepted transfer

Accepted main carries:

~~~text
conversation/design/records/2026-10-16-003-name-ontology-hybrid-relay.md
VexConversationHybridRelay
~~~

Conversation proved that the method transfers beyond Delivery Route vocabulary and can reject both
an over-broad old name and an over-specific proposed replacement.

## What this does not establish

~~~text
no global naming registry
no approved-word catalog
no static naming linter
no automatic rename engine
no requirement that every rename gets a design record
no authority for DesignHistory to rename feature source
no claim that all Convergence is Hybrid
~~~

Those would add recurring burden without current evidence.

## Current DesignHistory consequence

DesignHistory has now completed more than its original first POC.

It has demonstrated:

~~~text
rename-safe feature identity
current design vs historical reasoning
feature-scoped appendable records
cross-feature non-duplication
a transferred Name Ontology method
historical-name recovery after a semantic rename
~~~

The next DesignHistory edge is no longer "prove the first POC."

The next edge should be earned by another real continuity failure or repeated consumer pressure. Until
then, keep DesignHistory small and feature-scoped.

<!-- [VXG RealForever][feature.vexlife.design-history][DESIGN-RECORD-002][Name Ontology] -->
