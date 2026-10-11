# Logging

[VXG RealForever]

This module records a small, readable UI action history for the Android construction practicum.

It does not own navigation, screen state, application state, database policy, or network delivery.
The app may project accepted VexLogRecord values into a repository/persistence adapter; that adapter
remains downstream and cannot change whether the diagnostic record or product action was accepted.

~~~text
runtime owner decides what happened
-> VexLogger records the accepted fact in sequence
-> readable history projects the record
~~~

The current practicum deliberately keeps semantic screen order separate from Android and Compose
lifecycle callback order. A navigation interaction can therefore be recorded as:

~~~text
[6:53:00PM Oct 10, 2026 | 120 sec]
[DebugMenu | Clicked]
[HomeScreen ScreenDisappeared -> DebugMenuScreen ScreenAppeared]
~~~

before framework lifecycle callbacks happen in whatever native order Android chooses.

Duration is the elapsed time between accepted log records. Durable/local history is now being
explored by the app-level Data/Repository foundation; VexLogger itself remains independent of Room.

<!-- [VXG RealForever][VEXLIFE-ANDROID][LOGGING] -->
