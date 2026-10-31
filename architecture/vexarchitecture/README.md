# VexArchitecture — refined foundation snapshot

[VXG RealForever]

## READ FIRST — VexLogger

Before reading the rest of this package, read:

~~~text
VEXLOGGER.md
~~~

fully.

VexLogger is the stable architecture owner for chronological traceability, causal history, accepted /
observed facts, expectation-vs-observation separation, and the anti-accretion rule for trace IDs,
correlation IDs, execution history, status ledgers, parallel logger roots, and other observability
machinery.

This ordering is deliberate. A newcomer must understand **where traceability already belongs** before
reading RelationshipId / Execution, otherwise it is too easy to mistake runtime custody for history
ownership and recreate the OwnerToken / SourceRef / InstanceId / completed-Snapshot style machinery
this snapshot is designed to subtract.

After `VEXLOGGER.md`, continue with:

~~~text
README.md                         <- you are here
REFINED-BOUNDARIES.md
reference/behavior/VexFamily.kt
reference/ui/ThemeMode.kt
~~~

Do not use draft PR #206 as the entry point.

> **REFERENCE SNAPSHOT — NOT A MERGE RECOMMENDATION**
>
> This directory is intentionally not registered as a Gradle module. It is a clean reconstruction
> base for future source work after the architectural review of draft PR #206. A future instance
> should branch from then-current `main` and recreate a fresh implementation PR from this snapshot;
> it should **not** continue the #206 branch merely because #206 contains more history.

## Why this exists

Draft PR #206 accumulated valuable discovery together with 100+ commits of experimentation,
renaming, regression, correction, and contradictory architectural claims. Its latest source is useful
provenance, but its branch history is not a safe canonical starting point for another AI instance.

This snapshot preserves only the parts that survived the subtractive review far enough to be useful
as a clean reference:

~~~text
VexFamily
    State
    Control
    Act
    Effect

CURRENT State + Control
-> Act?
-> admitted State? + Effect?

DisplayState<Family, State>
Projection<Family, State, Display>
~~~

and the ThemeMode patient-zero language:

~~~text
Control.Select(mode)
-> Effect.Save(mode)
~~~

The word **Save** is preserved deliberately. The durability work originally described a latest
durable Save and the concrete Store operation remains `save(...)`. Later names such as
`PersistSelection` and `PersistenceExecution.persist(...)` are not treated as authoritative simply
because they appeared later in #206.

## Reference source

~~~text
reference/behavior/VexFamily.kt
reference/ui/ThemeMode.kt
~~~

These are source snapshots, not production duplicates. They exist so a successor can recover the
refined semantic grammar without replaying #206 or silently inheriting its later vocabulary.

## Current architecture posture

~~~text
PRESERVE

Direct Family semantic grammar
Family-owned DisplayState / Projection coordinates
Control payload vs typed identity distinction as a demonstrated requirement
Connection/runtime outside Family
Effect as requested runtime transformation, not resulting truth
Save language for ThemeMode durability

SETTLED ARCHITECTURAL LAW — IMPLEMENTATION SPELLING MAY STILL BE REFINED

RelationshipId
    active-work identity derived from the already-authored Control kind
    plus only the typed child/sub-IDs that actually distinguish that relationship

VexExecution active custody
    one live VexExecution owner keeps:
    RelationshipId -> actual Job(s)

    Job completion removes that Job
    no Jobs remaining removes the RelationshipId
    no second activeExecutions truth

overlap behavior
    Earliest / Latest / Queued operate on the Job(s) already stored under the same RelationshipId
    no second CoordinationKey identity vocabulary

cancellation
    cancelActiveRelationship(RelationshipId)
    cancels the current Job(s) under that exact relationship
    absent relationship is naturally a no-op
    no callable / SourceRef / InstanceId / retained Handle required

RE-DERIVE BEFORE IMPLEMENTING

exact minimal Kotlin syntax/visibility for the already-settled RelationshipId law
observation/recovery realization
VexLogger exact record/event schema
VexBoard exact projection

DO NOT CARRY FORWARD MERELY BECAUSE #206 HAS THEM

OwnerToken
CoordinationKey
CoordinationIdentity
SourceRef as architectural identity
process-global InstanceId
public Handle as required architecture
Snapshot as a second runtime history model
activeExecutions as a second active-work truth
callable-based cancellation identity
ThemeModeRelationships registry
family.relationship(control)
append(effect) as automatic identity
PersistSelection / PersistenceExecution.persist naming
~~~

The full reasoning boundary is recorded in `REFINED-BOUNDARIES.md`.

Traceability has a dedicated stable owner contract:

~~~text
VEXLOGGER.md
~~~

Any future change proposed for "traceability", "history", "correlation", "causal trail", or
"debuggability" must consult that contract before creating another ID, ledger, registry, logger root,
or completed-execution history surface.

## Provenance, not authority

~~~text
clean base used for this snapshot
772f506de2df66d372977ad487028289405fc10b

review/discovery branch head consulted
b334379efc0b3a986e56159280407bbc61cea8df

old draft review PR
#206
~~~

Those hashes explain where the snapshot came from. They do not make #206 current architecture
authority.

## Reconstruction rule

A successor implementing this architecture should:

~~~text
1. start from current main
2. read VEXLOGGER.md fully
3. read README.md and REFINED-BOUNDARIES.md
4. port the preserved source shape intentionally
5. implement the settled RelationshipId -> Job(s) custody/cancellation law without adding another identity layer
6. refine only the still-held observation/recovery and Logger record-schema details without changing VEXLOGGER.md ownership laws
7. prove each preserved requirement against the smaller implementation
8. open a fresh clean PR whose commits explain the accepted reasons
~~~

Do not preserve a type, key, helper, registry, alias, or diagnostic field merely because deleting it
would require rewriting #206 tests. Preserve the requirement the test was trying to prove and ask
whether the smaller architecture proves it more directly.

<!-- [VXG RealForever][VEXARCHITECTURE][REFINED-SNAPSHOT][REFERENCE-ONLY] -->
