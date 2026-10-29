# Logging

[VXG RealForever]

This module records a small, readable UI action history for the Android construction practicum.

It does not own navigation, Screen state, application state, database policy, or network delivery.
The app may project accepted VexLogRecord values into a repository/persistence adapter; that adapter
remains downstream and cannot change whether the diagnostic record or product action was accepted.

~~~text
runtime owner decides what happened
-> VexLogger records the accepted fact in sequence
-> screenScopes derives Screen visit ranges
-> readable history projects those scopes
~~~

## Rooted structured event boundary

`VexLogger` is the logging root. Foundations that need to report accepted structured facts depend
on a child contract of that root rather than creating another `Vex*Logger` peer:

~~~text
VexLogger
-> Sink
   -> None

VexLogger
-> Event
   -> Family
      -> ControlAccepted
      -> StateAdmitted
      -> OperationBegan
      -> OperationSuccess
      -> OperationFailure
~~~

A `VexLogger.Sink` decides how structured foundation events are projected. Family events do not
automatically become canonical UI-history `VexLogRecord` entries; this preserves the distinction
between the logger root and its current UI-history projection.

A consuming foundation should keep its logger dependency private and report only facts it has
actually accepted. Concrete semantic decision functions should not gain logging capability merely
because their parent uses the logger.

## Readable protocol

The readable projection groups events by semantic Screen rather than repeating the Screen on every
record:

~~~text
---HomeScreen---
[6:51:00PM Oct 10, 2026]
App Opened

[6:53:00PM Oct 10, 2026]
DebugMenu Clicked

[Duration: 120 seconds]

---DebugMenuScreen---
[6:54:00PM Oct 10, 2026]
Reset Clicked
~~~

A transition event belongs to the Screen where the action occurred. The destination Screen scope
begins at the same transition timestamp. Duration is therefore one Screen entry -> exit range, not a
duplicate duration beside every event.

The canonical record still retains stable subject refs and before/after Screen IDs, so the readable
projection can be regenerated/reinterpreted without making the formatted text the source of truth.


## Logcat developer projection

Every accepted canonical `VexLogRecord` is also projected to Android Logcat under one stable tag:

~~~text
VexLogger
~~~

Android Studio filter:

~~~text
tag:VexLogger
~~~

ADB filter:

~~~bash
adb logcat -s VexLogger:I '*:S'
~~~

Example:

~~~text
seq=7 kind=Interaction subject=element.user-profile.body.profile.actions.save action=Submitted before=screen.user-profile after=- section=2 delta=3s details={"name":"Victor"}
~~~

Logcat is not canonical history and is not durable evidence. It is a live developer/debugging surface.
The in-memory `VexLogRecord` sequence remains canonical for the logger session; persistent LogHistory
remains a downstream repository projection. Logcat and repository persistence are failure-isolated so
one diagnostic surface cannot suppress the other.

## Form submissions

Draft typing is not logged.

At an explicit submit boundary, one record may contain the immutable submitted snapshot as JSON:

~~~text
[10:05:18PM Oct 10, 2026]
Save Submitted
{
  "element.user-profile.body.profile.display-name": "Victor"
}
~~~

Stable element refs are used as JSON keys rather than localized labels. This captures what was
actually submitted without recording keystroke order.

## Session boundaries

A new AppOpened record starts a new readable Screen scope. The formatter does not invent a duration
across time when the previous process was not observed ending.

Activity/configuration recreation is now handled above this module by the ViewModel-owned VexLife
session, so rotation should not create a second AppOpened merely because the UI was rebuilt.

Durable/local history is projected through the app-level Data/Repository foundation; VexLogger itself
remains independent of Room.

<!-- [VXG RealForever][VEXLIFE-ANDROID][LOGGING] -->
