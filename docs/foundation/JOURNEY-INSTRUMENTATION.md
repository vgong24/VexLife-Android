# Journey / Instrumentation Foundation

[VXG RealForever]

## The question

> Can a developer describe a human walk through VexLife once, use that same walk as the expected
> semantic history, execute it through real Android UI, and know when production gained an
> interactive Screen/element that no journey covers?

This foundation is intentionally about **semantic app-map coverage + Android realization**.

It is not a replacement for line/branch code coverage, visual regression, accessibility proof,
Room migration proof, or physical-device product acceptance.

## Package / shard shape

The current test-only foundation is organized as:

~~~text
testing/journey/
  VexJourney.kt
  VexExpectedWalk.kt
  VexJourneyHistoryVerifier.kt
  VexJourneyCoverage.kt

  environment/
    VexJourneyEnvironment.kt

  catalog/
    Journeys.kt                 human/IDE typed path map
    VexJourneyCatalog.kt        executable catalog derived from registered domains
    VexJourneyShard.kt

    shell/
      AppShellJourneys.kt

    shared/
      debug/DebugJourneys.kt
      gallery/GalleryJourneys.kt
      profile/UserProfileJourneys.kt
      vexstream/VexStreamJourneys.kt
      vex/TalkToVexJourneys.kt
~~~

The shard answers "where should a developer look when this walk fails or coverage is missing?" It
does not prohibit cross-Screen journeys. A Profile journey may naturally begin at Home/FAB and still
belong to the Profile shard because the user intention being proved is Profile.

Journey owns the semantic walk. ScreenCrawler is a downstream, opt-in evidence consumer of that
same catalog; it does not own another traversal language. See
[`SCREEN-CRAWLER-EVIDENCE.md`](SCREEN-CRAWLER-EVIDENCE.md) for its selection/capture layer.

## Human contribution path

A developer adding a feature does **not** need to study every prior journey first.

Start from the product meaning being changed:

~~~text
owning *Screen.kt / feature
-> same-domain *Journeys.kt
-> typed Journeys.<domain>.<path> address
-> JVM map/grammar proof
-> baseline Android journey proof when UI behavior changed
-> optional ScreenCrawler evidence when visual/configuration review matters
~~~

For a new journey:

1. Add the walk beside its owning feature shard, for example
   `shared/profile/UserProfileJourneys.kt`, and include it in that shard's `journeys` list.
2. Give the walk one human-discoverable typed address in `Journeys.kt`, such as
   `Journeys.userProfile.formSubmit`. IDE autocomplete under `Journeys.` is the normal map of
   known paths; raw shard/journey ID strings are engine/report identities, not the human authoring
   surface.
3. Run the JVM tests. `VexJourneyHumanMapTest` protects the invariant that every executable catalog
   journey has exactly one typed human path, while `VexJourneyCoverageTest` reports registered
   production Screens/actions/editable fields/modals that no journey names.
4. Run the relevant Android journey proof when the change affects real UI/runtime behavior. Use an
   exact case/shard/environment selector when a whole connected suite is unnecessary.
5. Use ScreenCrawler only when screenshot, locale, font, or other visual evidence is actually
   needed. Adding an ordinary Screen or journey must not require edits to the crawler planner or
   evidence observer.

Change `VexExpectedWalk` or `VexTestEngine` only when the shared journey grammar/runtime
realization itself needs a new semantic capability. Do not add feature-specific branches there just
to make one new feature testable.

Stable `shardId/journeyId` and case refs make failures and evidence addressable later; Git/PR
history remains the source for *why* a path changed. ScreenCrawler is an executor/evidence consumer,
not the history owner.

## One journey, two consumers

Journey declarations live under:

~~~text
app/src/journeyTest/kotlin/.../testing/journey/
~~~

That source directory is compiled into both JVM unit tests and Android instrumentation tests.

A journey reads like the product walk:

~~~kotlin
vexJourney(
    id = "form-submit",
    title = "User Profile form submission",
) {
    start(HomeScreen)

    tap(AppShell.Elements.Add)
    tap(AppShell.Elements.UserProfile)

    type(
        field = UserProfileScreen.DisplayName,
        value = "Journey Victor",
    )
    submit(UserProfileScreen.Save)

    tap(AppShell.Elements.Back)
}
~~~

There is deliberately no second hand-written `expectLog { ... }` tree.

The walk itself forms the expectation.

## JVM semantic coverage mode

`VexJourneyCoverageRecorder` executes the declarations without Android UI.

It records which production semantics each journey names and compares them with
`AppScreens.registry`.

Current observed coverage surface:

~~~text
every registered Screen
every registered ActionElement
every editable TextFieldElement
every registered DialogElement / ModalBottomSheetElement
~~~

The report also includes per-shard journey counts and Android environment-case counts so debt can be
localized without treating a percentage as the source of truth.

