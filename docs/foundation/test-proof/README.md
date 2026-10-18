# Test / Proof Foundation

[VXG RealForever]

This directory is the single documentation doorway for the Test / Proof Foundation.

~~~text
test-proof/
├── README.md
│   current proof architecture
├── INCIDENTS.md
│   incident grammar / recurrence law / index
└── incidents/
    └── TF-INCIDENT-*.md
        one independently evolving incident history
~~~

The repository-wide atlas is `../INDEX.md`. Do not add another Test Proof peer document directly
under `docs/foundation/` when the meaning belongs inside this package.

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

Its question is:

> Given this controlled state and render configuration, are the current pixels the approved visual
> result?

Reference replacement is a judgment operation. A mismatch must not automatically rewrite the
reference.

The shared foundation wires the official Compose Preview Screenshot Testing standalone plugin at
`0.0.1-alpha16`, because the repository's current AGP 9.4 / Kotlin 2.4 / JDK 17 toolchain supports
the underlying Gradle tasks without moving the whole build to AGP 9.5 alpha test suites.

This is explicitly a compatibility bridge, not a forever plugin choice.

The independent Test Foundation candidate on PR #76 previously proved two VexStream deterministic
cases (`vexstream.player.playing` and `vexstream.player.paused`) and rejected a broader whole-screen
candidate when Layoutlib exposed a native media-files dependency. Those feature-owned screenshot
tests/references are **evidence for this foundation**, not source owned by this extraction.

PR #78 intentionally carries the reusable screenshot task/plugin surface without importing VexStream
feature semantics or accepted reference PNGs. A feature that admits deterministic visual cases keeps
those cases and reference bytes beside the rightful presentation owner.

Routine validation:

~~~bash
./gradlew vexVisualValidate
~~~

Reference formation/update remains explicit:

~~~bash
./gradlew :app:updateDebugScreenshotTest
~~~

No reference PNG becomes accepted merely because the update task generated it.

### ScreenCrawler

ScreenCrawler captures integrated screenshots from accepted Journey execution across selected
locale/font conditions. It is evidence acquisition, not the semantic oracle and not automatically a
pixel-diff authority.

Its stable scenario/configuration/evidence identity is a candidate for promotion only when a second
real visual-evidence consumer needs the same meaning.

## Flakiness is evidence, not something a retry erases

An intermittent proof failure remains part of repository knowledge even when the exact candidate
passes on retry.

Record recurring/intermittent proof incidents in
[INCIDENTS.md](INCIDENTS.md).

The distinction is:

~~~text
retry PASS
    clears "this exact candidate is currently proven broken"

retry PASS
    does NOT clear "this proof foundation has exhibited an intermittent failure"
~~~

An incident stays visible until evidence establishes a root cause and disposition. This protects
future features from assuming a foundation symptom is newly introduced merely because the previous
occurrence was months earlier.

### Native / lived proof

Use instrumentation/device/provider execution where the claim is inherently native:
permissions, services, notifications, MediaSession, AndroidView/MapLibre behavior, lifecycle,
storage and physical GPS/navigation behavior.

Do not build a JVM imitation merely to avoid the rightful substrate.

## Gradle implementation boundary

The root `build.gradle.kts` stays a small plugin/dependency front door. Test Foundation task
orchestration and evidence collection live in:

~~~text
gradle/test-foundation.gradle.kts
~~~

That script may compose existing proof tasks and collect their outputs. It must not accumulate
feature-specific test meaning.

## Canonical local commands


Repository-owned Gradle tasks are the source of the ordinary proof bundle. Hosted CI may call these
tasks later; CI YAML must not become a second definition.

~~~bash
./gradlew vexVerifyJvm
~~~

Runs:
- `:app:verifySourceAlignment`;
- `:architecture:behavior:test`;
- `:architecture:execution:test`;
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
./gradlew vexVerifyDevice
~~~

That task depends only on `:app:connectedDebugAndroidTest`. The connected task itself
`finalizedBy(verifyVexDeviceResults)`, so result inspection is scheduled through Gradle's
failure-safe finalizer relationship rather than as an ordinary aggregate dependency.

