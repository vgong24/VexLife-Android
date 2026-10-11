# F1 — UI Blueprint / Home Practicum

[VXG RealForever]

## Question

> Can a developer open one screen file, understand what it contains and where it can lead, then run
> the same structure without rebuilding it elsewhere?

The Home practicum is the first consumer. Lived use may correct the foundation when it exposes
missing structure.

## Current construction shape

~~~text
Android window
  -> AppFrame
       -> AppBar
       -> ScreenViewport
       -> BottomNavigation
       -> Overlay/FAB slot

screens/
  primary/
    home/
    events/
    settings/
  shared/
    debug/
      loghistory/
    gallery/
      elements/
      graph/
    profile/
      UserProfileScreen.kt
    vexstream/
      VexStreamMusicScreen.kt
    vex/
      TalkToVexScreen.kt
~~~

AppFrame owns the safe drawing boundary relative to Android status/navigation/cutout space. Screens
own their internal structural tree. The shell owns app-wide chrome.

## Navigation

Primary screens are peers, not accumulated history:

~~~text
primarySelection = Home | Events | Settings
~~~

Shared destinations are pushed over the selected primary screen:

~~~text
Events
  -> Debug
~~~

Transient presentation is shallower than a pushed screen:

~~~text
Events
  -> Debug
       -> LogHistoryDialog
~~~

Back resolves from one runtime plan, from the shallowest visible layer outward:

~~~text
1. dismiss transient presentation
2. else pop screen-local NavigationTrail depth
3. else pop pushed shared/detail screen
4. else return Events/Settings to primary root Home
5. else leave Home/root behavior to Android
~~~

The toolbar Back action and Android system Back consume the same resolved `BackResolution`; they do
not independently recompute separate `canGoBack` condition trees.

## Screen authoring

A normal screen declares its layout once through `ScreenBlueprint`. The renderer walks that same
tree. Named elements are reserved for things another relationship must address.

This removes the former duplication:

~~~text
blueprint layout
+
second handwritten Compose reconstruction
~~~

Each `*Screen.kt` file also carries its ordinary `@VexScreenPreviews` entry so the visual form is
visible beside the source that owns it.

Feature-specific preview states stay in that same Screen file. `VexPreview.kt` provides only the
generic host/environment. For example, Gallery's AppGraph preview locally calls
`runtime.presentation.swapTo(GalleryScreen.AppGraphPanel)`; the shared preview layer does not know
that Gallery has an AppGraph panel.

## Source alignment

The Gradle source-alignment gate intentionally checks convention drift, not runtime behavior:

~~~text
./gradlew :app:sourceAlignmentReport
  warning-only

./gradlew :app:verifySourceAlignment
  fails on source convention drift
~~~

It watches narrow contracts such as `*Screen.kt`/ScreenBlueprint/colocated Preview naming and
explicit Room physical schema names. Navigation behavior, migrations, transactions, persistence, and
device behavior remain unit/integration/lived proof responsibilities.

## Navigate vs swap

```text
navigateTo(Screen)
  changes the semantic current Screen
  participates in app Back

swapTo(child)
  changes the selected child of one SwappableContainer inside the current Screen
  does NOT create app Back depth
```

Gallery Elements/AppGraph are peer tabs inside GalleryScreen, so they use `swapTo`. Deeper AppGraph
exploration uses `NavigationTrail`, which supplies local Back depth. If a future panel is a true app
destination rather than a peer mode, use `navigateTo` instead.

## AppGraph

The graph is a projection of registered relationships, not a second registry.

A current screen is not emitted as a traversable next target. A screen already present in the current
exploration path may be shown as an existing relationship but cannot be descended into again. Cycles
stay visible without becoming endless traversal.

## Session memory

The current UI body keeps small identity-addressed session state rather than retaining rendered UI
trees:

~~~text
PresentationState.openElementIds
  transient presentation currently open

PresentationState.selectedByContainer
  selected child of a swappable container
  e.g. Gallery Content -> AppGraph

NavigationTrail.path
  screen-local depth while that local view is mounted

AppNavigationState
  selected primary screen + pushed shared/detail screens

VexLogger.history
  bounded recent records (currently 256)
~~~

These are in-memory runtime/Compose state unless a source explicitly says otherwise. Do not treat
them as durable DataStore/database preference or process-restoration contracts.

## Form interactions

Editable fields keep draft values as presentation state. Keystrokes are not semantic runtime actions
and are not logged individually.

A submit control such as User Profile Save creates one immutable `FormSubmission` keyed by stable
TextFieldElement identities. VexRuntime logs the Save interaction once, forwards the submission to
the app-action owner, and does not include raw submitted field values in the readable diagnostic log.

## Home navigation vs feature effects

Home now contains two placeholder destination buttons:

~~~text
HomeScreen
  VexStream Music -> NavigateTo(VexStreamMusicScreen)
  Talk to Vex     -> NavigateTo(TalkToVexScreen)
~~~

Those Home buttons do not require AppActionRegistrations because navigation is owned by VexRuntime.

Each destination then contains one placeholder Invoke action whose registration stays beside that
feature Screen. This is the intended contrast between runtime navigation and feature effect ownership.

## Scoped app-action ownership

A Screen may declare an Invoke action, but its feature effect should remain perceptible beside the
Screen/domain that owns it.

~~~text
Screen element
-> VexRuntime semantic logging
-> AppActionRegistrations
-> exactly one feature-local AppActionRegistration
-> effect
~~~

VexLifeApp assembles registrations and broad-lifetime dependencies only. It should not accumulate a
global feature `when` statement, and Screens should not subscribe as broadcast listeners.
AppActionRegistrations rejects duplicate ActionKey ownership; unit tests protect that delivery behavior.

The immutable registration set is queryable by ActionKey or ActionElement. Element lookup follows
the element's declared Invoke/Sequence intent, so no second element-to-action map is maintained.

REGISTRATION_EXISTS != ACTION_IS_AVAILABLE_OR_AUTHORIZED. Registration proves wiring only; future
Relay/capability/service state must decide whether an action is currently usable.

## Android system surface

VexLifeTheme draws the app background across the full edge-to-edge window while AppFrame applies
safeDrawing to the usable screen shell. Status/navigation icon appearance follows the active
dark/light theme, so Android-owned controls remain legible without white default strips around a
dark app.

The launcher icon reuses the canonical Vex mark with Android adaptive-icon resources rather than
introducing a separate app identity.

## Logging

Navigation intent is observed against the current runtime screen before navigation state changes.
System Back is also recorded when it changes presentation/screen state.

Platform lifecycle callback order remains separate from semantic navigation order.

## Proof state

The F1/UI-body work is merged and has been exercised through lived physical-device runs, including
the primary shell, Gallery/AppGraph traversal, nested directional Back, and Android-native Back
resolution.

This is enough to use the body as the next architecture consumer. It is not a claim that aggregate
Gradle tests/assemble proof or the entire Android foundation is complete. Preserve that distinction
when re-entering Relay/AppScope/service integration work.

<!-- [VXG RealForever][VEXLIFE-ANDROID][F1-UI-BLUEPRINT-HOME] -->
