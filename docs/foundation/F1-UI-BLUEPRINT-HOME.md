# F1 — UI Blueprint / Home Practicum

`[VXG RealForever]`

## The question

> Can a human or AI define a UI from small reusable cells, understand its placement before reading
> Compose code, and let runtime behavior reuse the same identity for navigation, logging, tests, and
> inspection?

This foundation is intentionally learned through one real practicum rather than designed as a
complete UI framework in advance.

## Developer path

A new screen should be understandable in this order:

```text
1. define the screen identity
2. define its meaningful elements
3. give action-bearing elements a small sentence-like intent
4. compose those elements with row / column / stack / scrollable / dialog / swappable placement
5. register the ScreenBlueprint
6. render with reusable cells
```

The declaration carries what an element is. The placement tree carries where it sits. Runtime state
carries where the user actually is now.

```text
ELEMENT DECLARATION != PLACEMENT
PLACEMENT != RUNTIME SCREEN CONTEXT
PLATFORM LIFECYCLE ORDER != SEMANTIC SCREEN HISTORY
```

## First practicum

The first application specimen provides:

- VexLife toolbar using the current Vex mark and brand language;
- Home / Events / Settings primary screens in one horizontal pager;
- intentionally provisional scrollable preview bodies;
- shared expandable FAB with Debug Menu and Gallery;
- Debug Menu with dark-mode toggle and readable Log History;
- Gallery with reusable Elements and generated AppGraph views;
- AppGraph depth navigation derived from the registered blueprint rather than another hand-maintained
  topology;
- ordered local action history with elapsed seconds between accepted events.

The primary shell owns toolbar, bottom navigation, and FAB placement. Those elements deliberately
have no static owning screen. Runtime ScreenRelay context supplies the actual screen from which a
shared-shell interaction occurred.

## Design lineage used

The Android practicum ports the current VexLife visual vocabulary rather than copying browser
implementation structure:

- dark/light surface palette;
- Vex mark geometry;
- VexLife brand and current-context tagline;
- 70-unit top/bottom shell proportions;
- existing tactile/surface/spatial motion timing as Android animation tokens.

These values are now Android-owned source. There is no runtime or build-time coupling to a VexLife
checkout.

## Logging order

A navigation interaction is accepted and logged before ScreenRelay changes current screen:

```text
Button/FAB activation
-> snapshot current semantic screen
-> record Clicked with before/after semantic screen
-> apply declared navigation intent
-> commit ScreenRelay target
```

This is deliberate. Android Activity/Compose lifecycle callback ordering does not define VexLife
semantic screen history.

The readable projection can therefore preserve:

```text
[time | duration][DebugMenu | Clicked]
[HomeScreen ScreenDisappeared -> DebugMenuScreen ScreenAppeared]
```

even if native composition/lifecycle callbacks occur in a different implementation order.

## What this slice does not claim

- no BulkData/API export or durable log persistence;
- no product content for Home, Events, or Settings beyond the explicit preview;
- no persistent theme preference;
- no Home/network/model integration;
- no claim that the current cell vocabulary is complete;
- no claim that source tests or APK build have run merely because the source is formed;
- no physical-device acceptance yet.

The next proof for this candidate is focused unit tests + app unit tests + debug APK assembly, then a
real-device walk of pager navigation, FAB expansion, Debug history, theme toggle, Gallery elements,
and AppGraph depth navigation.

<!-- [VXG RealForever][VEXLIFE-ANDROID][F1-UI-BLUEPRINT-HOME] -->
