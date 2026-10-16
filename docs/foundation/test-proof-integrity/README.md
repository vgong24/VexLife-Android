# Test / Proof Integrity History

[VXG RealForever]

## Human question

> What integrity warnings has the Test / Proof Foundation already shown us, what did we learn before
> they became normalized debt, what is current, and how should a future recurrence be recognized?

This is the **current operational index** for Test / Proof integrity history.

It is not the history of VexLife in totality.

The owner boundary is:

~~~text
TEST-PROOF-ARCHITECTURE.md
    current proof architecture / current execution contract

test-proof-integrity/README.md
    current integrity posture + recurrence/index surface

test-proof-integrity/records/*
    dated evidence/reasoning for one meaningful integrity event

VexLife DesignHistory
    teaches the reusable continuity grammar
    does not centralize Test Foundation operational truth
~~~

## Why this is a folder

A single growing incident Markdown file would eventually mix:
- current open posture;
- old failures;
- resolved causes;
- preventive hardening;
- recurrence history;
- unrelated future proof surfaces.

That would recreate the giant-notebook problem DesignHistory exists to avoid.

The structure is therefore fractal:

~~~text
test-proof-integrity/
├── README.md
│   current posture / status vocabulary / record index
└── records/
    ├── YYYY-MM-DD-NNN-meaningful-integrity-event.md
    └── ...
~~~

One record captures one meaningful integrity event or near-miss, not one red test or one retry.

## What earns a record

Create a record when at least one is true:

- a failure is intermittent or unexplained;
- a retry passes but erasing the first failure would lose important evidence;
- a foundation weakness allowed a failure to become less observable than it should have been;
- a warning/deprecation reveals a plausible future systemic debt vector;
- a recurrence would otherwise look falsely new months later;
- a preventive hardening changed repository culture or proof architecture.

Do not add a record for every routine deterministic test failure.

## Status vocabulary

~~~text
OPEN_UNEXPLAINED
    observed; root cause unknown

MONITORING_AFTER_HARDENING
    preventive/observability repair formed or landed;
    original cause is still not proven

EXPLAINED
    root cause established;
    corrective owner/effect identified

CLOSED_FIXED
    established root cause repaired
    and recurrence proof completed

CLOSED_EXTERNAL
    established external/provider/host cause;
    repository source was not the owner
~~~

A green retry does not automatically move an incident to CLOSED_*.

## Record shape

A historical record should answer:

~~~text
TRIGGER
    what happened that made us look harder?

LATENT RISK
    what future debt/failure pattern could this have become?

WHY IT COULD HAVE BEEN MISSED
    what normal retry/green/abstraction behavior could have hidden it?

OBSERVED EVIDENCE
    exact facts available at the time

IMMEDIATE DISPOSITION
    what was proven and what was not proven?

SYSTEMIC HARDENING
    what changed so recurrence is easier to detect/prevent?

UNRESOLVED UNCERTAINTY
    what root cause/question remains open?

RECURRENCE KEY
    how should a future worker recognize "we have seen this before"?

RIGHTFUL OWNER
    where does operational current truth live?

CURRENT CONSEQUENCE
    what source/culture/proof rule changed because of this?
~~~

Preserve uncertainty. Preventive hardening is not proof of historical root cause.

## Current records

### TF-INTEGRITY-2026-10-16-001

**Connected Android suite failed once, then passed unchanged.**

Current status:

~~~text
MONITORING_AFTER_HARDENING
rootCause = UNKNOWN
~~~

Record:

[records/2026-10-16-001-connected-android-intermittent-failure.md](records/2026-10-16-001-connected-android-intermittent-failure.md)

Recurrence key:

~~~text
vexVerifyDevice / connectedDebugAndroidTest
intermittent connected instrumentation failure
same source may pass on retry
especially Journey / Compose synchronization symptoms
~~~

## Current rule

~~~text
retry PASS
    clears:
    "this exact candidate is currently demonstrated broken"

retry PASS
    DOES NOT clear:
    "this proof foundation has previously exhibited an intermittent failure"
~~~

A future matching failure should update the relevant record/index rather than create the fiction that
the symptom is brand new.

<!-- [VXG RealForever][TEST-PROOF-INTEGRITY][CURRENT] -->
