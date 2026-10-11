# F1 — UI Blueprint / Home Practicum

[VXG RealForever]

## Question

> Can a developer open one page file, understand what it contains and where it can lead, then run the same structure without rebuilding it elsewhere?

The Home practicum is the first consumer. Lived use may correct the foundation when it exposes missing structure.

## Current construction shape

~~~text
Android window
  -> AppFrame
       -> AppBar
       -> PageViewport
       -> BottomNavigation
       -> Overlay/FAB slot

pages/
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
      UserProfilePage.kt
~~~

AppFrame owns the safe drawing boundary relative to Android status/navigation/cutout space. Pages own their internal structural tree. The shell owns app-wide chrome.

## Navigation

Primary pages are peers, not accumulated history:

~~~text
primarySelection = Home | Events | Settings
~~~

Shared destinations are pushed over the selected primary page:

~~~text
Events
  -> Debug
~~~

Transient presentation is shallower than a pushed page:

~~~text
Events
  -> Debug
       -> LogHistoryDialog
~~~

Back resolves from one runtime plan, from the shallowest visible layer outward:

~~~text
1. dismiss transient presentation
2. else pop page-local NavigationTrail depth
3. else pop pushed shared/detail page
4. else return Events/Settings to primary root Home
5. else leave Home/root behavior to Android
~~~

The toolbar Back action and Android system Back consume the same resolved `BackResolution`; they do
not independently recompute separate `canGoBack` condition trees.

## Page authoring

A normal page declares its layout once through PageBlueprint. The renderer walks that same tree. Named elements are reserved for things another relationship must address.

This removes the former duplication:

~~~text
blueprint layout
+
second handwritten Compose reconstruction
~~~

## AppGraph

The graph is a projection of registered relationships, not a second registry.

A current screen is not emitted as a traversable next target. A screen already present in the current exploration path may be shown as an existing relationship but cannot be descended into again. Cycles stay visible without becoming endless traversal.

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
  page-local depth while that local view is mounted

AppNavigationState
  selected primary page + pushed shared/detail pages

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

## Logging

Navigation intent is observed against the current runtime page before navigation state changes. System Back is also recorded when it changes presentation/page state.

Platform lifecycle callback order remains separate from semantic navigation order.

## Proof state

The F1/UI-body work is merged and has been exercised through lived physical-device runs, including
the primary shell, Gallery/AppGraph traversal, nested directional Back, and Android-native Back
resolution.

This is enough to use the body as the next architecture consumer. It is not a claim that aggregate
Gradle tests/assemble proof or the entire Android foundation is complete. Preserve that distinction
when re-entering Relay/AppScope/service integration work.

<!-- [VXG RealForever][VEXLIFE-ANDROID][F1-UI-BLUEPRINT-HOME] -->