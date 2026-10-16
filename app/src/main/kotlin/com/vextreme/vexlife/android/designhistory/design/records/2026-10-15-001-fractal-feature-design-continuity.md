# VexLife DesignHistory record — Fractal feature design continuity

[VXG RealForever]

Feature ref: `feature.vexlife.design-history`  
Recorded at: **2026-10-15T23:41-07:00**  
Record ref: `design.feature.vexlife.design-history.2026-10-15.001`

## Discussion / pressure

Conversation architecture reached a terminal accepted foundation, then immediately produced a new
continuity problem: the next important work was experiential design, and that design was being formed
in conversation rather than source.

Victor identified two failure modes:

~~~text
one giant current document
    grows forever and becomes unreadable

one file per feature with no historical records
    loses meaningful reasoning between implementation milestones
~~~

A second pressure appeared around renaming: if Conversation is renamed later, its design lineage
should still be recoverable by searching both old and new names.

## Prior state

Repository feature-authoring already required one durable feature home, but did not define:

- a current-design versus historical-record distinction;
- a fractal growth path for long-lived feature design;
- a stable feature identity independent of human/package name;
- a first proof for rename/search continuity.

## New understanding

Design continuity has enough independent pressure to be named and owned as its own meta-feature:

~~~text
[VexLife DesignHistory]
~~~

It should not centralize feature design. It should define and prove the continuity grammar that each
feature can consume locally.

## Decisions crystallized

### 1. Stable ref is independent of current feature name

Use a code-local Docs anchor with stable feature ref, current name and historical/search names.

### 2. Current design and historical reasoning are separate surfaces

~~~text
design/README.md
    current synthesis

design/records/*
    append-only meaningful refinements
~~~

### 3. Growth is fractal, not mandatory

A feature earns deeper design structure only when continuity pressure exists.

### 4. DesignHistory is meta, not central semantic authority

Feature designs remain feature-owned.

### 5. The first POC must use real pressure

Conversation supplies a real old/current name pair (`VexConversation` -> `Conversation`) and a real
current design/record pair. Debug Menu supplies cross-feature composition pressure.

## First POC acceptance

A fresh worker should be able to:

1. search `VexConversation`;
2. recover `feature.vexlife.conversation`;
3. see `Conversation` as the current name;
4. navigate to the current Conversation design;
5. navigate to the dated record explaining the visible Conversation Lab design;
6. open Debug Menu design and see a link to Conversation rather than duplicated scenario truth.

## What remains open

- whether a shared feature identity type is ever needed;
- whether an automatic design index is useful after more feature consumers;
- whether feature rename/move tooling should update paths while preserving refs;
- whether long-lived decisions later deserve a `design/decisions/` layer;
- how VexStream and VexMaps adopt the pattern when their next real design pressure appears.

## Current design consequence

PR #97 should be understood and reviewed as the first VexLife DesignHistory POC, not merely a
Conversation documentation patch.

<!-- [VXG RealForever][feature.vexlife.design-history][DESIGN-RECORD-001] -->
