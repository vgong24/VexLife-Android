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
| F1_UI_BLUEPRINT_HOME | element cells + placement + first Home practicum | **MERGED / LIVED DEVICE RUN OBSERVED / AGGREGATE TEST PROOF PENDING** | Make UI identity, placement, intent, navigation and diagnostics human-perceivable before feature depth | aggregate Gradle proof + clean rerun before any formal foundation checkpoint |\n| VEX_ARCHITECTURE_V2 | Experience -> Player Architecture -> typed capabilities/providers + VexStream Legacy/V2 practicum | **DRAFT EXPERIMENT / SOURCE PROOF PENDING** | Make the codebase walkable from human concern -> current truth -> requestable meaning -> rightful realization owner without one giant state/handler map | exact vexVerifyJvm/vexVerifyLocal + lived Legacy/V2 VexStream comparison + second independent concern consumer before broad promotion |
| DATA_REPOSITORY | domain repositories + Room local persistence + reusable data fixtures | **MERGED / LIVED PARTIAL / INTEGRATION PROOF PENDING** | Let screens observe data without knowing DAO/storage/backend details; first consumer is Log History | aggregate unit/build + Room migration/integration proof + clean lived rerun |
| TEST_PROOF_ARCHITECTURE | local-first proof topology + reusable verification entry points | **EXTRACTION CANDIDATE / A019 INDEPENDENT HOST PROOF OBSERVED** | Keep semantic, render, journey, visual, coverage and device proofs distinct while making the normal proof path reusable | exact convergence-branch host proof before merge |
| JOURNEY_INSTRUMENTATION | semantic walk grammar + map coverage + Compose instrumentation | **MERGED / CONNECTED DEVICE 13/13 OBSERVED** | Make real Android journeys executable once while keeping semantic coverage debt visible and non-blocking by default | extend journeys with new features + preserve baseline/device proof |
| ANDROID_HOST_PROOF | repository-owned Android proof requests carried by current VexRelay/VexLocalBridge | **EXTRACTION CANDIDATE / EVENTS A008 + TEST FOUNDATION A019 EVIDENCE** | Let any feature request exact Android repository/device proof without rebuilding host-carriage mechanics or making Victor interpret routine output | exact convergence-branch host proof before merge |
| EVENTS_CONVERGENCE_PRACTICUM | external-information ingress + Relay composition + stable dynamic list + anchored attention | **DRAFT CANDIDATE / PURE KOTLIN SMOKE PASS / ANDROID PROOF PENDING** | Master one convergence node before reverse-building real Messaging/Calls/Maps/Health/VexStream adapters toward it | exact Gradle/Journey/ScreenCrawler + physical-device attention walk |
| F2_STATE_PROJECTION | StateFlow/state projection kernel | PORT_CANDIDATE | Preserve one-owner observable semantic state and emission discipline | isolated JVM/native contract tests |
| F3_INTENTION_OPERATION_RESULT | intention -> operation -> attempt -> result grammar | PORT_CANDIDATE | Keep requested meaning, execution identity, retry identity, and result admission separate | deterministic operation/admission tests |
| F4_COMPOSITION_BOUNDARY | composition root / ViewModel / lifecycle boundary | PORT_CANDIDATE | Keep Android lifecycle wiring separate from product semantic ownership | lifecycle + ViewModel contract tests |
| F5_LOCALIZATION_PRESENTATION | portable localization mapping beyond Android @StringRes + native presentation | PORT_CANDIDATE | Keep visible language and cross-platform string identity source-addressable | resource/localization + Compose tests |
| F6_DESIGN_LAB | debug/developer component gallery | FIRST PRACTICUM INCLUDED IN F1 | Exercise building blocks and inspect their registered topology without creating another truth map | prove F1 Gallery + AppGraph on device |
| F7_HOME_TRANSPORT | real Home/network/session integration | HELD | Requires explicit real transport, identity, permission, and failure contracts | not part of bootstrap |
| F8_MODEL_COMPANION | model/AI companion integration | HELD | Requires separate model/runtime ownership and real integration contracts | not part of bootstrap |

## VexArchitecture V2 — VexStream Experience / Media Player practicum

Canonical detail: architecture/vexarchitecture/README.md

### Intention

V2 separates human/product composition from architectural semantics:

~~~text
VexStreamExperience
├── Library
├── Sources
└── VexStreamPlayerArchitecture
    ├── Playback
    └── Queue
~~~

Only Player is named Architecture at this level. Playback and Queue are typed capability realms below that one architecture.

Library is the known catalog identity; Sources answer where media can come from. Connection is a remote-source state and Availability is derived from source ability, so neither is promoted into a peer architecture merely because the facts matter.

The reusable player boundary separates semantic contracts from runtime providers:

~~~text
VexMediaPlayerArchitecture.*Contract
!=
VexMedia*Provider
~~~

