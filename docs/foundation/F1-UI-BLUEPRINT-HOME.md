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
       -> LogHistorySheet
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

A normal VexLife app screen extends `AppScreenBlueprint`, which layers optional feature-action
registration over the portable `ScreenBlueprint`. The Screen declares its layout once and the
renderer walks that same tree. Named elements are reserved for things another relationship must
address.

This removes the former duplication:

~~~text
blueprint layout
+
second handwritten Compose reconstruction
~~~

Each `*Screen.kt` file also carries its ordinary `@VexScreenPreviews` entry so the visual form is
visible beside the source that owns it.

Feature-specific preview states stay in that same Screen file. `VexPreview.kt` provides only the
generic host/environment plus the preview-only `VexPreviewSetup` DSL.

The app Screen object also owns the bridge from registered identity to Android realization:

~~~text
AppScreenBlueprint.renderContent(runtime, repositories, darkTheme)

default:
  render this Screen's registered content through VexElementTree

override when earned:
  Debug
    -> choose repositories.logHistory + darkTheme
    -> DebugMenuScreenContent(...)

  Profile
    -> choose repositories.userProfile
    -> UserProfileScreenContent(...)

  Gallery
    -> choose Gallery's custom Scaffold/tab realization
~~~

VexLifeApp therefore owns pager/shared-screen transitions but not a feature `when(screenId)` map.
AppScreens derives a screenId lookup from the same registered list; adding a Screen does not require
another composition-root mapping.

For example, Gallery's AppGraph preview locally declares:

~~~kotlin
previewSetup = {
    selectChild(
        container = GalleryScreen.Content,
        child = GalleryScreen.AppGraphPanel,
    )
}
~~~

The shared preview layer knows how to seed a valid local selection; it does not know that Gallery has
an AppGraph panel, and VexRuntime does not expose Preview-specific setup methods.

## Source alignment

The Gradle source-alignment gate intentionally checks convention drift, not runtime behavior:

~~~text
./gradlew :app:sourceAlignmentReport
  warning-only

./gradlew :app:verifySourceAlignment
  fails on source convention drift
~~~

It watches narrow contracts such as `*Screen.kt`/AppScreenBlueprint/colocated Preview naming,
explicit Room physical schema names, and the rule that VexLifeApp must not import feature Screens to
recreate a central render switchboard. Navigation behavior, migrations, transactions, persistence,
and device behavior remain unit/integration/lived proof responsibilities.

## Navigate vs swap

```text
navigateTo(Screen)
  changes the semantic current Screen
  participates in app Back

swapTo(container, child)
  changes one explicitly named SwappableContainer inside the current Screen
  child must be a direct child of that container
  does NOT create app Back depth
```

Gallery Elements/AppGraph are peer tabs inside GalleryScreen, so they use `swapTo`. The registry
also verifies the source button, container, and child share GalleryScreen ownership; runtime
activation fails if a Screen-owned source is activated while another Screen is current.

Deeper AppGraph exploration uses `NavigationTrail`, which supplies local Back depth. If a future
panel is a true app destination rather than a peer mode, use `navigateTo` instead.

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

VexRuntime / navigation / presentation / logger session / dark-theme selection are now owned by a
VexLifeSession retained in VexLifeViewModel, so Activity configuration recreation (for example
rotation) keeps the same semantic Screen and open modal state.

That is NOT a process-death restoration claim. Screen-local Compose-only state must still explicitly
choose rememberSaveable or another rightful longer-lived owner when it needs that behavior.

## Form interactions

Editable fields keep draft values as presentation state. Keystrokes are not semantic runtime actions
and are not logged individually.

A submit control such as User Profile Save creates one immutable `FormSubmission` keyed by stable
TextFieldElement identities. VexRuntime logs one `Submitted` interaction, forwards the submission to
the app-action owner, and serializes that submitted snapshot once as JSON diagnostic detail. Draft
keystrokes remain absent.

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

AppScreens assembles each registered AppScreenBlueprint's local registration override and validates
Invoke coverage; VexLifeSession supplies construction dependencies without maintaining another
feature list. It should not accumulate a global feature `when` statement, and Screens should not
subscribe as broadcast listeners. AppActionRegistrations rejects duplicate ActionKey ownership and
missing declared Invoke ownership; unit tests protect that delivery behavior.

The immutable registration set is queryable by ActionKey or ActionElement. Element lookup follows
the element's declared Invoke/Sequence intent, so no second element-to-action map is maintained.

REGISTRATION_EXISTS != ACTION_IS_AVAILABLE_OR_AUTHORIZED. Registration proves wiring only; future
Relay/capability/service state must decide whether an action is currently usable.

## Modal presentation samples

Debug Log History now uses Material's partial/expanded `ModalBottomSheet` states while keeping
its semantic open/closed state in VexRuntime.presentation. It opens partially so the underlying
Screen remains perceptible, can be dragged fully open for reading, and can be dragged/scrim-dismissed.
The Log History sheet has no redundant Close button; platform dismissal records `Sheet Dismissed`.

Its inner Log History scroller disables its own overscroll effect so leftover downward drag/fling can
resolve through nested scrolling to the sheet instead of visually rubber-banding at the content edge.

Gallery Elements preserves both modal realization patterns as inspectable samples:

~~~text
DialogElement
  -> AlertDialog sample

ModalBottomSheetElement
  -> draggable ModalBottomSheet sample
~~~

Both use the same local Open/Close ownership rules; the difference is Android presentation style.

## Android system surface

VexLifeTheme draws the app background across the full edge-to-edge window while AppFrame applies
safeDrawing to the usable screen shell. Status/navigation icon appearance follows the active
dark/light theme, so Android-owned controls remain legible without white default strips around a
dark app.

The launcher icon reuses the canonical Vex mark with Android adaptive-icon resources rather than
introducing a separate app identity.

## Logging

Navigation intent is observed against the current runtime Screen before navigation state changes.
System Back is also recorded when it changes presentation/Screen state.

Readable Log History groups events into Screen scopes instead of repeating the Screen per event:

~~~text
---ScreenName---
[timestamp]
Element Clicked
...
[Duration: x seconds]
---NextScreen---
~~~

Screen duration is derived from semantic entry -> transition time. Submitted form snapshots may add a
JSON block beneath the Submitted event.

Platform lifecycle callback order remains separate from semantic navigation order.

## Proof state

The F1/UI-body work is merged and has been exercised through lived physical-device runs, including
the primary shell, Gallery/AppGraph traversal, nested directional Back, and Android-native Back
resolution.

This is enough to use the body as the next architecture consumer. It is not a claim that aggregate
Gradle tests/assemble proof or the entire Android foundation is complete. Preserve that distinction
when re-entering Relay/AppScope/service integration work.

<!-- [VXG RealForever][VEXLIFE-ANDROID][F1-UI-BLUEPRINT-HOME] -->
