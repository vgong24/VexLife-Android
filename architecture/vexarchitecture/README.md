# VexArchitecture — refined foundation snapshot

[VXG RealForever]

## Accepted implementation origin and reasoning record

The first accepted executable VexArchitecture stability point that grew from this refined snapshot is:

- [PR #209 — VexArchitecture executable composition / ThemeMode patient zero](https://github.com/vgong24/VexLife-Android/pull/209)

PR #209 is intentionally useful as a **before / pressure / subtraction / after** review record. It
shows traditional-looking alternatives that were actually tried or inherited, the duplication or
ownership leaks they created, and the smaller relationships that replaced them. A fresh human or AI
may use that diff to understand *why* the current design exists instead of cargo-culting either the
old code or the final names.

PR #209 produced the first accepted executable VexArchitecture stability point. The repository's
current source and tests own current behavior. Keep the PR link here as provenance for the rejected
alternatives, pressure, subtraction, and causal reasoning; accepted source remains the repository
itself.

Fresh architecture orientation:

~~~text
root README
-> architecture/vexarchitecture/README.md
-> VEXLOGGER.md
-> contexts/logger-readable-dependencies/TOPOLOGY-AND-ID-PROTOCOL.md
   current relationship projection; navigation, not another truth owner
-> REFINED-BOUNDARIES.md
-> docs/foundation/VEX-BODY-SEMANTIC-BLUEPRINT.md
-> PR #209 before/after review when the reason for a choice matters
-> exact owning production source + tests
~~~

The current causal body is broader than execution alone:

~~~text
REQUEST / REALIZATION

Family.Parameter
-> Family.Control
-> Family.Act
-> Family.Effect
-> Family.Act.Method
   -> specializes VexMethod
-> concrete realization
-> VexMethod.Result
   -> Success / Failure / Error
-> rightful result interpretation


OUTSIDE / OBSERVATION

outside/provider truth
-> rightful observation / reconciliation
-> already-semantic Family.State


EXPERIENCE / DISPLAY

Family.State
-> Family.Projection
-> Family.DisplayState
-> concrete presentation meaning
-> concrete manifestation such as Compose
-> human experience
~~~

VexMethod.Result reports the outcome of attempting one Method realization. It is the terminal
return contract of `VexMethod.invoke()`, not a generic provider / Store / repository outcome wrapper.
Those boundaries return their smallest truthful fact; the bound Method interprets that fact into
Success / Failure. Error is formed inside the behavior foundation only when a Method actually escapes
its returned contract. Result does not become resulting State or require a subsequent Observation,
and cancellation remains Execution lifetime.

ThemeMode is the current concrete display example:

~~~text
ThemeMode State
-> ThemeModeFamily.DisplayState
-> VexThemePresentation
-> VexLifeTheme
-> MaterialTheme / system bars / Compose Surface
~~~

DisplayState is Family-owned outward interpretation. Compose is a manifestation technology, not a
Family child or semantic truth owner. Do not introduce a generic FamilyRenderer, UiState, or
ViewState merely to bridge those layers when the concrete consumer already has a truthful
presentation boundary.

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
contexts/logger-readable-dependencies/TOPOLOGY-AND-ID-PROTOCOL.md
    current cross-root relationship map; descend from it to owning source
REFINED-BOUNDARIES.md
reference/behavior/VexFamily.kt
reference/ui/ThemeMode.kt
~~~

Do not use draft PR #206 as the entry point. PR #209 is the accepted implementation-origin /
reasoning record; the snapshot below is historical reconstruction context that led into the accepted
body.

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

## Historical reconstruction rule that produced the accepted #209 body

The steps below explain how this snapshot was intended to become implementation and are preserved as
reasoning provenance. They are **not** an instruction to replay the implementation sequence now that
#209 is accepted provenance and current repository source owns behavior.

A successor implementing this architecture from this historical snapshot should:

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
