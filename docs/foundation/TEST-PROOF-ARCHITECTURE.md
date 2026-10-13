# Test / Proof Architecture

[VXG RealForever]

## The question

> How does VexLife prove a feature at the cheapest truthful owner, then compose those proofs into
> confidence without making unit tests, screenshots, journeys, coverage, and device evidence repeat
> the same claim?

The governing rule is:

~~~text
TEST THE TRANSFORMATION AT ITS OWNER
~~~

A proof layer should answer one question well. More expensive layers verify relationships that
cheaper layers cannot own; they do not re-implement lower-layer meaning merely to increase test
count.

## Current proof topology

~~~text
architecture contract
  reusable primitive invariants
        |
        v
feature/domain JVM test
  input -> process -> semantic output
        |
        v
presentation projection
  semantic output -> UI-facing state
        |
        v
Compose render contract
  known UI-facing state -> semantic Android surface
        |
        v
Journey
  human/external steps -> real runtime/logging behavior
        |
        +-------------------+
        |                   |
        v                   v
deterministic visual     ScreenCrawler
golden                   integrated environment evidence
        |                   |
        +---------+---------+
                  v
       native / physical-device proof
       when Android/provider reality owns the claim
~~~

Line/branch coverage is orthogonal. It reports executable code that the selected tests did not
exercise; it is not an oracle for correctness and does not replace any proof layer above.

## What each layer owns

### Architecture contracts

Reusable modules prove their own laws once.

Examples:
- Relay serialization, projection and dependency guarantees;
- reusable element identity/placement mechanics;
- logging invariants.

Feature tests consume those laws rather than re-proving the generic kernel.

### Feature/domain JVM proof

Prefer deterministic JVM proof for semantic relationships that do not require Android reality.

Relay-shaped behavior is especially cheap to prove here:

~~~text
input facts
-> synchronous/serialized process
-> output meaning
~~~

Events convergence, Maps navigation/style convergence, playback projection, route matching and
provider translation belong here when their claim is pure Kotlin/domain meaning.

### Presentation projection

When a feature has meaningful domain output and a non-trivial Android presentation shape, keep the
projection testable independently:

~~~text
semantic output
-> presentation/UI state
~~~

This lets rendering tests start from a known presentation state instead of re-driving every domain
transition.

### Compose render contract

A render-contract test asks whether a known state becomes the correct Android semantic surface:
stable element identity, visibility, enabled/disabled state, text/content semantics, and action
addressability.

It does not re-prove the Relay/domain reducer and does not substitute for visual appearance.

### Journey

Journey owns human-readable semantic walks and their real Android/runtime/logging realization.

One declaration feeds:
- fast JVM semantic-map coverage;
- independent expected-walk interpretation;
- baseline Compose instrumentation;
- downstream ScreenCrawler evidence.

Journey is not a feature-state reducer and ScreenCrawler does not own a second traversal language.

### Deterministic visual golden

A visual golden, when admitted, should represent a **meaningful named conditional state**, not every
Composable merely because it exists.

Examples:

~~~text
vexstream.player.playing
vexstream.player.paused
vexmaps.style.failed
vexmaps.navigation.active
~~~

Its question is:

> Given this controlled state and render configuration, are the current pixels the approved visual
> result?

Reference replacement is a judgment operation. A mismatch must not automatically rewrite the
reference.

The initial implementation/tool choice remains a candidate until it proves compatible with the
repository's current Android/Kotlin toolchain and local workflow.

### ScreenCrawler

ScreenCrawler captures integrated screenshots from accepted Journey execution across selected
locale/font conditions. It is evidence acquisition, not the semantic oracle and not automatically a
pixel-diff authority.

Its stable scenario/configuration/evidence identity is a candidate for promotion only when a second
real visual-evidence consumer needs the same meaning.

### Native / lived proof

Use instrumentation/device/provider execution where the claim is inherently native:
permissions, services, notifications, MediaSession, AndroidView/MapLibre behavior, lifecycle,
storage and physical GPS/navigation behavior.

Do not build a JVM imitation merely to avoid the rightful substrate.

## Canonical local commands

Repository-owned Gradle tasks are the source of the ordinary proof bundle. Hosted CI may call these
tasks later; CI YAML must not become a second definition.

~~~bash
./gradlew vexVerifyJvm
~~~

Runs:
- `:app:verifySourceAlignment`;
- `:architecture:elements:test`;
- `:architecture:logging:test`;
- `:architecture:relay:test`;
- `:app:testDebugUnitTest`.

~~~bash
./gradlew vexVerifyLocal
~~~

Runs the JVM/source bundle and assembles the debug APK. It requires no connected Android target.

Connected Journey/device proof remains separate because it owns a different execution substrate:

~~~bash
./gradlew :app:connectedDebugAndroidTest
~~~

ScreenCrawler remains opt-in; see `SCREEN-CRAWLER-EVIDENCE.md`.

## Feature-scoped proof

Do not create another feature registry merely for tests.

Use existing ownership:
- source/test packages for focused JVM tests;
- typed `Journeys.<domain>.<path>` and Journey shards for human paths;
- registered Screens for ScreenCrawler selection;
- feature-owned preview/state fixtures for deterministic render cases.

Whole-system proof composes those same owners.

## Coverage direction

No JVM coverage plugin is accepted yet.

The next spike should compare a Kotlin/Android-friendly local report path with direct JaCoCo against
the repository's actual toolchain. The first accepted outcome should be a useful local report, not an
arbitrary global percentage gate.

Coverage should help answer:

> Which executable source did none of our intended proof surfaces exercise?

It should not incentivize tests for trivial getters or duplicate higher-level behavior just to raise
a number.

## First exemplar consumers

VexStream and VexMaps are deliberately different enough to pressure this foundation.

~~~text
VexStream
  playback/session meaning
  -> playback presentation state
  -> playing/paused render contract
  -> deterministic visual state
  -> Journey / ScreenCrawler / native MediaSession proof

VexMaps
  style/navigation semantic convergence
  -> map/planner presentation state
  -> render contract
  -> deterministic visual state where provider-independent
  -> Journey / ScreenCrawler
  -> MapLibre/location/service/device proof where native
~~~

If an abstraction serves only one of these, keep it feature-local until another real consumer earns
promotion.

<!-- [VXG RealForever][VEXLIFE-ANDROID][TEST-PROOF-ARCHITECTURE] -->