A new production action with no journey therefore appears as explicit **journey tech debt** with
its stable semantic ID.

Ordinary JVM/build development does NOT fail merely because semantic coverage is incomplete. The
coverage test:
- fails for malformed journey grammar, duplicate shard IDs, duplicate local journey IDs, or duplicate case refs;
- emits a visible warning when production semantics are missing/stale;
- writes a per-host-test report under `build/reports/vex/journey-coverage-<task>.txt`;
- fails on coverage debt only when strictness is explicitly requested with
  `-PvexStrictJourneyCoverage=true`.

This keeps debt visible without mapping it through a silencer or making APK construction depend on
journey completeness.

Example warning shape:

~~~text
ActionElement journey coverage mismatch.
Missing production semantics:
  - element.settings.some-new-action
~~~

This is the fast answer to:

> did we add an interactive part of the app and forget to place it in any known walk?

Static Text/Card/Column/Image nodes are not required to have their own journey step merely because
they exist. Rendering/style/accessibility need their own proof surfaces.

Local clickable implementation details that are not registered semantic elements are likewise not
silently counted as registered ActionElement coverage. If such an interaction becomes important to
the semantic app map, give it a registered identity first.

## Environment / conditional variants

Semantic journey meaning is separated from Android configuration pressure.

**Normal functional instrumentation is baseline-only.** Every semantic journey must expose a baseline
environment, and ordinary `:app:connectedDebugAndroidTest` selects exactly one baseline case per
journey.

Declared non-baseline environments are available without becoming an automatic cost on every
functional run:

~~~text
no argument
  -> baseline only

vexJourneyMatrix=all
  -> every environment declared by every journey

vexJourneyEnvironment=<environment-id>
  -> only that environment batch

vexJourneyCase=<caseRef>
  -> one exact case

vexJourneyShard=<shard-id>
  -> selected shard, baseline by default
~~~

This preserves one semantic catalog while keeping localization/accessibility matrices as explicit
batch work rather than repeated functional proof.

~~~kotlin
vexJourney(
    id = "form-submit",
    title = "User Profile form submission",
    environments = listOf(
        VexJourneyEnvironments.Baseline,
        VexJourneyEnvironments.LargeText,
        VexJourneyEnvironments.AlternateLocaleFallback,
    ),
) {
    ...
}
~~~

One journey may therefore become several Android cases without duplicating its expected history.

Current profiles:

~~~text
baseline
  device/test-host defaults

large-text-1.45x
  Compose DeviceConfigurationOverride.FontScale(1.45f)

alternate-locale-fallback
  Compose DeviceConfigurationOverride.Locales("es-ES")
~~~

The alternate locale case is **not a localization-complete claim**. There are not yet translated
product resources. It currently proves:
- stable semantic selectors are independent of English visible text;
- Android locale/resource configuration can be injected into the same journey harness;
- future localization can add supported-language profiles without rewriting the walk grammar.

When localization is admitted, add real supported locale profiles and language-specific presentation
proof at that foundation. RTL, window-size, orientation, font-weight, and other Android configuration
profiles can use the same environment seam when a real consumer earns them.

App dark/light mode is currently VexLife session/product state, not merely system dark-mode
configuration, so journeys should continue using the real Debug DarkMode action to prove that
behavior rather than pretending a device override changed product state.

## Android instrumentation mode

`VexTestEngine` executes the exact same journey declarations using Compose instrumentation.

~~~text
tap(ActionElement)
  -> production elementId test tag
  -> Compose performClick()

type(TextFieldElement)
  -> production elementId test tag
  -> Compose text input
  -> NO semantic interaction record expected

submit(ActionElement)
  -> real click
  -> one Submitted semantic record with stable field refs

dismiss(UIElement)
  -> real gesture against the registered presentation tag
  -> Dismissed semantic record

back()
  -> Android Back dispatch
  -> Back semantic record
~~~

The app is hosted with fresh in-memory repositories for every journey.

The actual history therefore traverses the real app path:

~~~text
Compose input
-> VexRuntime
-> VexLogger
-> LogHistoryPersistenceQueue
-> InMemoryLogHistoryRepository
~~~

## Failure isolation and negative controls

One source-level `@Test` method does NOT mean one giant Android run.

JUnit's parameterized runner creates one test instance per `VexJourneyCase`:

~~~text
shard/journey@environment

shared.gallery/elements-modals-swap@baseline
shared.gallery/elements-modals-swap@large-text-1.45x
shared.user-profile/form-submit@baseline
...
~~~

Each case gets a fresh:
- Compose rule;
- VexTestEngine;
- expected walk;
- in-memory repository graph.

A failing Gallery/large-text case therefore does not share state with Profile/baseline.

Cases may be re-run in isolation with instrumentation arguments:

~~~bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.vexJourneyCase=shared.gallery/elements-modals-swap@large-text-1.45x
~~~

