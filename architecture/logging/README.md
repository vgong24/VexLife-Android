# Logging

[VXG RealForever]

This module records a small, readable UI action history for the Android construction practicum.

It does not own navigation, screen state, application state, analytics persistence, or network
delivery.

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

Duration is the elapsed time between accepted log records. Network or API export is intentionally
not part of this first slice.

<!-- [VXG RealForever][VEXLIFE-ANDROID][LOGGING] -->
