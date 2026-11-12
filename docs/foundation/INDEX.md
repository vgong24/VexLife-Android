# VexLife-Android Foundation Index

[VXG RealForever]

> **TEMPORARY FOUNDATION STATUS: INCOMPLETE**
>
> The standalone repository is still establishing its build, architecture, proof, and source
> boundaries. While this notice exists, current structures are evidence-backed starting points, not
> a claim that the repository foundation is finished. Remove this notice only after the
> establishment criteria below are met.

This is the architecture map, not a requirement to read every foundation on every task.

For VexArchitecture-level work, descend through
[architecture/vexarchitecture/README.md](../../architecture/vexarchitecture/README.md). The accepted
implementation-origin/reasoning record is
[PR #209](https://github.com/vgong24/VexLife-Android/pull/209); current repository source and tests
own current behavior. PR #209 remains useful before/after provenance so a fresh reader can inspect
rejected alternatives and the reason they were subtracted.

A foundation earns a dedicated entry when it has a real recurring consumer, invariant, or proof
burden. Add detail at the narrowest rightful source rather than turning this index into a second
implementation manual.

### Direct Family / Method — accepted current foundation

Current accepted source:

~~~text
architecture/behavior/FAMILY-CIRCUIT.md
architecture/behavior/src/main/.../VexMethod.kt
architecture/behavior/src/main/.../VexFamily.kt
architecture/behavior/src/test/.../VexMethodHighConfidenceTest.kt
architecture/behavior/src/test/.../VexFamilyHighConfidenceTest.kt
~~~

PR #193 remains accepted lineage for the earlier direct Family refinement. PR #209 is the accepted
subtractive VexArchitecture stability point on top of the #207/#208 review. Current source and tests
own subsequent refinements.

Current accepted call graph:

~~~text
Family.activate(Control)
-> concrete Family.act(CURRENT State, Control)
-> Act?
   -> admitted State?
   -> Begin?
      -> Effect
      -> Independent / Earliest / Latest / Queued
-> immediate semantic State admission
-> Act.Accepted returned
   -> authored Act
   -> relationshipId when required
   -> realizationBegin? while a new occurrence remains owed

realization boundary
-> Act.Accepted.relationshipId + Act.Accepted.realizationBegin + bound Act.Method
   -> specializes VexMethod
-> VexExecution custody
-> concrete realization
-> VexMethod.Result
   -> Success / Failure / Error
-> rightful result interpretation

outside/provider truth
-> rightful observation / reconciliation
-> already-semantic State
-> Family.observe(State)
-> Projection(State, DisplayState)
-> concrete presentation meaning when earned
-> manifestation / UI

ThemeMode display path:
    ThemeMode State
    -> ThemeModeFamily.DisplayState
    -> VexThemePresentation
    -> VexLifeTheme
    -> Compose / system-bar presentation
~~~

Permanent accepted boundary:

~~~text
VexMethod
    owns one fully-bound executable contract
    owns the terminal return contract of VexMethod.invoke():
        Success<T>
        Failure<F>
        Error (behavior-foundation construction only)
    is NOT a generic provider / Store / repository outcome wrapper
    does NOT own Family State or VexExecution custody

VexFamily
    owns Family-scoped Parameter / State / Control / Act / Begin / Effect semantic relationships
    owns current semantic State
    binds maintained RelationshipId and live Family execution owner onto the accepted Act
    exposes Act.Method as the Family specialization of VexMethod

VexExecution
    owns Job custody / overlap / source observation / cancellation

VexFamily
    does NOT own Store/provider mechanics
    does NOT own VexLogger
    does NOT own dependency construction
~~~

ThemeMode is the PR #209 patient zero:

~~~text
Parameter.Mode(mode)
-> Control.Select(the same Parameter.Mode)
-> beginLatest(Effect.Save(the same Parameter.Mode))
-> Act.Method.Save(ThemeModeStore, the exact accepted Effect.Save)
-> Act.Accepted.launch(VexExecutionRoot, Method.Save)
   verifies the exact realization Begin Effect and consumes live Family owner + Approach +
   RelationshipId from the accepted boundary
-> VexExecution Job custody
-> ThemeModeStore.save(mode)
   -> Boolean provider commit fact
-> ThemeModeFamily.Act.Method.Save maps that fact into:
   VexMethod.Result<ThemeMode, Unit>
   -> Success(ThemeMode)
   -> Failure(Unit)
   -> Error(Throwable) only when invocation escapes
-> VexLogger chronology
~~~

The Family-owned Parameter is carried through the exact accepted Effect rather than reconstructed.
The Method carries that Effect object, and Act.Accepted.launch(...) verifies it before the accepted
boundary's live Family owner, Act-authored Approach, and Family-authored RelationshipId enter the
reusable execution foundation. Execution adds custody rather than another feature/domain decision.

PR #209 is accepted/merged provenance for the first executable VexArchitecture stability point.
Later accepted Direct-Family realization refinements and current repository source/tests own present
behavior. Future candidates require their own exact-head proof; the old #209 proof-pending lifecycle
language is not current foundation status.

Compound/Relay remains a separate semantic owner for multi-truth convergence.

<!-- [VXG RealForever][FOUNDATION][FAMILY][DIRECT-ACT-EXECUTION-RELATIONSHIP] -->



### VexPartner — accepted external-participant foundation

Current entry:

~~~text
architecture/behavior/README.md
architecture/behavior/src/main/.../VexPartner.kt
architecture/behavior/src/test/.../VexPartnerHighConfidenceTest.kt
~~~

Accepted Partner grammar:

~~~text
VexPartner<Partner>
├─ Source<Partner>
├─ Signal<Partner>
└─ Connection<Partner, Signal>
   ├─ Id<Partner>
   ├─ Conversion<Input, Partner, Signal>
   ├─ Observation<Partner, Signal>
   └─ Receiver<Partner, Signal, exact Observation>
~~~

PR #219 is accepted/merged provenance. CGM, connected pen and insulin pump pressure established
transport-diverse Conversion, exact Connection occurrence/provenance, queued admission, Store/Family
separation and bounded Android Room restart proof.

~~~text
ACCEPTED PARTNER ROOT
!= REAL LOCAL HTTP LIFECYCLE COMPLETE
!= REAL BLE/GATT LIFECYCLE COMPLETE
!= EVERY PROVIDER IS A PARTNER
~~~

Current VexStream also consumes Partner through the Player relationship:

~~~text
Playback / Queue / Repeat Direct Families
-> exact bound Methods / requests
-> VexStreamPlayerPartner
-> later Player Signals
-> VexStreamPlayerStore
-> Family observation
~~~

Partner owns external participation. Store owns durable/current outside truth where earned. Family
owns product semantics. VexExecution owns only active custody.

### Android Service pressure — sustained realization, generic root held

Current production witnesses:

~~~text
VexStreamPlaybackService
    MediaSession / ExoPlayer lifetime
    Player Partner observation attachment
    Service-lifetime Widget publication custody

VexMapsNavigationService
    foreground Android lifetime
    location / speech / notification realization
    consumes VexNavigationSession truth
~~~

The repeated question is sustained platform realization beyond a shorter caller/UI lifetime. Current
source does not contain or require an accepted generic `VexService` root.

~~~text
SERVICE PLATFORM LIFETIME
!= SEMANTIC CURRENT OWNER

generic VexService<Service, Current>
= NOT ACCEPTED / NOT CURRENT MAIN

universal Service.current
= NOT ACCEPTED

mandatory VexExecution / typed ServiceScope
= NOT ARCHITECTURE LAW
~~~

Do not rebuild the old #231/#233 Service hypothesis merely because Family and Partner have reusable
roots. Re-derive any future Service abstraction from materially different current consumers after
subtracting Family, Partner, Store, Relay, Execution, Dependency and Android platform ownership.

<!-- [VXG RealForever][FOUNDATION][PARTNER][SERVICE-PRESSURE] -->


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
- identify the currently provisional/held foundations rather than assuming examples are canonical;
- pass `vexFoundationHealth` and `vexVerifyLocal` on current source before treating the repository
  foundation/proof route as healthy; use `vexVerifyDevice` when the claimed foundation property
  actually depends on connected Android reality.

Removal means the **repository foundation is established enough to build outward**, not that every
feature, integration, or future architecture question is settled.

## Repository onboarding and foundation health

The repository front door and the proof system are themselves stable infrastructure. A future worker
must be able to find them from source rather than from remembered chats:

```text
README.md
-> AGENTS.md
-> docs/CULTURE.md
-> docs/foundation/INDEX.md

foundation question
-> this index
-> exact owning foundation document
-> canonical source + focused tests

feature question
-> docs/features/README.md
-> feature home
-> FEATURE-WALKS.md only when whole-feature completion/traceability is implicated

proof question
-> test-proof/README.md
-> vexVerifyJvm
-> vexVerifyLocal
-> JOURNEY-INSTRUMENTATION.md / SCREEN-CRAWLER-EVIDENCE.md / vexVerifyDevice as required

host/device substrate missing
-> ANDROID-HOST-PROOF.md
```

The executable structural guard is:

```bash
./gradlew vexFoundationHealth
```

It fails when required entry sources disappear, the root maps stop pointing at their canonical
owners, Feature-Walk proof becomes undiscoverable, the source-alignment check disappears, or the
repository-owned verification tasks are no longer registered.

`vexFoundationHealth` is part of `vexVerifyJvm`, so ordinary `vexVerifyLocal` proof always checks
this front-door/foundation integrity before being treated as a healthy local checkpoint. Its receipt
is written to:

```text
build/vex/test-foundation/foundation-health.txt
```

and is collected into `VexLife-Test-Foundation-Proof.zip`.

Permanent distinction:

```text
FOUNDATION_HEALTH_PASS
!= FEATURE_BEHAVIOR_PASS
!= JOURNEY_DEVICE_PASS
!= PRODUCT_ACCEPTANCE
```

Foundation health prevents silent drift of the repository's stable entry/proof infrastructure. The
owning semantic, Journey, native/provider and lived proofs still own their respective claims.

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
| F1_UI_BLUEPRINT_HOME | element cells + placement + first Home practicum | **MERGED / LIVED DEVICE RUN OBSERVED / AGGREGATE TEST PROOF PENDING** | Make UI identity, placement, intent, navigation and diagnostics human-perceivable before feature depth | aggregate Gradle proof + clean rerun before any formal foundation checkpoint |
| VEX_COMPOSE | VexModifier root + inherited custom-Compose identity + sanctioned cells | **ACCEPTED ON MAIN / PR #88 / A013 LOCAL + PHYSICAL DEVICE PASS / ADVISORY MIGRATION ACTIVE** | Make custom Compose inherit the same semantic lattice as ScreenBlueprint so product UI cannot silently fall back to detached primitive code | migrate one real consumer at a time; preserve exact runtime identities; keep raw-Compose findings advisory until replacements are earned |

| DIRECT_FAMILY_METHOD | Direct VexFamily + VexMethod semantic/realization circuit | **ACCEPTED CURRENT FOUNDATION / PR #209 ORIGIN + LATER MAIN REFINEMENTS** | Keep CURRENT State + Control -> accepted Act, exact bound Method and Family-owned DisplayState under one semantic owner while realization/custody stay separate | current source/tests; future semantic changes require exact-head proof |
| VEX_PARTNER | rooted external-participant Source / Signal / Connection grammar | **ACCEPTED ON MAIN / PR #219 / SOURCEOWNER + INDEPENDENT ASSURANCE + BOUNDED CONNECTED RESTART PROOF** | Preserve external Source/Signal/Connection occurrence/Conversion/Observation/Receiver meaning without making Partner own Store, Family or execution | pressure real Local HTTP/BLE consumers; do not infer every provider is Partner |
| ANDROID_WIDGET_SURFACE | Glance App Widget projection/callback boundary | **ACCEPTED ON MAIN / PR #174 FOUNDATION + PR #185 TYPED SURFACE-STATE CURRENTNESS / #255 RUNTIME CURRENTNESS CORRECTION PENDING** | Let home-screen widgets consume feature-owned projections and return stable interaction identity without becoming product State or Effect owners | preserve accepted foundation; close #255 failed-update-request debt before treating identical SurfaceState as sufficient to suppress every future update request |
| EXECUTION_BRIDGE | VexExecution owner + RelationshipId -> actual Job(s) custody | **ACCEPTED ON MAIN / PR #87 BASE + PR #209 MERGED REFINEMENT** | Keep asynchronous work under explicit runtime owners while making the actual Job the occurrence and removing parallel InstanceId/SourceRef/Handle/history architecture | current source/tests own behavior; future candidates require exact-head owner isolation / overlap / recovery proof |
| READABLE_DEPENDENCY_TOPOLOGY | lazy prerequisite construction + lifetime memoization | **ACCEPTED ON MAIN / PR #209 MERGED ORIGIN** | Make production construction order, reuse and lifetime relationships inspectable without reflection/service-locator lookup or another identity namespace | current owning source/tests; future topology changes require exact-head source review and repository proof |
| LOGGER_TRACEABILITY | one chronology + Blueprint-derived readable interpretation | **ACCEPTED ON MAIN / PR #209 MERGED REFINEMENT** | Let UI/semantic/runtime facts share one chronology and resolve stable UI coordinates through the existing Blueprint registry while exposing missing mappings | current owning source/tests; additional lived Debug Log review is required only for new lived claims |
| BODY_SEMANTIC_BLUEPRINT | first-principle purpose -> primitive -> relationship -> form orientation | **DOCUMENTATION FOUNDATION / NO NEW RUNTIME CONTRACT** | Let a fresh human/AI recognize reusable semantic roles across differently named domains without turning the map into one class hierarchy | pressure the map against live feature/foundation examples; promote a primitive to a type/interface only when addressability or an executable invariant earns it |
| BEHAVIOR_BLUEPRINT_EXPERIMENT | compatibility family-bound relational Behavior foundation | **ACCEPTED COMPATIBILITY / LIVE CURRENT CONSUMERS REMAIN / NOT COMMON NEW-FAMILY ROUTE** | Preserve lawful compatibility Control + current State -> Act and State + Act -> Effect + admitted State-kind relationships for current consumers such as VexMaps Distribution | maintain current consumers; route new/materially evolving semantic Families through Direct VexFamily first |
| DATA_REPOSITORY | domain repositories + Room local persistence + reusable data fixtures | **MERGED / LIVED PARTIAL / INTEGRATION PROOF PENDING** | Let screens observe data without knowing DAO/storage/backend details; first consumer is Log History | aggregate unit/build + Room migration/integration proof + clean lived rerun |
| TEST_PROOF_ARCHITECTURE | local-first proof topology + reusable verification entry points | **ACCEPTED ON MAIN / PR #78 EXACT HOST PROOF PASS** | Keep semantic, render, journey, visual, coverage and device proofs distinct while making the normal proof path reusable | extend only when a new proof claim earns another owner/task |
| JOURNEY_INSTRUMENTATION | semantic walk grammar + map coverage + Compose instrumentation | **MERGED / CONNECTED DEVICE 13/13 OBSERVED** | Make real Android journeys executable once while keeping semantic coverage debt visible and non-blocking by default | extend journeys with new features + preserve baseline/device proof |
| ANDROID_HOST_PROOF | repository-owned Android proof requests carried by current VexRelay/VexLocalBridge | **ACCEPTED ON MAIN / PR #78 EXACT HOST PROOF PASS** | Let any feature request exact Android repository/device proof without rebuilding host-carriage mechanics or making Victor interpret routine output | reuse current repository tasks + live SDK contract; evolve only from repeated real proof needs |
| EVENTS_CONVERGENCE_PRACTICUM | external-information ingress + Relay composition + stable dynamic list + anchored attention | **DRAFT CANDIDATE / PURE KOTLIN SMOKE PASS / ANDROID PROOF PENDING** | Master one convergence node before reverse-building real Messaging/Calls/Maps/Health/VexStream adapters toward it | exact Gradle/Journey/ScreenCrawler + physical-device attention walk |
| VEXMAPS_FEATURE_TOPOLOGY | native map composition + provider / Relay / session boundaries | **ACCEPTED ON MAIN / PR #73 MERGED / A014 PHYSICAL + A017 LOCAL PASS** | Make Maps teach where semantic identity, durable context, convergence, provider mechanics and presentation belong without turning MapLibre into app architecture | extend only when real product evidence earns age/accuracy, gesture or second-renderer evolution |
| F2_STATE_PROJECTION | StateFlow/state projection kernel | PORT_CANDIDATE | Preserve one-owner observable semantic state and emission discipline | isolated JVM/native contract tests |
| F3_INTENTION_OPERATION_RESULT | intention -> operation -> attempt -> result grammar | PORT_CANDIDATE | Keep requested meaning, execution identity, retry identity, and result admission separate | deterministic operation/admission tests |
| F4_COMPOSITION_BOUNDARY | composition root / ViewModel / lifecycle boundary | PORT_CANDIDATE | Keep Android lifecycle wiring separate from product semantic ownership | lifecycle + ViewModel contract tests |
| F5_LOCALIZATION_PRESENTATION | portable localization mapping beyond Android @StringRes + native presentation | PORT_CANDIDATE | Keep visible language and cross-platform string identity source-addressable | resource/localization + Compose tests |
| F6_DESIGN_LAB | debug/developer component gallery | FIRST PRACTICUM INCLUDED IN F1 | Exercise building blocks and inspect their registered topology without creating another truth map | prove F1 Gallery + AppGraph on device |
| F7_HOME_TRANSPORT | real Home/network/session integration | HELD | Requires explicit real transport, identity, permission, and failure contracts | not part of bootstrap |
| F8_MODEL_COMPANION | model/AI companion integration | HELD | Requires separate model/runtime ownership and real integration contracts | not part of bootstrap |

## Vex Body Semantic Blueprint

Entry:
[`VEX-BODY-SEMANTIC-BLUEPRINT.md`](VEX-BODY-SEMANTIC-BLUEPRINT.md)

### Intention

A fresh human or AI should be able to move in both directions without predecessor-chat reconstruction:

~~~text
purpose
-> domain
-> semantic primitive
-> relationship
-> established architecture
-> concrete form
-> manifestation
-> proof

implementation
-> form
-> relationship
-> primitive
-> purpose
~~~

The Body map is an orientation foundation, not another runtime framework. It relates current live
examples across Behavior/SCAE, Convergence Relay, Conversation, Events, VexMaps, Elements and
VexExecution while preserving their separate owners.

Permanent:

~~~text
REUSABLE SEMANTIC PRIMITIVE
!= REQUIRED SHARED INTERFACE

ALIGNMENT
!= SAMENESS

DOMAIN MEANING
!= IMPLEMENTATION SUBSTRATE
~~~

Use the map to recover why a source owner exists and what first-principle role it carries. Promote a
primitive into a durable type/interface only when a real addressability, compiler-safety or repeated
executable-invariant pressure earns that stronger form.

## Vex Compose foundation

Entry:
[`architecture/elements/README.md`](../../architecture/elements/README.md)

### Intention

Custom Compose should inherit architectural identity rather than require every author to remember
test tags, parent paths and Vex-specific presentation conventions manually.

~~~text
ScreenBlueprint parent path
        |
        +-> registered Vex Element cells
        |
        +-> VexComposeScope
              -> VexColumn / VexRow / VexCard / VexText
              -> child IDs append from the inherited parent
~~~

`VexModifier` is a real Compose Modifier root, not a parallel modifier type. Ordinary Compose
extensions remain available underneath it while Vex-owned semantic identity/inspection behavior has
one visible front door.

Gallery is the first visual practicum. VexStream Search is the first larger product consumer: it
enters through the registered Search sheet, preserves historical stable field/action IDs, and uses
runtime track presentation refs without turning dynamic catalog data into fake static elements.

Acceptance checkpoint:

~~~text
PR #88
merge = 222c10206685946f7225f801a9e62e0beabcf74e
tree = a68fe2eebaa2bdfbc0503648aee77ca6ddeb7577

A013
vexVerifyLocal = PASS
physical connected Android = PASS
instrumentation tests executed = 10
~~~

Further migration is consumer-driven. Raw Compose findings remain advisory while real product
surfaces teach which additional Vex forms are actually worth preserving.

## Execution bridge — accepted foundation

Entry:
[`architecture/execution/README.md`](../../architecture/execution/README.md)

### Intention

A developer should not decide coroutine ownership ad hoc from whichever Screen, repository, Service,
or callback needs asynchronous work. That is an ownership rule, not universal VexExecution admission:
first identify the rightful semantic and runtime lifetime owners, then use VexExecution only when the
work actually needs its custody semantics such as maintained RelationshipId overlap, exact-source
duplicate/replacement/cancellation, bounded source/subscription recovery, or owner-isolated stop/join
when shared custody is actually required. Whether that earned custody becomes direct entry or a
concrete VexExecution owner is a later source-shape question, not an independent admission reason. A
bounded owner-local coroutine lifetime is not debt merely because it uses a different mechanism.

PR #209 refines the accepted execution base to the smaller current law:

~~~text
VexExecutionRoot
    shared coroutine tree
    terminal shutdown

one VexExecution owner
    ordinary owned Jobs
    maintained active relationships:
        RelationshipId -> actual Job(s)

Job
    runtime occurrence

VexLogger
    history / chronology
~~~

Current accepted source removes the earlier public InstanceId / SourceRef / Handle / root execution-registry
architecture while preserving the real behaviors discovered by those earlier implementations:
launchOnce duplicate protection, independent finite work, maintained Flow observation, Earliest /
Latest / Queued overlap, source-vs-downstream failure distinction, bounded source recovery, structured
child lifetime, provider handoff, exact relationship cancellation and terminal root shutdown.

Owner-scoped stop/join is now distinct from root shutdown, so sibling VexExecution owners sharing one
root do not terminate one another.

ThemeMode, VexMaps Distribution/search, and VexStream roots provide current production pressure.
PR #209 is accepted/merged provenance for this refinement; current repository source and tests own
present execution behavior, and future candidates require their own exact-head proof.

## Readable dependency topology — accepted foundation

Entry:
[`architecture/dependency/README.md`](../../architecture/dependency/README.md)

### Intention

Production construction should answer, from source:

~~~text
what provider exists?
what prerequisites does it require?
what lifetime owns/reuses it?
what requested root caused it to be built?
what consumer receives the concrete object?
~~~

PR #209 adds one deliberately small construction kernel:

~~~text
provider declarations
-> explicit prerequisite graph
-> lazy dependency-first resolution
-> one memoized instance per factory lifetime

VexApplication factory
-> application/process lifetime

VexLifeViewModel child factory
-> session/configuration lifetime
-> reuses parent providers
~~~

Provider declaration object identity is the construction coordinate; class/name is only a readable
label. The factory is accessible at composition roots/provider construction only. Ordinary feature/UI
consumers receive concrete dependencies and do not call an ambient get<T>() service locator.

The production session graph now exposes Logger, ThemeMode Family/runtime realization, Relay
composition, action wiring, Runtime and Session instead of hiding their construction inside one
opaque Session provider. Preview/test direct construction remains an explicit alternate fixture
route.

Current refinement keeps construction reusable without creating a peer feature owner to bridge
existing relationships. ThemeModeFamily authors Select -> beginLatest(Save), ThemeModeStore remains
the outside-truth boundary, and the session composition supplies VexExecutionRoot/VexLogger to the
existing realization callsite. Store observation uses direct VexExecution observation under the same
live Family owner. Dependency construction owns lifetime/wiring only; it does not acquire feature
runtime meaning.

VexLifeRelayGraph is now a projection-only session graph. Events and Conversation Relays are
independently provided/activated nodes with their own readable prerequisite closures; the graph only
projects which Relay owners participate in the VexLife lifetime.

## Logger traceability refinement

Entry:
[`architecture/logging/README.md`](../../architecture/logging/README.md)

### Intention

VexLogger owns chronology, not semantic/UI/execution truth.

PR #209 connects UI interactions, structured semantic/runtime facts and architecture diagnostics into
one chronology, while UIBlueprintRegistry remains the owner used to resolve stable element IDs into
Screen / path / kind / declared-intent fields.

Intentional dynamic runtime element coordinates remain distinct from static Blueprint omissions.
Missing expected static interpretation emits an observable BlueprintMappingMissing diagnostic rather
than silently becoming unknown.

## Behavior Blueprint — accepted compatibility surface

Compatibility SCAE entry:
[`architecture/behavior/README.md`](../../architecture/behavior/README.md)

Common new/materially-evolving Family authoring route:
[`architecture/behavior/FAMILY-CIRCUIT.md`](../../architecture/behavior/FAMILY-CIRCUIT.md)

Historical/package construction reference:
[`architecture/behavior/VEX-BEHAVIOR-FAMILY-BLUEPRINT.md`](../../architecture/behavior/VEX-BEHAVIOR-FAMILY-BLUEPRINT.md)

Historical review checkpoint:
[`architecture/behavior/REVIEW-CHECKPOINT.md`](../../architecture/behavior/REVIEW-CHECKPOINT.md)

Current distinction:

~~~text
Direct VexFamily / VexMethod
    accepted common route for new/materially evolving semantic Families

VexBehaviorFamily / VexBehaviorBlueprint
    accepted compatibility source
    still has production consumers
    != peer default route for a new Family
~~~

Current compatibility pressure includes VexMaps Distribution Mode / Delivery Route. VexStream
Playback / Queue / Repeat have moved to Direct `VexFamily` and must not be used as evidence that
compatibility Behavior is still their current semantic owner.

Compatibility SCAE still preserves useful laws for its remaining consumers:

~~~text
Control + CURRENT State
-> Act

CURRENT State + Act
-> Effect + admitted State-kind constraint

CURRENT State + Controls
-> Projection
~~~

The concrete Connection/runtime may construct an exact admitted State value where compatibility
source owns only the State-kind constraint. Do not normalize that historical/current compatibility
shape into Direct Family's exact-value admission, and do not migrate a consumer merely to make
documentation or VexBoard visually uniform.

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

PR #78 is accepted on `main` and received its own exact host proof before merge. Feature branches
such as VexMaps now consume the repository-owned proof surface rather than copying another lane's
Relay package formation or proof command list. For a fresh agent composing several dependent checks
into one human round trip, use the **Reusable multi-path host-task template** in
`ANDROID-HOST-PROOF.md`; it preserves per-path evidence while keeping one canonical RETURN.

## Test / Proof Architecture candidate

Canonical detail: [`test-proof/README.md`](test-proof/README.md)

### Intention

A developer should be able to prove feature meaning, Android rendering, user journeys, visual
stability and executable-code coverage at the narrowest truthful owner, then invoke the ordinary
source/JVM/build bundle through one repository-owned local command. CI remains a caller rather than
another proof definition.

The reusable proof topology is accepted on `main` through PR #78. It keeps focused semantic proof,
repository-local aggregate proof, optional visual evidence, and connected-device proof as distinct
claims. Feature-specific screenshot/golden semantics remain with their owning features rather than
becoming another universal test framework.


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

## VexMaps feature topology

Canonical detail: [`VEXMAPS-FEATURE-TOPOLOGY.md`](VEXMAPS-FEATURE-TOPOLOGY.md)

### Intention

A fresh developer should be able to descend from VexMaps as a human destination into semantic
registration, durable Maps state, cross-truth Relay relationships, navigation-session lifetime,
provider adapters and visible composition without first reconstructing a giant Compose file.

The accepted PR #73 topology keeps MapLibre provider mechanics feature-local, keeps Relay for
semantic reconciliation rather than turning it into an event bus, and makes custom-Screen
Preview/Journey use renderer-independent Screen identity. Its executable behavior is closed by
A014's full physical Samsung ScreenCrawler proof (9/9, including Events and VexStream), while A017
proves the documentation-currentized repository-owned `vexVerifyLocal` surface with 260/260 JVM
tests, debug APK formation and clean source state. A007 remains valid evidence for its earlier
pre-feedback head; unresolved product questions remain explicitly separate.

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