Or by broader filters:

~~~text
vexJourneyShard
vexJourneyEnvironment
~~~

Unselected parameter cases are reported as skipped rather than silently removed.

The verifier itself has JVM negative controls. `VexJourneyHistoryVerifierTest` intentionally feeds:
- a missing semantic record;
- a reordered semantic record;
- an unexpected extra record;
- records whose timestamps/durations/logger sequence differ but whose semantic meaning is equal.

The first three must be detected at the first divergent list position. The timing-only difference
must compare equal.

Do not keep deliberately broken production journeys merely to prove the harness can fail. Keep
negative controls focused on the reusable verifier/oracle, then let real journeys fail only when the
app or declared walk actually disagrees.

## Expected history comes from the walk

`VexExpectedWalk` is a small independent test-side interpreter.

It does NOT call VexRuntime or VexLogger.

For each journey step it:
- tracks expected Screen/navigation/presentation position;
- interprets the declared ElementIntent grammar;
- appends the expected canonical semantic record;
- tracks typed values for explicit Submit snapshots;
- understands the current Debug Reset contract, where ClearLogHistory intentionally starts a fresh
  observable log history.

Comparison uses the stable semantic projection:

~~~text
kind
subjectRef
action
screenBeforeId
screenAfterId
detailsJson
~~~

It intentionally ignores:

~~~text
sequence
timestamp
durationSincePreviousSeconds
~~~

List order is the journey ordering contract. `VexJourneyHistoryVerifier` compares the semantic
records by index and reports the first divergent position with a small sequence neighborhood.
Timestamp/duration/logger-sequence formatting remains outside this equality and is protected by
focused VexLogger tests.

## Why expected and actual stay separate

Do not generate the expected side by calling the production logger/runtime.

~~~text
production implementation bug
-> must be able to disagree with expected walk
~~~

The test-side interpreter may read the public element grammar because the journey is verifying:

> Android/runtime/logging faithfully realized this declared semantic walk.

A journey may later add sparse independent product checkpoints when it needs to prove something the
declaration itself cannot independently establish.

## Stable identity is the test selector

Production elements already use:

~~~kotlin
Modifier.testTag(element.elementId)
~~~

Journeys therefore address the same identity used by:
- ScreenBlueprint / registry;
- runtime ownership;
- logging;
- instrumentation.

Do not use localized visible strings as the primary journey selector.

Modal Android realizations likewise carry their registered Dialog/ModalBottomSheet element IDs.

## Action addressability

Journey coverage is now a legitimate relationship that may need to address an action.

Therefore Gallery's former inline sample actions are named refs while preserving their already
established element IDs.

Do not rename semantic IDs merely to make tests convenient.

## Current first catalog

The initial catalog covers:
- Home / Events / Settings peer navigation and native Back;
- Debug -> Log History, drag dismissal, Reset and toolbar Back;
- Gallery reusable actions, both modal styles, and Elements/AppGraph local selection;
- User Profile typing + Save submission;
- VexStream Music placeholder;
- Talk to Vex placeholder.

The catalog remains the one semantic source. Adding a journey automatically adds its **baseline**
case to ordinary Android instrumentation. Declared non-baseline environments are expanded only when
an environment batch is requested (for example `vexJourneyMatrix=all`).

Stable local journey IDs combine with shard/environment IDs to give every case a durable `caseRef`
for reports and isolated reruns.

## Proof layers

~~~text
JVM:
  :app:testDebugUnitTest

  VexJourneyCoverageTest
    production semantic map coverage

  VexExpectedWalkTest
    independent expected-side grammar/oracle

  VexJourneyHistoryVerifierTest
    negative controls for missing/reordered/extra records
    proves timing metadata is excluded from semantic equality

Android instrumentation:
  :app:connectedDebugAndroidTest

  every VexJourneyCatalog entry
    -> real Compose input
    -> actual persisted semantic history
    -> generated expected semantic history
    -> exact semantic comparison

build:
  :app:assembleDebug

lived:
  physical-device journey behavior when gesture/device-specific behavior matters
~~~

## Distinctions

~~~text
JOURNEY_MAP_COVERAGE != CODE_LINE_COVERAGE
JOURNEY_DECLARATION != ANDROID_INSTRUMENTATION_PROOF
EXPECTED_WALK != PRODUCTION_LOGGER
TEST_TAG != HUMAN_LABEL
ACTION_EXISTS != SAFE_AUTOMATIC_CRAWL
REGISTERED_SEMANTIC_ACTION != EVERY CLICKABLE IMPLEMENTATION DETAIL
~~~

A future automatic crawler may use registry/journey information, but the current foundation does not
blindly execute every action. The catalog is explicit human-meaningful traversal; the coverage gate
makes omissions visible.

<!-- [VXG RealForever][VEXLIFE-ANDROID][JOURNEY-INSTRUMENTATION] -->
