# Feature authoring home

[VXG RealForever]

## Purpose

This is the repository front door for a **new product feature or major feature evolution**.

It exists so a fresh human, AI instance, or coding agent can answer:

~~~text
What human outcome am I trying to create?
What truth already exists?
Which product question is actually missing?
Where does its meaning belong?
Which established foundations and ordinary development vocabulary already fit?
What source shape is the smallest truthful answer?
What proof owns each claim?
~~~

This file is a router and completion contract. It does not duplicate the Behavior, Elements, Journey,
ScreenCrawler, Android-host-proof, or provider manuals.

## Fresh feature entry

1. Read `../CULTURE.md`.
2. Read `../foundation/FEATURE-EXPERIENCE-LANGUAGE.md`.
3. Start/update one durable feature home using `VEX-FEATURE-TEMPLATE.md` as a thinking aid.
4. Describe the human situation and existing truth before hardening new nouns.
5. Inspect only the foundations implicated by the feature.
6. Use an existing feature only as a worked example, never as a mandatory source skeleton.
7. Implement one real vertical slice before generalizing reusable abstractions.
8. Prove each claim at its rightful proof owner.
9. Reconcile the feature home, source, generated effects, and next real edge before calling it done.

## Concept formation

For every proposed durable noun:

~~~text
HUMAN SITUATION
-> CURRENT TRUTH
-> REAL PRESSURE
-> MISSING QUESTION
-> RELATIONSHIPS / BOUNDARY
-> ESTABLISHED DEVELOPMENT LANGUAGE
-> EXISTING RIGHTFUL HOME
-> ORDINARY DESCRIPTION WITHOUT THE NEW NOUN
-> NAME
-> MINIMUM SOURCE SHAPE
-> PROOF
~~~

Do not start from a code shape and invent architecture language afterward.

A repeated implementation representation does not automatically mean one semantic concept.

## Feature home contract

Every substantial feature should have one obvious durable home—normally a feature-local README or
equivalent owning document—that lets a fresh worker recover the feature without predecessor chat.

Include only what is materially useful:

~~~text
1. Human/experience sentence
2. Human question map
3. Current source/ownership map
4. Existing foundations reused
5. Provider / repository / platform boundaries
6. User-visible surfaces
7. Important human journeys
8. Test / proof map
9. Current holds / known non-goals
10. Next real unfinished edge
~~~

Do not create empty sections for ceremony.

## Stable feature identity and continuity growth

A feature's current human name and package path may evolve. Its durable identity should not need to.

When rename continuity matters, keep one stable feature ref at a code-local navigation anchor:

~~~text
featureRef
    stable semantic identity
    survives human/package renames

currentName
    current human-facing/source-navigation name

historicalNames
    old names/search aliases retained so prior discussions and source remain discoverable
~~~

Do not create a repository-wide runtime feature registry merely to satisfy documentation. A
feature-local code anchor is enough until multiple real consumers need one shared machine-readable
contract.

A feature home may grow fractally when continuity pressure earns it:

~~~text
feature/
├── README.md
├── Vex<Feature>Docs.kt
└── design/
    ├── README.md
    └── records/
        ├── YYYY-MM-DD-NNN-meaningful-refinement.md
        └── ...
~~~

Meaning:

~~~text
feature README
    what is true in source now

design/README
    what we currently intend to build / experience next

design/records/*
    dated meaningful design conversations/refinements
    preserved provenance, not competing current authority
~~~

Append one record for one meaningful design refinement, not one record for every chat turn. Keep the
current design small by synthesizing accepted/current reasoning into design/README.md and linking the
records that explain how it got there.

Older records remain useful historical evidence. If implementation or the current design has
advanced, the current source + feature README + design/README.md outrank stale record prose.

Small features do not need this package merely for symmetry. Add layers only when losing the design
trajectory would make a successor reconstruct intent from chats, issues, or commit archaeology.

## Package placement

The source tree should first help someone locate the **human part of the feature**.

~~~text
feature/
├── library/
├── playback/
└── queue/
~~~

Inside that area, code-role names are welcome when they are precise:

~~~text
queue/
├── ...State
├── ...Control
├── ...Act
├── ...Effect
├── ...Behavior
└── Media3...Connection
~~~

Do not force this exact tree onto a feature with a different truthful shape.

## Ownership questions

Before adding a class/layer, identify which existing owner answers each material question:

~~~text
semantic meaning
current truth
data/source
runtime effect
Android/platform lifetime
authorization/permission
projection/UI
proof
~~~

One object may legitimately fulfill several roles. Do not multiply owners merely to make a diagram
symmetrical.

## Reusable-contract rule

A reusable abstraction needs repeated real pressure. One consumer may teach a pattern; it does not
automatically authorize a global framework.

Before extracting a reusable contract, ask:

~~~text
Which independent consumers need the same semantic promise?
Is the commonality meaning, or merely implementation shape?
Would ordinary Android/Kotlin/domain vocabulary communicate this better?
What would be lost if the shared abstraction did not exist?
~~~

If the answer is only "the code looks similar", keep it local.

## Proof follows the claim

| Claim | Cheapest truthful proof |
|---|---|
| Behavior relationship / domain transformation | focused JVM test |
| reusable foundation invariant | foundation JVM test |
| semantic truth -> presentation | projection/render contract |
| human interaction sequence | Journey |
| deterministic visual appearance | screenshot/golden when admitted |
| Android lifecycle / MediaSession / native SDK behavior | instrumentation/device/provider proof |
| physical product feel | lived review |

Canonical routes live in the foundation index.

## Completion contract

Before presenting a feature as complete/current:

### Meaning
- Can a fresh developer explain the feature in ordinary language?
- Does each durable top-level noun answer a distinct question?
- Were ordinary established terms preferred when they mean the same thing?
- Does the feature home still match source?

### Source
- Is there one obvious entry point?
- Are semantic and runtime/platform owners distinguishable?
- Did a technical helper accidentally become a peer feature concept?
- Were speculative abstractions avoided or explicitly held?

### Behavior and proof
- Do focused tests protect changed meaning?
- Did changed human walks update Journey proof when warranted?
- Did native/provider claims receive native/provider proof?
- Were aggregate verification and source cleanliness reconciled?

### Finish
- Are stale docs/comments/experimental duplicates currentized or explicitly held?
- Is the next unfinished edge explicit?
- Can a successor continue without reconstructing the intent from commit diffs?

## Worked reference: VexStream

VexStream is a current worked reference, not the template.

Read:

1. `../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/README.md`
2. `../foundation/FEATURE-EXPERIENCE-LANGUAGE.md`
3. `../../architecture/behavior/README.md`
4. `../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/playback/VexStreamPlaybackDocs.kt`
5. `../../app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/queue/VexStreamQueueDocs.kt`
6. `../../app/src/journeyTest/kotlin/com/vextreme/vexlife/android/testing/journey/catalog/shared/vexstream/VexStreamJourneys.kt`

What to copy:

~~~text
human question first
one rightful truth owner
feature-local meaning before global abstraction
established language before invented vocabulary
provider/platform mechanics below semantic meaning
proof follows claim
explicit holds
~~~

What not to copy blindly:

~~~text
Library
Sources
Playback
Queue
Media3
the exact package depth
the exact test matrix
~~~

<!-- [VXG RealForever][FEATURE-HOME][FEATURE-AUTHORING] -->
