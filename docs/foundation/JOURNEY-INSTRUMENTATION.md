# Journey / Instrumentation Foundation

[VXG RealForever]

## The question

> Can a developer describe a human walk through VexLife once, use that same walk as the expected
> semantic history, execute it through real Android UI, and know when production gained an
> interactive Screen/element that no journey covers?

This foundation is intentionally about **semantic app-map coverage + Android realization**.

It is not a replacement for line/branch code coverage, visual regression, accessibility proof,
Room migration proof, or physical-device product acceptance.

## One journey, two consumers

Journey declarations live under:

~~~text
app/src/journeyTest/kotlin/.../testing/journey/
~~~

That source directory is compiled into both JVM unit tests and Android instrumentation tests.

A journey reads like the product walk:

~~~kotlin
vexJourney("User Profile form submission") {
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

Current required coverage:

~~~text
every registered Screen
every registered ActionElement
every editable TextFieldElement
every registered DialogElement / ModalBottomSheetElement
~~~

A new production action with no journey therefore fails the ordinary JVM test path with its stable
semantic ID.

Example failure shape:

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

List order already expresses journey order. Timestamp/duration formatting remains protected by
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

The catalog is parameterized directly into instrumentation tests. Adding a journey to the catalog
automatically adds an Android instrumentation case; there is no second test-method registry.

## Proof layers

~~~text
JVM:
  :app:testDebugUnitTest

  VexJourneyCoverageTest
    production semantic map coverage

  VexExpectedWalkTest
    independent expected-side grammar/oracle

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