That distinction matters: ordinary tasks that depend on a failed task are skipped, while a finalizer
still runs after the finalized task fails. The inspector therefore forms
`build/vex/test-foundation/device-result-summary.txt`, reports exact failing test class/name/message
when JUnit XML exists, and still fails closed when a successful Android task produced zero tests.

A retry PASS does **not** erase an earlier intermittent failure. Durable recurrence history lives in
[INCIDENTS.md](INCIDENTS.md); use it to distinguish a first occurrence from a
known unresolved/intermittent proof symptom.

Android Studio is not required: Android Gradle Plugin discovers the connected emulator/physical
device and drives the instrumentation/adb path. ADB remains useful for device discovery,
install/launch, Logcat and native/device diagnostics, but ScreenCrawler screenshot retrieval stays
on AndroidX PlatformTestStorage rather than raw filesystem pulls.

ScreenCrawler remains opt-in; see `SCREEN-CRAWLER-EVIDENCE.md`.

## Host-proof return

The Test Foundation candidate can be exercised entirely from a terminal. Android Studio does not
need to be open.

### Visual-reference formation is feature-triggered

The shared extraction contains the screenshot proof machinery but intentionally contains no
feature-owned screenshot cases or accepted reference PNGs. Therefore `vexVisualValidate` may be
`NO-SOURCE` on the shared foundation alone; that is not a visual-regression claim.

When a feature admits its first deterministic visual case, reference formation is explicit:

~~~text
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-configuration-cache --continue vexVerifyLocal vexCoverage :app:updateDebugScreenshotTest vexVisualValidate vexTestFoundationProofBundle
~~~

Generated PNGs remain unaccepted candidates until reviewed.

After feature-owned references are accepted, ordinary proof must **not** run the update task first:

~~~text
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-configuration-cache --continue vexVerifyLocal vexCoverage vexVisualValidate vexTestFoundationProofBundle
~~~

That distinction is essential:

~~~text
UPDATE_REFERENCE
  = form a candidate/accept an explicitly reviewed visual change

VALIDATE_REFERENCE
  = detect whether current rendering drifted from accepted pixels
~~~

Running update before every validation would erase the regression signal and is forbidden by this
foundation.

Both commands use the checked-in Gradle wrapper JAR directly, so the human/host route does not need
to choose between `./gradlew` and `.\\gradlew.bat`.

### Host-proof configuration-cache boundary

The exact host-proof command currently disables Gradle configuration cache. That is intentional:
`verifySourceAlignment` performs repository-file scanning and `vexTestFoundationProofBundle`
collects host-local Git/generated evidence at execution time. Those proof/evidence tasks should not
silently dictate whether ordinary product builds use configuration cache.

A future reusable proof-task implementation may earn configuration-cache compatibility. The current
foundation prefers truthful execution over pretending these host-observation tasks are cacheable.

`vexTestFoundationProofBundle` is collection-only. It does not make failed tasks pass and it does
not accept generated reference images. It forms:

~~~text
build/vex/test-foundation/VexLife-Test-Foundation-Proof.zip
~~~

with whatever evidence the run produced:
- aggregate Kover reports;
- app + architecture JVM test result XML;
- connected Android JUnit XML + HTML report when formed;
- `device-result-summary.txt` with exact failing testcase identity when available;
- Journey coverage diagnostics;
- screenshot validation report;
- generated screenshot reference candidates;
- exact Git/JDK/OS metadata.

This gives a host or Relay one returnable file without making CI or Android Studio the proof owner.
A generated golden remains unaccepted until its pixels are explicitly reviewed.

## Feature-scoped proof

Do not create another feature registry merely for tests.

Use existing ownership:
- source/test packages for focused JVM tests;
- typed `Journeys.<domain>.<path>` and Journey shards for human paths;
- registered Screens for ScreenCrawler selection;
- feature-owned preview/state fixtures for deterministic render cases.

