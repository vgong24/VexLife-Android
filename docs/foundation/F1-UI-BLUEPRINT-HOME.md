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

Back resolves from the topmost semantic layer:

~~~text
1. dismiss transient presentation
2. else pop pushed page
3. else leave root behavior to Android
~~~

The toolbar Back action and Android system Back consume the same navigation owner.

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

## Logging

Navigation intent is observed against the current runtime page before navigation state changes. System Back is also recorded when it changes presentation/page state.

Platform lifecycle callback order remains separate from semantic navigation order.

## Proof state

Source remains a draft candidate until the exact head compiles/tests and Victor completes the lived device walk.

<!-- [VXG RealForever][VEXLIFE-ANDROID][F1-UI-BLUEPRINT-HOME] -->