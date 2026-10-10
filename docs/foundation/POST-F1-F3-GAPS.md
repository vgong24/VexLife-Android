# After F1-F3 — Remaining Architecture Gaps

[VXG RealForever]

This document prevents the first three foundations from being mistaken for the entire Android architecture.

## Foundation slice being reviewed now

~~~text
F1 Relay / Compound Relay
  current semantic truth + bounded composition

F2 VexAppScope
  named structured coroutine ownership

F3 VexLogger
  ordered structured observability
~~~

If their focused/full build proof passes and reviewer findings are cleared, these may form one stable
architecture checkpoint without solving every future concern below.

## Next tooling phase — code coverage

Before broad UI construction, establish a code-coverage foundation for the pure Kotlin architecture
modules and app unit tests.

The next worker should evaluate current Android/Kotlin-compatible coverage tooling rather than
assuming a plugin/version from memory.

Desired outcome:

~~~text
repeatable coverage command
architecture modules included
app JVM tests included
machine-readable report
human-readable report
clear exclusions
no coverage percentage treated as semantic correctness
~~~

Coverage should reveal untested branches; it must not become a target-gaming architecture metric.

## Held architecture — startup/readiness

Real async SDK/service bootstrap still needs:

~~~text
CONSTRUCTED
!= INITIALIZED
!= READY
!= ACCEPTING_WORK
!= STOPPING
!= STOPPED
~~~

Koin resolution is not readiness.

Do not build the generic readiness system until a real async dependency earns it.

## Held Relay depth — observation identity

For network/database/sensor/Home-backed truth, initial default state must eventually distinguish:

~~~text
UNOBSERVED
EMPTY
PRESENT
HELD
UNAVAILABLE
~~~

A StateFlow initial value is not proof that the real producer has spoken.

## Held durable continuity

Still separate:

~~~text
coroutine
!= operation
!= attempt
!= durable identity
~~~

Process death/re-entry, result admission, idempotency, retry authority and WorkManager/service hosts
remain future foundations.

## Held logging depth

Future options:

- BulkDataLogging sink;
- Design Lab architecture trace viewer;
- generated stable log/ref registry;
- richer trace/cause propagation.

Do not add them merely because VexLogger now has extension seams.

## Next construction phase after coverage

Move into component-first Compose architecture:

~~~text
views/
  container/
    base/
    tooltip/
    snackbar/
  list/
    base/
~~~

Goals:

- reusable container/anchor placement;
- Tooltip complexity built from Relay + AppScope + observability;
- List items become stable relational anchors;
- component-local state stays under the component;
- Design Lab becomes executable visual documentation.

## Screen developer experience still to earn

A later screen foundation should make the common UI path shallow:

~~~text
Koin
-> thin ViewModel
-> state
-> onAction
-> Compose
~~~

A feature developer should not remember deep Relay paths or activation rituals to render a screen.

The deeper architecture must remain traceable for reviewers/debuggers without becoming daily
boilerplate.

<!-- [VXG RealForever][VEXLIFE-ANDROID][POST-F1-F3-GAPS] -->