Whole-system proof composes those same owners.

## Coverage direction

Kover 0.9.8 remains the repository coverage producer because it understands Kotlin Android variants,
merges modules, and emits both human HTML and JaCoCo-compatible XML. Android on-device
instrumentation coverage remains a separate proof surface.

Current aggregate coverage includes:
- `:app`;
- `:architecture:behavior`;
- `:architecture:elements`;
- `:architecture:execution`;
- `:architecture:logging`;
- `:architecture:relay`.

The repository-level entry point is:

~~~bash
./gradlew vexCoverage
~~~

Alongside Kover HTML/XML, every run produces one replaceable latest-state projection:

~~~text
build/vex/test-foundation/coverage-status.md
build/vex/test-foundation/coverage-summary.json
~~~

The human surface intentionally stays tiny:

~~~text
🟢 >= 90% line coverage
🟡 <  90% line coverage
~~~

**90% is an awareness target, not a correctness/build gate.** Yellow means “look here,” not
“tests failed.” Branch coverage is reported beside lines but does not currently own the color.

The generated block also includes up to ten under-90% classes ranked by **missed executable lines**.
That keeps the PR focused on the largest exposure rather than letting tiny 0% helpers dominate
attention. Full Kover HTML/XML remains the detailed source.

Coverage may also surface **architecture debt context** from an owning source file marker:

~~~text
// VEX-COVERAGE-DEBT: LIVE_REFACTOR_DEBT | <why this live source is intentionally being reshaped>
~~~

That marker is explanatory metadata, not a coverage waiver.

~~~text
LIVE_REFACTOR_DEBT
  production still reaches the code
  -> keep it inside the active coverage denominator
  -> show the debt state/note beside its exposure

DEPRECATED_REPLACED
  only after accepted replacement means production no longer reaches the old code
  -> a future exclusion policy may be admitted when a real case earns it
  -> never silently disappear the debt/excluded code
~~~

No deprecated-code exclusion is implemented merely because the state name exists. A concrete
replacement must first establish that the old source is no longer production-owned.

### Autonomous PR projection after VexRelay

GitHub Actions is not required to publish the coverage state.

When `vexCoverage` is one proof command inside an exact-head VexRelay source-owner proof, its
`VEX-COVERAGE-STATUS` block is already retained in that RUN operation receipt. After F09
independently consumes a PASS canonical RETURN:

~~~text
require return candidate head == current PR head
require exactly one bounded VEX-COVERAGE-STATUS block in the coverage proof receipt

then:
  adapter.direct-provider
  -> append marked coverage block when absent
  -> otherwise replace only the prior marked block
  -> no new routine PR comment
  -> continue F13 Assurance
~~~

If the head moved, the block is missing/ambiguous, or provider capability is unavailable, no
coverage state is published. That holds only the projection effect; it does not relabel a valid Kover
run as failed.

This is intentionally a **repository-specific continuation using the current generic Relay contract**.
VexRelay itself does not gain a coverage-specific schema or provider mutation inside the host proof.

For PR cleanliness, update the generated coverage block in the PR body in place. Do not append one
coverage comment per commit/run. Detailed reports remain in the proof bundle.

### Proving the proof framework

Architecture implementation belongs in aggregate code coverage because it is executable production
foundation that features rely on.

Gradle/test orchestration itself is different: folding build-script lines into the app percentage
would make the number easier to game without proving the framework. Instead,
`vexCoverageSummaryContract` behaviorally proves the coverage parser and the 90% GREEN/YELLOW
boundary, while `vexVerifyJvm` continues to prove architecture-module contracts. The proof bundle
also retains architecture execution test results alongside the other architecture modules.

Coverage should help answer:

> Which executable source did none of our intended proof surfaces exercise?

It should not incentivize tests for trivial getters or duplicate higher-level behavior just to raise
a number.

## Independent exemplar evidence

VexStream and VexMaps were deliberately different enough to pressure the Test Foundation candidate.
Their feature-specific source remains outside this shared extraction:

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