The current provider is Media3; future SDK/provider adapters can implement the same narrow capability contracts without changing VexStream semantic vocabulary.

PR #80 remains predecessor quarry for root-only discoverability, Case / Control / Placement and derived-map lessons. PR #81 V2 is a fresh candidate from accepted main.

Debug Menu retains Legacy and V2 simultaneously. Both use the same UI, repositories, MediaController, MediaSessionService and ExoPlayer; only the semantic request route changes.

Still owed:
- exact vexVerifyJvm;
- exact vexVerifyLocal;
- lived Android Legacy/V2 comparison;
- second independent consumer before broad contract promotion.

## F1 — UI Blueprint / Home Practicum

Canonical detail: [`F1-UI-BLUEPRINT-HOME.md`](F1-UI-BLUEPRINT-HOME.md)

### Intention

A developer should be able to answer "what is this element, where is it placed, what can it do, and
what screen was actually active when it was used?" from one small blueprint path instead of
repeating those facts across Compose, navigation, tests, and logs.

The first source candidate grew from reusable cells into one real Home shell and has now been merged.
Lived-device runs exercised the phone-safe AppFrame, primary screen pager, shared Gallery/AppGraph path,
nested directional Back, and native Android Back resolution. That is meaningful lived evidence, but
the repository still owes the aggregate Gradle proof/clean rerun before this row should become a
formal `[Foundation: ...]` checkpoint.

## Data / Repository candidate

Canonical detail: [`DATA-REPOSITORY.md`](DATA-REPOSITORY.md)

### Intention

The UI should depend on domain-specific repository contracts, not Room DAOs/entities. The first
consumer persists readable Log History while keeping database failure non-authoritative. The same
domain fixture shape should seed previews, unit tests, and in-memory repository tests without
creating a second mock data model.

No generic `BaseRepository<T>` is admitted by this candidate.

## Android host-proof convergence candidate

Canonical detail: [`ANDROID-HOST-PROOF.md`](ANDROID-HOST-PROOF.md)

### Intention

A feature lane should declare **what Android claim needs proof** while the repository owns the
canonical Gradle/device proof entry points and the live Vextreme-SDK contract owns task/RETURN
carriage. Events A008 and Test Foundation A019 independently established the reusable boundary.

The extracted source still requires its own exact host proof before merge. Once accepted on main,
feature branches such as VexMaps should rebase onto this foundation rather than copying another
lane's Relay package formation or proof command list.

## Test / Proof Architecture candidate

Canonical detail: [`TEST-PROOF-ARCHITECTURE.md`](TEST-PROOF-ARCHITECTURE.md)

### Intention

A developer should be able to prove feature meaning, Android rendering, user journeys, visual
stability and executable-code coverage at the narrowest truthful owner, then invoke the ordinary
source/JVM/build bundle through one repository-owned local command. CI remains a caller rather than
another proof definition.

The current candidate converges already-earned Relay, Journey and ScreenCrawler test surfaces. It
does not yet claim accepted code-coverage tooling, deterministic visual-golden tooling, or native
device proof.


## ScreenCrawler evidence candidate

Canonical detail: [`SCREEN-CRAWLER-EVIDENCE.md`](SCREEN-CRAWLER-EVIDENCE.md)

### Intention

A reviewer or localization/accessibility workflow should be able to select all canonical shards, a
typed subset of feature shards, or one or more registered Screens, then execute those accepted
semantic walks under chosen locale/font profiles and capture traceable screenshot evidence.

This matrix is opt-in. Ordinary journey instrumentation remains baseline-only by default.

## Journey / Instrumentation candidate

Canonical detail: [`JOURNEY-INSTRUMENTATION.md`](JOURNEY-INSTRUMENTATION.md)

### Intention

A developer should be able to describe a walk through the app once, have that walk generate its own
expected semantic history, execute the same declaration through real Compose instrumentation, and
receive a fast JVM failure when a registered Screen/action/editable field/modal is absent from every
known journey.

This is semantic app-map coverage. It remains separate from ordinary line/branch code coverage.


## Events Convergence Practicum candidate

Canonical detail: [`EVENTS-CONVERGENCE-PRACTICUM.md`](EVENTS-CONVERGENCE-PRACTICUM.md)

### Intention

A developer should be able to trace one external-information path from provider identity through
normalized current truth, Relay processing, stable repeated-item identity, current presentation
eligibility and anchored evidence without making Events the owner of the originating service.

This is the first current-main Relay consumer and a reverse-protocol reference for future real
service adapters. The current refinement makes its Compound Relay session-owned and declarative
about repository/Flow dependencies, while keeping `processStates()` as the synchronous serialized
semantic reducer and keeping Compose as an output/presentation observer. It also exercises a real
cross-domain UserProfile name-change input. It remains a draft/refinement candidate until exact
Gradle/Android proof closes.

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
