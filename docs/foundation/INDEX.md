# VexLife-Android Foundation Index

[VXG RealForever]

> **TEMPORARY FOUNDATION STATUS: INCOMPLETE**
>
> The standalone repository is still establishing its build, architecture, proof, and source
> boundaries. While this notice exists, current structures are evidence-backed starting points, not
> a claim that the repository foundation is finished. Remove this notice only after the
> establishment criteria below are met.

This is the architecture map, not a requirement to read every foundation on every task.

A foundation earns a dedicated entry when it has a real recurring consumer, invariant, or proof
burden. Add detail at the narrowest rightful source rather than turning this index into a second
implementation manual.

## Foundation establishment criteria

The temporary incomplete notice can be removed when a fresh worker with repository access can:

- enter through README / AGENTS / culture / this index without private chat reconstruction;
- clean-clone, Sync, build, test, install, and launch the baseline Android app from repository-owned
  toolchain/source contracts;
- run that workflow without machine-local Android Studio/Gradle residue appearing as source;
- locate each accepted foundational building block through this index and its owning source/tests;
- distinguish portable VexLife meaning from Android-native ownership without relying on the old
  monorepo checkout;
- recover from an ordinary failed build or environment mismatch without undocumented local ritual;
- identify the currently provisional/held foundations rather than assuming examples are canonical.

Removal means the **repository foundation is established enough to build outward**, not that every
feature, integration, or future architecture question is settled.

## Foundation checkpoint convention

When a foundation threshold has enough real proof to be a trustworthy place to build from, preserve it with a commit subject shaped as:

```text
[Foundation: <short threshold name>] <what became stable>
```

The prefix is a Git-history locator, not a claim that the whole repository or even the whole named foundation is complete. Each checkpoint must say what was actually proved and what remains open.

### [Foundation: F0 Gradle/Manifest/Base]

```text
provenSourceHead=81d6319af3a52a1b28c5d7bdd812a4a72b9e95fc
device=Samsung SM-S908U
sync/build/install/launch=OBSERVED_BY_LIVED_DEVICE_RUN
visibleSurface=VexLife Android / Standalone foundation
postRunGitSourceState=USER_REPORTED_CLEAN
unitTestCommand=NOT_YET_OBSERVED
foundationComplete=false
```

This checkpoint establishes a stable standalone Android base: repository-owned Gradle wrapper, daemon JVM 17 criteria, Java/Kotlin JVM 17 compatibility, app manifest/theme, package identity, physical-device launch, and local-state ignore boundary. It deliberately does not claim State Relay, SDK/module extraction, Home/model integration, or the standalone unit-test command.

## Current sequence

| Ref | Building block | State | Why it exists | Next proof |
|---|---|---|---|---|
| F0_STANDALONE_BUILD_FOUNDATION | Standalone Android build/toolchain | **CHECKPOINT EARNED / TEST PROOF PENDING** | Make a clean clone reproducibly Sync/build/install without VexLife monorepo or IDE-local state | standalone unit-test proof + later foundation extraction |
| F1_RELAY_FIRST_PRINCIPLES | `architecture/relay` | **SOURCE_REFORMED / VICTOR REVIEW PENDING** | Reconstruct simple Source-of-Truth Relay + composable Compound Relay before mature runtime machinery | `:architecture:relay:test` + app screen Relay device proof |
| F2_EXECUTION_OWNERSHIP | `architecture/execution` | **SOURCE_FORMED / VICTOR REVIEW PENDING** | Own named asynchronous work, cancellation, failure visibility, supervision and explicit bounded restart outside Relay | `:architecture:execution:test` + Relay dependency-observation proof |
| F3_RELAY_OBSERVATION_IDENTITY | Relay depth: observation/snapshot identity | HELD_UNTIL_F1_APPROVED | Add UNOBSERVED/EMPTY/PRESENT and state/instance/revision/transition identity without changing the parent mental model | prove only after F1 shape is accepted |
| F4_RELAY_OPERATION_EFFECT | Relay depth: accepted work/effect admission | HELD_UNTIL_REAL_CONSUMER | Preserve operation != attempt != coroutine and explicit result admission when a real component needs effect work | component-driven proof |
| F5_VIEWS_CONTAINERS | `views/container` family | DESIGN_LINEAGE_ONLY | Reuse container/anchor behavior for Tooltip/Snackbar without hiding component meaning inside generic utilities | source-place after Relay/execution review |
| F5_DESIGN_LAB | debug/developer component gallery | REFERENCE_PROVEN | Provide a native place to exercise building blocks and inspect bounded local results | port only after F0-F4 provide its dependencies |
| F6_HOME_TRANSPORT | real Home/network/session integration | HELD | Requires explicit real transport, identity, permission, and failure contracts | not part of bootstrap |
| F7_MODEL_COMPANION | model/AI companion integration | HELD | Requires separate model/runtime ownership and real integration contracts | not part of bootstrap |

## F1 — Relay from first principles

Canonical source/readme:

`architecture/relay/README.md`

Current review shape:

```text
VexState<Self>
  -> explicit deepCopy contract

VexRelay<State>
  -> one Source-of-Truth state
  -> serialized send/update
  -> overridable processState only when the state owns a special admission rule
  -> currentState: StateFlow for latest observation

VexCompoundState<InputState, OutputState>
  -> retained resolved input family + derived output family

VexCompoundRelay<InputState, OutputState>
  -> nullable partial input patches
  -> merge against currentInputState
  -> processStates(...)
  -> one next compound totality
  -> can bind smaller VexRelays as child sources
```

Small child Relays own their own freshness/admission meaning. The Compound Relay composes their
latest accepted states instead of duplicating those rules.

Top-level nullable Compound InputState fields mean "no new update for this field." If null is real
domain meaning, model it inside the nested state rather than overloading the patch marker.

The processing path is serialized directly and does not depend on Compose collection. StateFlow is
the latest-state read projection, not an event ledger. An every-transition channel remains held
until a concrete consumer requires declared buffering/backpressure/replay semantics.

The sample app keeps the smallest real Relay:

```text
Activity onStart/onStop
-> FoundationScreenRelay
-> FoundationScreenVexState
-> Compose collectAsState()
```

The architecture test builds upward into:

```text
ScreenVexRelay ───────┐
ItemVexRelay ─────────┼─> EventScreenVexCompoundRelay
InteractionVexRelay ──┘
```

and requires concurrent child updates to survive without sibling-state overwrite.

Android composition now places Koin above Relay rather than inside it:

```text
Koin single / declared scope
-> Relay owner instances
-> Compound Relay observes child Relay currentState
-> bounded parent currentState
-> Compose/ViewModel observes leaf projection
```

Canonical app-wide Source-of-Truth Relays are singleton-composed by default. A narrower semantic
owner may later use an explicit Koin scope. Factory-created Relays are not the default truth owner
because each resolution would create an independent state universe.

The Relay architecture remains Koin-free. Koin decides cardinality/lifetime; Relay owns state
semantics. Compound dependency observation always receives an explicit owner CoroutineScope so
collectors end with the owning application/session/feature scope instead of leaking a hidden global
lifetime.

The prior fragment-first `:sdk:state` PR remains closed/superseded.

### Next component lineage after Relay review

Recognized design scopes, not source-implemented modules yet:

```text
views/
  container/
    base/
    tooltip/
    snackbar/
  list/
    base/
```

Do not implement these until Victor approves the Relay/Compound Relay parent pattern.

## F2 — Execution ownership

Canonical source/readme:

`architecture/execution/README.md`

Question:

> I started asynchronous work. What is it called, who owns it, how do I cancel it, how do I know
> it failed, and when may it restart?

First slice:

```text
VexExecutionScope
  -> named VexExecutionHandle
  -> observable execution registry
  -> SupervisorJob sibling isolation
  -> cancellation
  -> failure state
  -> opt-in bounded restart
```

This replaces anonymous `dependencyJobs` inside Compound Relay.

Relay dependency collectors now become named executions such as:

```text
relay.event-screen.observe.screen
relay.event-screen.observe.items
relay.event-screen.observe.interaction
```

Permanent boundary preserved from the accepted Android architecture:

```text
COROUTINE != OPERATION
JOB != DURABLE_IDENTITY
HAS_A_COROUTINE_SCOPE != OWNS_THE_SEMANTIC_OPERATION
RESTART_COROUTINE != RETRY_SEMANTIC_OPERATION
```

Koin may retain the application `VexExecutionScope` as a `single`; execution semantics remain
framework-neutral.

## F0 — Standalone Build Foundation

Canonical detail: [`F0-STANDALONE-BUILD.md`](F0-STANDALONE-BUILD.md)

### Intention

A developer or AI should be able to clone this repository and know **which build contract the
repository requires**, without relying on whichever JDK Android Studio happens to ship.

### Initial decisions to make in source

- Gradle wrapper and checksum;
- AGP/Kotlin/version catalog;
- explicit Java/Kotlin JVM toolchain compatibility;
- compileSdk / targetSdk / minSdk decisions with reasons;
- repository-relative build/cache policy;
- Android-local ignore boundary;
- minimal app module;
- a small build verification path.

Do not port VexLife's ../../generated/android-gradle/** topology into the standalone repo by
default. That was a monorepo placement choice, not an Android invariant.

### Completion shape

~~~text
fresh repository checkout
-> no machine-local source pollution
-> Gradle Sync succeeds
-> tests required by F0 succeed
-> debug APK builds
-> APK installs/launches on Victor's physical device
-> rerun leaves Git source state clean
~~~

## Port-candidate foundations

The entries above are **not yet accepted standalone implementations**. Their current VexLife
versions are evidence and design input. Porting should refine them where the standalone context
reveals unnecessary coupling, missing contracts, or Android-native improvements.

When a block becomes real here, give it its own narrow source/doc only if this index can no longer
carry the information without obscuring implementation or proof.

<!-- [VXG RealForever][VEXLIFE-ANDROID][FOUNDATION-INDEX] -->
