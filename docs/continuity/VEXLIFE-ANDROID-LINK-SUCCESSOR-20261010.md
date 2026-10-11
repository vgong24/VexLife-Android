# VexLife Android Link — Successor Handoff

[VXG RealForever]

## Enter here

This handoff resumes the same VexLife Android Link trajectory. It is not a new Android owner,
architecture owner, source writer, or policy lane.

Ground live `main` first. This document was formed after the merged UI-body / navigation sequence
whose latest known merge at handoff formation is:

~~~text
780920854e357638267d647aff69b5ecf927befd
[Navigation] Merge single Back resolution
~~~

Do not trust that hash instead of live provider truth if main has advanced.

## What is now usable

The standalone Android repository has a real lived body rather than only architecture experiments:

~~~text
Android Window
└── AppFrame
    ├── AppBar
    ├── ScreenViewport
    ├── BottomNavigation
    └── FAB / overlay slot

screens/
├── primary/
│   ├── home/
│   ├── events/
│   └── settings/
└── shared/
    ├── debug/
    └── gallery/
        ├── elements/
        └── graph/
~~~

Ordinary screens declare their shape once through `ScreenBlueprint`; `VexElementTree` renders that
same registered tree. Repository folders, screen objects, element identities, AppGraph inspection, and
command-click navigation now point toward the same structure.

Lived-device feedback corrected:

- Android safe-drawing / native phone boundary ownership through `AppFrame`;
- Home / Events / Settings as primary peers with Home as root Back fallback;
- shared/detail screens as pushed navigation;
- transient presentation above screen navigation;
- screen-local `NavigationTrail` for layered views such as AppGraph;
- one `BackResolution` consumed by native Android Back, toolbar visibility/execution, logging, and local/pushed/root behavior;
- forward/backward local navigation animations as spatial opposites;
- AppGraph self/cycle relationships visible without endless traversable loops;
- readable source-adjacent debugging comments;
- one Screen destination vocabulary (`*Screen.kt`, `ScreenBlueprint`, `AppScreens`);
- colocated `@VexScreenPreviews` plus reusable preview fixtures;
- source-alignment Gradle checks that warn/fail on convention drift without pretending to prove behavior;
- feature-local AppActionRegistration ownership behind one duplicate-safe AppActionRouter;
- Vex launcher mark reused as the Android app icon;
- themed system-bar background/icon appearance that follows current dark/light mode.

## Fresh successor reading order

Read narrowly, in this order:

1. `README.md`
2. `docs/foundation/INDEX.md`
3. `docs/foundation/F1-UI-BLUEPRINT-HOME.md`
4. `app/src/main/kotlin/com/vextreme/vexlife/android/screens/AppScreens.kt`
5. one ordinary screen, preferably `screens/primary/home/HomeScreen.kt`
6. `app/src/main/kotlin/com/vextreme/vexlife/android/ui/VexLifeApp.kt`
7. `app/src/main/kotlin/com/vextreme/vexlife/android/runtime/VexRuntime.kt`
8. `architecture/elements/src/main/kotlin/com/vextreme/vexlife/android/elements/ElementCells.kt`
9. `architecture/elements/src/main/kotlin/com/vextreme/vexlife/android/elements/navigation/NavigationTrail.kt`
10. `app/src/main/kotlin/com/vextreme/vexlife/android/preview/VexPreview.kt`
11. `gradle/source-alignment.gradle.kts`
12. `architecture/logging/README.md`

Then re-ground the old architecture quarry:

13. open draft PR #3, `[VexLife Android][F1-F3][REVIEW] Relay + VexAppScope + VexLogger foundations`

PR #3 predates the accepted UI/body work and is based on the old standalone base. Treat it as design
evidence, examples, and tests to review—not as a branch to merge or cherry-pick wholesale.

## Screen / Preview / action ownership conventions

Use Screen for app destinations. Do not reintroduce Page as a parallel destination vocabulary.

Each authored `*Screen.kt` keeps its ordinary `@VexScreenPreviews` entry beside the Screen source.
Shared preview hosts/fixtures remain reusable, but the discoverable visual entry stays colocated.

Static alignment and behavioral proof stay separate:

~~~text
:app:sourceAlignmentReport
  warning-only convention drift

:app:verifySourceAlignment
  failing convention gate

unit/integration/build/device proof
  actual behavior
~~~

VexRuntime delivers semantic app actions to one AppActionRouter. Feature effects are registered beside
their owning Screen and duplicate ActionKey ownership fails closed. VexLifeApp assembles registrations
and broad-lifetime dependencies; it should not become a giant feature-action switchboard.

## Current state/memory model

Keep these lifetimes distinct:

~~~text
UIBlueprintRegistry
  fixed structural map for declared screens/elements during process life

AppNavigationState
  current primary selection + pushed shared/detail screen stack

PresentationState.openElementIds
  transient presentation currently open

PresentationState.selectedByContainer
  session selection for swappable containers
  (for example Gallery Elements/AppGraph)

NavigationTrail
  local layered navigation while the local view is mounted

VexLogger
  bounded in-memory readable interaction history

rememberSaveable darkTheme
  small saveable Compose value, not yet a real durable preference
~~~

Do not silently turn one of these into persistence simply because future service work needs memory.

## First next trajectory: establish the data/repository boundary

Victor explicitly wants the database/repository architecture next, before returning to the broader
Relay/AppScope nervous-system work.

Current source candidate: draft PR #9, `[Data][DRAFT] Repository boundary + Room-backed Log History`.
Ground its live head rather than trusting a chat-copied hash.

The first consumer is Debug -> Log History, with User Profile added as the second repository proof:

