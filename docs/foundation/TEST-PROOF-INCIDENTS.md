# Test / Proof Incidents

[VXG RealForever]

## Human question

> How should Test / Proof record an intermittent, unexplained, or near-miss proof event so a retry,
> time, or future feature cannot make it look falsely new?

This file is the **incident-recording contract and navigation surface**.

It does not contain the full history of every incident.

Current proof architecture remains in:

`TEST-PROOF-ARCHITECTURE.md`

Individual incident histories live in:

`test-proof-incidents/`

## Why this is split

Two questions already have independent lifecycles:

~~~text
QUESTION A — INCIDENT GRAMMAR
    When should an incident exist?
    What statuses mean?
    What counts as recurrence?
    Where should a fresh worker look?

QUESTION B — ONE INCIDENT
    What exactly happened?
    Which executions observed it?
    What evidence accumulated?
    What was hardened?
    What remains unknown?
~~~

Therefore:

~~~text
TEST-PROOF-INCIDENTS.md
    small current contract
    vocabulary
    recurrence law
    navigation index

test-proof-incidents/TF-INCIDENT-*.md
    one independently evolving incident history
~~~

The split is semantic, not based on line count.

## What earns an incident

Create an incident when at least one is true:

- a proof failure is intermittent or unexplained;
- the same exact source passes on retry but the first red still matters;
- a proof-system weakness caused evidence to be lost or misclassified;
- a warning/deprecation reveals a plausible systemic reliability risk worth tracking;
- a recurrence would otherwise look falsely new months later;
- preventive hardening is justified even though historical root cause is still unknown.

Do **not** create an incident for every ordinary deterministic failing test.

## Two independent state axes

Do not make one status imply both root-cause knowledge and hardening lifecycle.

### Failure status

~~~text
OPEN_UNEXPLAINED
    the observed failure/symptom has no established root cause

EXPLAINED
    failure root cause is established

CLOSED_FIXED
    established repository-owned root cause was repaired
    and closure evidence is complete

CLOSED_EXTERNAL
    established external/provider/host cause;
    repository source was not the owner
~~~

### Hardening state

~~~text
NONE
    no systemic hardening formed

CANDIDATE
    hardening source exists but is not yet accepted/current

PROVED_NOT_LANDED
    exact hardening candidate has adequate proof
    but is not yet accepted on main

LANDED
    hardening is accepted/current on main
~~~

Example:

~~~text
failureStatus = OPEN_UNEXPLAINED
failureRootCause = UNKNOWN

observabilityGapRootCause = KNOWN
hardeningState = CANDIDATE
~~~

That is not contradictory. We may know why evidence disappeared without knowing why the test failed.

## Incident versus occurrence

~~~text
INCIDENT
    one durable symptom / likely-owner lineage

OCCURRENCE
    one observed execution relevant to that incident
~~~

One incident may accumulate multiple occurrences.

But:

~~~text
same Gradle task
!= same incident
~~~

Treat a future failure as a likely recurrence only when:

~~~text
same or compatible owner
+
materially matching failure signature
+
compatible execution circumstances
-> likely recurrence
~~~

If those do not materially agree, create a separate incident rather than turning one record into
"Android tests are sometimes weird."

No generic fingerprint engine is earned yet. Human-readable evidence is sufficient.

## Individual incident file shape

Each incident file has two layers.

### Current synthesis

Keep this small and immediately readable:

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

### Historical evidence

Append meaningful observations chronologically:

~~~text
OBSERVATIONS

<date / occurrence>
    exact observed facts
    candidate/device/provider coordinates
    what passed/failed
    what evidence existed

CURRENT INTERPRETATION
    what we know
    what we explicitly do not know
    what was hardened
    what evidence would explain/close/reclassify the incident
~~~

Do not rewrite older observations to match later understanding.

## Retry law

~~~text
retry PASS
    may clear:
    "this exact candidate is currently demonstrated broken"

retry PASS
    does NOT clear:
    "this proof substrate previously exhibited an intermittent failure"
~~~

A retry is another observation, not a time machine.

## Current incident index

### TF-INCIDENT-2026-10-16-001

**Connected Android suite failed once, then passed unchanged; original failing testcase was lost.**

Current synthesis:

~~~text
failureStatus = OPEN_UNEXPLAINED
failureRootCause = UNKNOWN

observabilityGapRootCause =
    connectedDebugAndroidTest failure prevented downstream result inspection

hardeningCandidate = PR #100
hardeningState = CANDIDATE
~~~

Record:

`test-proof-incidents/TF-INCIDENT-2026-10-16-001.md`

## DesignHistory relationship

Test / Proof owns operational incident currentness.

VexLife DesignHistory owns the reusable continuity pressure:

- current synthesis versus historical evidence;
- preserve early debt signals;
- do not centralize every domain's operational history;
- grow owner-local record structure when the semantic question splits.

A global IntegrityHistory registry is not earned.

<!-- [VXG RealForever][TEST-PROOF-INCIDENTS][CONTRACT] -->
