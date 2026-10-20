# Debug Menu design record — Feature Lab composition pattern

[VXG RealForever]

Recorded at: **2026-10-15T23:41-07:00**  
Record ref: `design.debug-menu.2026-10-15.001`  
Pressure source: `feature.vexlife.conversation`

## Discussion / pressure

Conversation needed to become visible before a real remote human/Home/model environment was
available.

A naive solution would put mock participant/message behavior directly inside Debug Menu or directly
paint fake Conversation UI. That would make the developer surface a second feature owner.

The existing Events Simulator showed a better direction: developer controls can create synthetic
external conditions while normal feature state/rendering remains authoritative.

## Decision

Debug Menu is the reusable **feature-lab doorway**.

~~~text
Debug Menu
-> feature-owned synthetic controller/lab
-> normal feature repository/state
-> normal product UI
~~~

The feature owns scenario semantics. Debug Menu owns only the developer-facing composition/action
surface.

## Consequences

For Conversation:

- Conversation owns participants, simulated remote speech, mirror behavior and currentness
  scenarios.
- Debug Menu exposes controls into that owner.
- Conversation screen never needs a debug rendering path.
- The in-memory provider remains one explicit feature provider instance, not a Debug singleton.

For future features:

- reuse the relationship, not Conversation's scenario vocabulary;
- add a lab only when unavailable external conditions materially block lived feature pressure;
- avoid one central Debug fixture containing every feature's state.

## Why this is preferable

~~~text
feature-scoped semantics
+ shared developer doorway
= scalable Debug Menu without semantic centralization
~~~

This record does not authorize a global generic simulator framework.

<!-- [VXG RealForever][DEBUG-MENU][DESIGN-RECORD-001] -->