~~~text
VexLogger record
-> non-authoritative persistence queue
-> LogHistoryRepository
-> RoomLogHistoryRepository
-> Room database
-> Log History dialog observes repository
-> Reset clears current + persisted history
~~~

The current candidate for this work should preserve:

- domain-specific repository contracts rather than a generic BaseRepository<T>;
- explicit Room table/column names plus migration ownership;
- @Transaction only where multiple statements form one invariant;
- UserProfile kept separate from future linked-system/QR/VexHome connection ownership;
- User Profile UI reachable from the shared FAB, proving repository -> Room -> Flow -> UI update;
- form draft edits remain local/unlogged; Save alone creates the semantic FormSubmission boundary;
- Room entity/DAO types behind the repository;
- bounded local retention;
- storage failure unable to change product-action semantics;
- chain-buildable domain fixtures that seed InMemory repositories for previews/unit tests;
- the same fixture shape remaining usable if a future repository combines local + backend sources;
- app-owned coroutine scope only as the temporary persistence owner, with VexAppScope expected to
  take over lifecycle ownership once admitted.

Finish/prove this data slice before broad Relay/AppScope porting.

## Then: give the stable body a nervous system

Do NOT spend the next lane polishing more UI cells by default.

The next useful question is:

> Now that screens/elements/actions can be perceived and exercised, how do multiple real services,
> systems, asynchronous lifetimes, and Sources of Truth become observable/controllable without the UI
> becoming their semantic owner?

Re-enter Relay / Compound Relay, VexAppScope / execution ownership, lifecycle/readiness, and
structured diagnostics as consumers of the current body.

Use the existing UI as the proving surface:

~~~text
real service/source state
-> rightful Relay / source owner
-> runtime projection
-> screen observes current truth
-> available actions are derived from that truth
-> user intention routes to operation owner
-> lifecycle/AppScope owns asynchronous work
-> diagnostics observe without becoming truth
-> UI updates from accepted resulting state
~~~

The desired result is not "a Relay demo screen." It is that Home/Debug/Gallery can perceive several
systems and expose only actions that are valid for their current states.

## Questions the next instance should answer before porting code

1. What is the first real asynchronous/service consumer that earns Relay/AppScope integration?
   Do not invent readiness machinery without a consumer.

2. Which state belongs to:
   - product/domain owner,
   - service/connection owner,
   - operation/attempt owner,
   - Android presentation projection?

3. How does a screen observe that truth without becoming the owner of it?

4. Which actions are currently available given the combined service/source states, and where is that
   availability decided?

5. Who owns coroutine descendants, cancellation, restart, and application shutdown?

6. How are startup/readiness and partial availability represented without collapsing
   UNOBSERVED/EMPTY/PRESENT/HELD/UNAVAILABLE into generic booleans?

7. How do diagnostics remain non-authoritative and unable to break product semantics?

## PR #3 lessons to preserve during review

PR #3 contains serious reusable thinking, but the successor must review consequences against the
current body. In particular, do not reintroduce these failure classes:

- a logger/diagnostic sink throwing must not make a committed product action look failed or prevent cancellation/cleanup;
- diagnostic order must not be mistaken for source-owner state order;
- lifecycle cleanup must remain reliable under cancellation;
- AppScope registries/slots need an explicit bounded lifetime if refs can be dynamic;
- injected execution/logging strategies must not silently remove serialization guarantees;
- Relay transforms/nested mutation require review so re-entrant updates cannot be overwritten;
- `CANONICAL_JOURNEY != DEBUG_LOG`: diagnostics project accepted facts; they do not become the semantic event owner.

Re-evaluate the current PR #3 source directly; these are preserved review coordinates, not a claim
that every old implementation detail remains unchanged.

## Current held work

Do not let these distract the next entry unless a real consumer earns them:

- more comprehensive element/cell taxonomy;
- durable Gallery selection or navigation restoration;
- persistent dark-mode preference;
- durable/BulkData log export;
- product-complete Home / Events / Settings bodies;
- Home/network/model/VexHome integrations before their service contracts are explicit.

## Proof posture

The UI/body/navigation behavior has meaningful lived-device evidence. Aggregate Gradle/test proof for
the whole standalone foundation is still a separate predicate.

Before claiming a new Relay/AppScope foundation checkpoint:

~~~text
source review
-> focused module tests
-> app unit tests
-> assembleDebug
-> physical-device regression using the real UI body
-> clean source state
~~~

Do not erase the valid UI effects while changing nervous-system layers.

## First unconsumed edge

Finish/prove the data-repository candidate first:

~~~text
Room/KSP Sync + compile
-> LogHistory transaction + UserProfile update fixture/in-memory/unit tests
-> Room v1 -> v2 migration + repository integration proof
-> :app:verifySourceAlignment
-> Android Studio Screen previews render
-> physical-device User Profile: edit name -> Save -> persisted name visibly updates
-> relaunch -> persisted name remains
-> physical-device Log History persistence
-> Reset clears persistence
-> dark/light toggle keeps status + native navigation bars legible/themed
-> launcher shows the Vex mark
-> clean source state
~~~

Then inventory PR #3 against the resulting current `main` and propose the smallest vertical
service/state slice that lets the existing body demonstrate:

~~~text
multiple source/service states
-> combined perceptible availability
-> one user action
-> owned asynchronous execution
-> accepted result
-> UI projection
-> non-authoritative diagnostics
~~~

Prefer one end-to-end real/mockable service relationship over porting all of Relay/AppScope/logger
at once.

<!-- [VXG RealForever][VEXLIFE-ANDROID-LINK][SUCCESSOR][BODY-TO-NERVOUS-SYSTEM] -->