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
| F1_RELAY_FIRST_PRINCIPLES | `architecture/relay` | **SOURCE_FORMED / VICTOR REVIEW PENDING** | Give one component a small readable input -> current output relationship before adding mature runtime machinery | `:architecture:relay:test` + app screen-enter/exit device proof |
| F2_RELAY_OBSERVATION_IDENTITY | Relay depth: observation/snapshot identity | HELD_UNTIL_F1_APPROVED | Add UNOBSERVED/EMPTY/PRESENT and state/instance/revision/transition identity without changing the parent mental model | prove only after F1 shape is accepted |
| F3_RELAY_OPERATION_EFFECT | Relay depth: accepted work/effect admission | HELD_UNTIL_REAL_CONSUMER | Preserve operation != attempt != coroutine and explicit result admission when a real component needs effect work | component-driven proof |
| F4_VIEWS_CONTAINERS | `views/container` family | DESIGN_LINEAGE_ONLY | Reuse container/anchor behavior for Tooltip/Snackbar without hiding component meaning inside generic utilities | source-place after Relay review |
| F5_DESIGN_LAB | debug/developer component gallery | REFERENCE_PROVEN | Provide a native place to exercise building blocks and inspect bounded local results | port only after F0-F4 provide its dependencies |
| F6_HOME_TRANSPORT | real Home/network/session integration | HELD | Requires explicit real transport, identity, permission, and failure contracts | not part of bootstrap |
| F7_MODEL_COMPANION | model/AI companion integration | HELD | Requires separate model/runtime ownership and real integration contracts | not part of bootstrap |

## F1 — Relay from first principles

Canonical source/readme:

`architecture/relay/README.md`

The first public shape is intentionally tiny:

```text
named partial component changes
-> component-specific VexRelay subclass
-> one InputState family
-> processLatestState(...)
-> one OutputState family
-> VexRelayState<InputState, OutputState>
-> observer
```

The sample app uses Activity `onStart/onStop` as the smallest real relationship:

```text
ENTER / EXIT
-> VexRelay
-> ENTERED / EXITED
-> Compose reads current output
```

A component can later make its InputState relational (for example Tooltip screen + events +
interaction + target) without changing the Relay into a giant runtime API.

The component subclass exposes meaningful send methods rather than a public replace-whole-state API.
The base Relay serializes those partial transformations against the latest aggregate input and
requires explicit defensive snapshot/deep-copy rules for InputState and OutputState.

This F1 review therefore tests both:
- human organization: one Relay family answers state-of-what;
- mechanical safety: concurrent sibling updates cannot overwrite each other by starting from a
  stale whole input object.

External freshness remains component-local: serial arrival order is not automatically semantic
truth order.

The previous draft `:sdk:state` direction was explicitly superseded because it made an
implementation fragment the top-level concept. State projection, observation identity,
operation/effect admission, durability, and surface projection remain deeper Relay concerns to add
only when their concrete relationship earns them.

### Next component lineage after Relay review

These are recognized design scopes, **not source-implemented modules yet**:

```text
views/
  container/
    base/
    tooltip/
    snackbar/
  list/
    base/
```

The design lineage already supports this direction: Tooltip geometry separates semantic arbitration
from physical placement; VexContainer/VexTransientHost/Tooltip/Snackbar were proposed as reusable
Compose primitives; repeated-item identity remains item/occurrence/slot based rather than position
based.

Do not implement these until Victor has reviewed the Relay parent shape.

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
