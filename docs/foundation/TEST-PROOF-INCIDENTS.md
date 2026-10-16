# Test / Proof Incidents

[VXG RealForever]

## Purpose

This is the Test / Proof Foundation's incident-recording contract.

Use it when asking:

> Have we seen this proof failure or reliability symptom before, even if a retry later passed?

It owns:
- when an incident should exist;
- status vocabulary;
- recurrence rules;
- a small navigation index.

It does **not** own each incident's full chronology. Those live in:

`test-proof-incidents/TF-INCIDENT-*.md`

Current proof architecture remains in:

`TEST-PROOF-ARCHITECTURE.md`

## Why the split is semantic

Two questions have independent lifecycles:

~~~text
INCIDENT CONTRACT
    How do incidents work?

INCIDENT RECORD
    What happened in this one lineage?
~~~

Do not wait for file size to force that distinction.

## When to create an incident

Create one when losing the event would materially damage future reasoning, especially when:

- a failure is intermittent or unexplained;
- unchanged source passes on retry;
- proof plumbing lost or weakened evidence;
- a platform/deprecation signal exposes a plausible systemic reliability risk;
- a future recurrence could otherwise look falsely new.

Do not create one for every ordinary deterministic failing test.

## Two independent state axes

### Failure status

~~~text
OPEN_UNEXPLAINED
    symptom observed; root cause not established

EXPLAINED
    root cause established

CLOSED_FIXED
    repository-owned cause repaired and closure proof complete

CLOSED_EXTERNAL
    external/provider/host cause established
~~~

### Hardening state

~~~text
NONE
    no systemic hardening exists

CANDIDATE
    hardening source exists but is not accepted/current

PROVED_NOT_LANDED
    exact hardening candidate is proven but not on main

LANDED
    hardening is accepted/current on main
~~~

These axes intentionally do not collapse.

Example:

~~~text
failureStatus = OPEN_UNEXPLAINED
failureRootCause = UNKNOWN

observabilityGapRootCause = KNOWN
hardeningState = CANDIDATE
~~~

Knowing why evidence disappeared does not mean we know why the test failed.

## Incident versus occurrence

~~~text
INCIDENT
    one durable symptom / likely-owner lineage

OCCURRENCE
    one observed execution relevant to that incident
~~~

Same proof task is **not** enough to mean same incident.

A future event is a likely recurrence only when:

~~~text
compatible owner
+
materially matching failure signature
+
compatible circumstances
-> likely recurrence
~~~

Otherwise create a separate incident.

No generic fingerprint engine is earned yet.

## Incident file shape

Keep a small current synthesis at the top:

~~~text
incidentRef
failureStatus
failureRootCause
observabilityGapRootCause
firstObserved
lastObservedFailure
lastReproductionAttempt
proofSurface
hardeningCandidate
hardeningState
recurrenceRule
~~~

Then preserve meaningful observations chronologically.

Finish with current interpretation:
- what is known;
- what is unknown;
- what was hardened;
- what evidence would reclassify/close the incident.

Older observations are evidence. Do not rewrite them to fit later understanding.

## Retry law

~~~text
retry PASS
    may clear:
    "this exact candidate is currently demonstrated broken"

retry PASS
    does NOT clear:
    "this proof substrate previously exhibited an intermittent failure"
~~~

A retry is another occurrence, not a time machine.

## Current incident index

### TF-INCIDENT-2026-10-16-001

Connected Android suite failed once, then passed unchanged; the original failing testcase was lost.

~~~text
failureStatus = OPEN_UNEXPLAINED
failureRootCause = UNKNOWN

observabilityGapRootCause =
    failed connected task prevented downstream result inspection

hardeningCandidate = PR #100
hardeningState = CANDIDATE
~~~

Record:

`test-proof-incidents/TF-INCIDENT-2026-10-16-001.md`

## DesignHistory relationship

Test / Proof owns operational incident truth.

VexLife DesignHistory owns the reusable continuity lesson:
- current synthesis versus historical evidence;
- preserve early debt signals;
- keep records beside the rightful owner;
- split when the semantic question splits.

A global IntegrityHistory registry is not earned.

<!-- [VXG RealForever][TEST-PROOF-INCIDENTS][CONTRACT] -->
