# Test / Proof Incidents

[VXG RealForever]

## Why this exists

This is the small durable history for **intermittent or unexplained proof-foundation failures**.

A green retry does not delete an incident. It updates its reproduction status.

Use this when asking:

> Have we seen this proof symptom before, even if the source later passed unchanged?

This is not a general bug tracker and not a dump of every red test. Add an incident when a failure is
intermittent/unexplained or when losing its history would make a future recurrence look falsely new.

## Status vocabulary

~~~text
OPEN_UNEXPLAINED
    observed; root cause unknown

MONITORING_AFTER_HARDENING
    preventive/observability repair landed; original cause not proven

EXPLAINED
    root cause established; corrective owner/effect identified

CLOSED_FIXED
    established root cause repaired and recurrence proof completed

CLOSED_EXTERNAL
    established external/provider/host cause; repository source was not the owner
~~~

## Incidents

### TF-INCIDENT-2026-10-16-001 — connected Android suite failed once, then passed unchanged

~~~text
status = MONITORING_AFTER_HARDENING

firstObserved = 2026-10-16
lastObservedFailure = 2026-10-16
lastReproductionAttempt = 2026-10-16

surface =
    vexVerifyDevice
    -> :app:connectedDebugAndroidTest

firstCandidate =
    PR #98
    head f0e36a3d30e4351101217b482856e8c7316e4404
    tree 6bdc9d2be3770f96a1f80f8fd835dd82bbcb3f59

device =
    Samsung SM-S908U
    Android 16

firstFailure =
    connectedDebugAndroidTest reported failing tests
    exact failing testcase identity was NOT retained by the then-current gate

sameCandidateRecovery =
    focused Queue JVM PASS
    vexVerifyLocal PASS
    next device-only attempt PASS
    10 instrumentation tests
    source unchanged

reproduction =
    not reproduced on immediate retry

rootCause =
    UNKNOWN

correlatingSignal =
    both VexJourneyInstrumentedTest and VexScreenCrawlerInstrumentedTest were still using deprecated
    Compose test v1 createComposeRule / UnconfinedTestDispatcher semantics

hardening =
    migrate those rules to Compose test v2 / StandardTestDispatcher
    make connectedDebugAndroidTest always finalize into exact result inspection
    retain device result summary + connected JUnit XML/report in proof bundle

evidence =
    PR #98 A001/A002 lifecycle comments and canonical RETURN hashes
~~~

Important interpretation:

The later PASS proved that PR #98 was not demonstrated to be the cause. It did **not** establish why
the first connected run failed.

The Compose v2 migration is preventive hardening based on an official deprecation/migration signal;
it is **not** recorded as the proven root cause of this incident.

Future recurrence should update this same incident when the symptom matches, including the newly
retained testcase identity. Create a separate incident only when the symptom/owner is materially
different.

<!-- [VXG RealForever][TEST-PROOF-INCIDENTS] -->
