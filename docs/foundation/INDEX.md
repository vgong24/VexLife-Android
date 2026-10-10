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
| F0_STANDALONE_BUILD_FOUNDATION | Standalone Android build/toolchain | **CHECKPOINT EARNED / TEST PROOF PENDING** | Reproducible standalone Android build/device base | architecture regression + remaining F0 unit proof |
| F1_RELAY_FIRST_PRINCIPLES | `architecture/relay` | **REVIEWER SOURCE PASS / EXECUTION PROOF PENDING** | Source-of-Truth Relay + inspectable acyclic Compound topology + transactional recursive lifetime | `:architecture:relay:test` + exact-head build/device proof |
| F2_VEX_APP_SCOPE | `architecture/execution` | **REVIEWER SOURCE PASS / EXECUTION PROOF PENDING** | Structured named coroutine ownership, bounded ref registry, supervision, cancellation and opt-in restart | `:architecture:execution:test` + exact-head build/device proof |
| F3_VEX_LOGGER | `architecture/observability` | **REVIEWER SOURCE PASS / EXECUTION PROOF PENDING** | Bounded ordered architecture diagnostics without turning observability into correctness | `:architecture:observability:test` + cross-foundation exact-head proof |
| F4_CODE_COVERAGE | test/coverage foundation | **NEXT AFTER F1-F3 CHECKPOINT** | Make untested architecture branches perceivable before broad Compose construction | select current compatible tooling + repeatable reports |
| F5_STARTUP_READINESS | startup/readiness sequencing | **HELD_FOR_REAL_ASYNC_CONSUMER** | Keep DI construction distinct from initialization/readiness/accepting work | first real SDK/service bootstrap consumer |
| F6_RELAY_OBSERVATION_IDENTITY | Relay depth | **HELD_FOR_EXTERNAL_TRUTH_CONSUMER** | Distinguish UNOBSERVED/EMPTY/PRESENT/HELD/UNAVAILABLE | Home/network/database/sensor-backed truth |
| F7_DURABLE_OPERATION_REENTRY | operation/effect/recovery depth | **HELD_FOR_REAL_EFFECT_CONSUMER** | Preserve operation != attempt != coroutine across process/lifetime loss | real durable effect/re-entry consumer |
| F8_VIEWS_CONTAINERS | `views/container`, `views/list` | **NEXT PRODUCT PHASE AFTER COVERAGE** | Recursive Compose containers, Tooltip/Snackbar anchoring and reusable screen cells | base container + Tooltip Design Lab proof |
| F9_DESIGN_LAB | executable architecture/component gallery | REFERENCE_PROVEN | Visual proof/examples for reusable foundations and views | rebuild from accepted standalone foundations |
| F10_HOME_TRANSPORT | real Home/network/session integration | HELD | Explicit transport/identity/permission/failure contracts | later |
| F11_MODEL_COMPANION | model/AI companion integration | HELD | Separate model/runtime ownership and real integration contracts | later |

## F1 — Relay from first principles

Canonical source/readme:

`architecture/relay/README.md`

Current review shape:

```text
VexDeepCopyable<Self>
  -> explicit transitive copy contract for nested mutable ownership

VexState<Self>
  -> independently Relay-owned state
  -> extends VexDeepCopyable<Self>

VexRelay<State>
  -> one VexRelayRef Source-of-Truth owner
  -> serialized send/update
  -> explicit semantic equality compatible with StateFlow conflation
  -> prepare-then-commit defensive publication
  -> overridable processState only for bounded semantic admission/derivation
  -> currentState: StateFlow for latest observation

VexCompoundState<InputState, OutputState>
  -> retained resolved input family + derived output family

VexCompoundRelay<InputState, OutputState>
  -> stable declared child-Relay topology
  -> nullable partial input patches
  -> merge against currentInputState
  -> processStates(...) as bounded derivation only
  -> one next compound totality
  -> preflight cycle / Relay-ref / observer-ref identity
  -> Compound-child leases enforce leaf-before-parent activation
     and parent-before-child deactivation
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
semantics. Compound dependency observation receives a VexExecutionOwner. The current app-wide
implementation is VexAppScope, which owns the named coroutine collectors and their lifetime.

Deep-copy safety is transitive: Kotlin data-class copy() is shallow. A nested value that owns mutable
children must recursively detach them (for example through VexDeepCopyable) or be truly immutable.

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

## F2 — VexAppScope / execution ownership

Canonical:

- architecture/execution/README.md
- architecture/execution/examples/APP_SCOPE.md
- architecture/execution/src/test/.../VexAppScopeTest.kt

Current reviewed shape:

~~~text
named execution owns nested coroutine descendants
stable VexCoroutineRef remains reserved through terminal bookkeeping
cancel during restart delay -> CANCELLED
ordinary Exception may opt into bounded restart
JVM Error -> terminal FAILED / no restart
stopApplication closes admission before cancelling tree
post-stop launch fails closed
registry exposure is defensive
executions StateFlow owns state observation
diagnostics cannot become execution ownership
~~~

## F3 — VexLogger / structured observability

Canonical:

- docs/foundation/F3-OBSERVABILITY.md
- architecture/observability/README.md
- architecture/observability/examples/LOGGER_AND_INTERCEPTOR.md
- architecture/observability/src/test/.../VexLoggerTest.kt

Current reviewed shape:

~~~text
VexLogger singleton
-> injectable serialized VexLogExecutor
-> production single-thread executor with bounded pending admission
-> producer-time timestamps + diagnostic recording sequence
-> explicit rejected-log count
-> structured stable refs
-> bounded recent record count + filters

Relay + VexAppScope emit owner facts automatically
Flow.vexLog(...) optionally records bounded observer receipt
ordinary diagnostic failure != semantic/execution failure
~~~

## What remains after F1-F3

Read:

docs/foundation/POST-F1-F3-GAPS.md

The existence of held gaps does not automatically prevent an F1-F3 checkpoint. The review/proof
question is whether these three modules correctly enforce the contracts they claim.

## F4 — Code coverage (next tooling phase)

After F1-F3 reviewer clearance and Gradle proof, establish coverage before broad Compose work.

Coverage is a test-perception tool, not semantic acceptance.

Do not pin a coverage plugin/version until current Kotlin/AGP compatibility is freshly checked.

## Held — startup/readiness

~~~text
KOIN_RESOLVED != INITIALIZED != READY != ACCEPTING_WORK
~~~

Do not hide readiness inside Koin, Relay, or VexAppScope.

## Held — observation identity

StateFlow initial/default value may not prove a real source has been observed.

Preserve room for:

~~~text
UNOBSERVED / EMPTY / PRESENT / HELD / UNAVAILABLE
~~~

## Held — durable operation/re-entry

~~~text
COROUTINE != OPERATION != ATTEMPT != DURABLE_IDENTITY
~~~

Do not make Relay or VexAppScope persistence machinery.


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
