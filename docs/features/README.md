# VexLife-Android Feature Home

[VXG RealForever]

## Purpose

This is the repository front door for a **new product feature or major feature evolution**.

It exists so a fresh human, AI instance, or coding agent can answer:

~~~text
What am I trying to create?
How should I describe it before coding?
Where does its meaning belong?
Which existing foundations should I reuse?
How should source be organized?
What proof does each claim require?
What must be updated before this feature is actually "done"?
~~~

This file is a router and completion contract. It deliberately does not duplicate the detailed
architecture, Journey, Test Foundation, ScreenCrawler, or Android-host-proof manuals.

## Fresh feature entry

When assigned a new feature:

1. Read ../CULTURE.md.
2. Read ../foundation/FEATURE-EXPERIENCE-LANGUAGE.md.
3. Start a feature home from VEX-FEATURE-TEMPLATE.md.
4. Inspect only the existing foundations implicated by the feature.
5. Use VexStream below as the current worked reference when the feature needs Experience /
   Architecture / Provider relationships.
6. Implement one real vertical slice before generalizing new reusable abstractions.
7. Prove each claim at its rightful proof owner.
8. Run the completion contract before calling the feature complete.

## Do not begin from package structure

Begin from the experience sentence:

> A **[WHO]** comes to **[HOME]** because **[WHY]**...

Then classify:

~~~text
WHAT
WHERE
HOW
NOW / WHEN
WHAT CAN I DO?
WHO / WHAT REALIZES IT?
WHAT DO THEY SEE?
~~~

Only after those answers are understandable should package/class names harden.

The question map is a **category-drift detector**, not a requirement for one class per answer.

## Feature home contract

Every substantial feature should have one obvious durable home—normally its feature-local README or
equivalent owning document—that lets a fresh worker recover the feature without predecessor chat.

The feature home should contain, when relevant:

~~~text
1. Experience sentence / intention
2. Human question map
3. Current source map
4. Meaning / state / effect owners
5. Reused foundations and contracts
6. Provider / SDK / platform boundaries
7. User-visible surfaces
8. Important human journeys
9. Test / proof map
10. Current maturity / known holds
11. Next real unfinished edge
12. Completion checklist
~~~

Do not copy empty sections merely for ceremony. Use the template to discover what the feature really
needs, then keep the durable home as small as correctness and continuity allow.

## Source map rule

A feature source map should answer:

> If this behavior is wrong, where would a new developer start?

Prefer a small readable map such as:

~~~text
Experience
├── known domain things
├── sources / destinations
└── Architecture
    ├── Capability
    └── Capability

Provider contracts
└── native / SDK adapter

UI / Screen
└── projection + Controls
~~~

Do not force this exact tree onto a feature that has a different truthful shape.

## Ownership questions

Before adding a class or layer, identify which owner answers each material question:

~~~text
semantic meaning owner
current state owner
data/source owner
runtime effect owner
Android lifecycle/platform owner
authorization/permission owner
projection/UI owner
proof owner
~~~

One object may legitimately implement several roles. That does not make the roles semantically
identical.

## Reusable-contract rule

Use an existing reusable contract when its words mean the same thing.

Create a new reusable abstraction only after answering:

~~~text
Which real consumers need it?
What repeated problem does it remove?
What does its name let a developer predict?
Who owns the contract?
What tests prove its invariant?
Could the same truth stay feature-local for now?
~~~

A first feature may teach a candidate abstraction. A second materially different consumer is strong
evidence that the abstraction is actually reusable.

## Testing / proof map

Choose proof by claim, not by habit.

| Claim | Cheapest truthful proof |
|---|---|
| reusable architecture/contract invariant | reusable-module JVM test |
| feature/domain transformation | feature JVM test |
| semantic state -> presentation projection | projection JVM test |
| known state -> Android semantic UI | Compose/render contract |
| human interaction sequence | Journey |
| deterministic visual appearance | screenshot/golden when admitted |
| integrated environment evidence | ScreenCrawler when relevant |
| Android lifecycle/service/permission/MediaSession/native SDK behavior | instrumentation/device/provider proof |
| physical product feel / human acceptance | lived review |

Canonical detail: ../foundation/TEST-PROOF-ARCHITECTURE.md

New/changed human walks: ../foundation/JOURNEY-INSTRUMENTATION.md

Host execution when local Android substrate is unavailable: ../foundation/ANDROID-HOST-PROOF.md

## Ordinary repository proof

Use focused proof while developing, then use the repository-owned aggregate that matches the claim:

~~~bash
./gradlew vexVerifyJvm
./gradlew vexVerifyLocal
./gradlew vexVerifyDevice
~~~

~~~text
vexVerifyJvm
    source alignment + reusable contracts + app JVM proof

vexVerifyLocal
    JVM/source proof + debug APK

vexVerifyDevice
    connected Android instrumentation/Journey substrate
~~~

A focused PASS does not replace the aggregate claim when the feature changes shared source.

## Human journey requirement

If the feature adds or materially changes an interactive user path:

- add/update the owning feature Journey;
- give important interaction surfaces registered semantic identity;
- let Journey coverage expose missing semantic paths;
- do not rewrite the same expected walk separately in logs/tests.

A feature is not required to create a Journey for every static visual node.

## Visual requirement

If the claim is about how a meaningful state looks:

- name the meaningful state;
- use deterministic visual proof only when it owns that claim;
- use ScreenCrawler for integrated environment evidence when appropriate;
- do not treat an automatically generated reference image as accepted merely because tooling emitted
  it.

## Provider / partner SDK requirement

When integrating Media3, VLC, a partner SDK, VexHome, or another external runtime:

~~~text
Vex semantic contract
!= provider capability contract
!= vendor SDK vocabulary
~~~

Prefer:

~~~text
Vex-facing Provider contract
-> adapter
-> vendor/native SDK
~~~

Do not leak vendor names upward into portable semantic vocabulary unless the vendor identity itself is
product meaning.

A provider should implement only capabilities it truthfully supports. Avoid giant interfaces whose
unsupported methods throw at runtime.

## Android boundary

Android owns Android-only facts:

~~~text
lifecycle
permissions
services
intents
device behavior
Compose/native rendering
packaging/install
MediaSession / platform integration
~~~

Do not make Android the universal semantic owner merely because Android realizes the feature.

## Completion contract — "once you're done"

Before presenting a feature as complete/current, reconcile these questions.

### Meaning

- Does the feature home still describe the source that actually exists?
- Can a fresh developer explain the feature in ordinary language?
- Are new top-level nouns answering distinct human questions?
- Are temporary experiments/version names confined to the correct boundary?

### Source

- Is there one obvious entry point for the feature?
- Are semantic owners and provider/platform owners distinguishable?
- Were speculative abstractions avoided or explicitly held?
- Are generated source / schema / reference effects deliberately accepted, rejected, or cleaned?

### Behavior

- Do focused unit/domain tests protect the changed meaning?
- Did new/changed interactions update their Journey when warranted?
- Are visual/native/provider claims proved at the layer that actually owns them?

### Repository proof

- Did the appropriate canonical Gradle proof run?
- If Android/device/provider reality matters, was that exact candidate proved on the rightful
  substrate?
- Is any missing proof explicitly marked as owed rather than implied PASS?

### Documentation / maps

- Update the feature home when architecture/ownership/current edge changed.
- Update docs/foundation/INDEX.md **only** when a reusable/load-bearing foundation materially
  changed; ordinary feature work should not churn the foundation map.
- Update repository culture/front-door docs only when the repository-wide authoring contract itself
  changed.

### PR / handoff

The PR or durable handoff should answer:

~~~text
What human outcome changed?
Why was this shape chosen?
What owns the meaning?
What integrations/providers changed?
What proof actually passed?
What proof is still owed?
What is the next real unfinished edge, if any?
~~~

Do not leave a future worker to reconstruct these answers from commit diffs alone.

### Clean finish

Before final completion:

~~~text
exact candidate identified
required proof complete or honestly held
unexpected source effects reconciled
stale docs/comments removed or currentized
no duplicate temporary implementation silently left authoritative
next edge either finished or explicitly preserved
~~~

"Done" means the admitted outcome and its required proof/continuity are complete—not merely that the
new code compiles.

## Head worked example: VexStream

VexStream is the current **worked reference**, not a mandatory feature shape.

Read these in order when an example would help:

1. Experience sentence / whole-feature orientation:
   app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/experience/v2/VexStreamExperience.kt
2. Reusable architecture vocabulary:
   architecture/vexarchitecture/README.md
3. VexStream feature history / product boundaries:
   app/src/main/kotlin/com/vextreme/vexlife/android/data/vexstream/README.md
4. Player Architecture:
   app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/experience/v2/player/VexStreamPlayerArchitecture.kt
5. Capability examples:
   app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/experience/v2/player/playback/VexStreamPlayback.kt
   app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/experience/v2/player/queue/VexStreamQueue.kt
6. Provider boundary:
   app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/experience/v2/player/provider/VexStreamPlayerProvider.kt
   app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/experience/v2/player/provider/media3/Media3VexStreamPlayerProvider.kt
7. Feature JVM proof:
   app/src/test/kotlin/com/vextreme/vexlife/android/vexstream/experience/v2/VexStreamExperienceTest.kt
8. Human Journey:
   app/src/journeyTest/kotlin/com/vextreme/vexlife/android/testing/journey/catalog/shared/vexstream/VexStreamJourneys.kt

What to copy from VexStream:

~~~text
human-first naming
explicit ownership boundaries
feature-local meaning before global abstraction
contract/provider separation
proof follows claim
comments that preserve non-obvious naming reasons
~~~

What NOT to copy blindly:

~~~text
Library
Sources
PlayerArchitecture
Playback
Queue
Media3
the exact package depth
the exact test matrix
~~~

A different feature should answer the same authoring questions with its own truthful nouns.

<!-- [VXG RealForever][FEATURE-HOME][FEATURE-AUTHORING] -->
