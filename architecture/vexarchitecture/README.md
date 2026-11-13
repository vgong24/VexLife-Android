# VexArchitecture — current entry + refined foundation snapshot

[VXG RealForever]

## Current source authority and accepted origin

Current repository source and tests own present VexArchitecture behavior.

The first accepted executable stability point is:

- [PR #209 — VexArchitecture executable composition / ThemeMode patient zero](https://github.com/vgong24/VexLife-Android/pull/209)

The accepted external-participant stability point is:

- [PR #219 — VexPartner Source / Conversion through CGM, connected pen, and insulin pump](https://github.com/vgong24/VexLife-Android/pull/219)

PR #209 remains the before / pressure / subtraction / after reasoning record for the first body.
PR #219 remains the pressure/proof record for the Partner root. Neither PR replaces current source.

## Fresh architecture orientation

A fresh human or parallel AI should not have to reconstruct the current body from PR chronology.

~~~text
root README
-> architecture/vexarchitecture/README.md
-> VEXLOGGER.md
-> architecture/behavior/FAMILY-CIRCUIT.md
-> architecture/behavior/README.md
-> architecture/execution/README.md
-> architecture/widget/README.md when a system surface is implicated
-> architecture/relay/README.md when independently-owned truths converge
-> architecture/dependency/README.md when construction/lifetime is implicated
-> contexts/logger-readable-dependencies/TOPOLOGY-AND-ID-PROTOCOL.md
   current relationship projection; navigation, not another truth owner
-> docs/foundation/VEX-BODY-SEMANTIC-BLUEPRINT.md
-> exact owning production source + tests
~~~

Use `REFINED-BOUNDARIES.md`, `reference/`, and PR #209 when the reason/history of the first
stability point matters. They are provenance/reference, not a substitute for the current owners above.

## Current accepted architecture map

Read VexArchitecture by the question each owner answers, not as a mandatory class stack.

~~~text
SEMANTIC MEANING
    VexFamily
        CURRENT State + Control
        -> accepted Act
        -> admitted State when authored
        -> optional Begin(Effect + Approach)
        -> exact Act.Method when realization is required

BOUND REALIZATION
    VexMethod
        one fully-bound executable contract
        -> Success / Failure
        escaped invocation -> Error at the behavior/execution continuation
        != provider/Store truth
        != resulting State

ACTIVE ASYNC CUSTODY
    VexExecution
        source-addressed work when the exact callable/Flow is the local work address
        relationship-addressed work when Family already authored RelationshipId + Approach
        concrete VexExecution owner only when a distinct sustained custody responsibility is earned
        ordinary owner-local coroutine lifetime may remain rightful when no VexExecution invariant is needed

EXTERNAL PARTICIPATION
    VexPartner
        Source
        Signal
        Connection
            Id
            Conversion
            Observation
            Receiver
        != Store
        != Family
        != VexExecution
        != every provider by default

OUTSIDE / DURABLE TRUTH
    Store / provider / platform owner
        returns or exposes its smallest truthful fact
        keeps independently-owned reality outside Family

RELATIONSHIP TRUTH
    Relay / Compound Relay
        composes independently-complete truths
        owns the relationship among them
        does not steal either source owner

REMOTE SYSTEM SURFACE
    Android Widget foundation
        rightful product/provider truth
        -> feature-owned VexWidgetSurfaceState
        -> VexWidgetProjection
        -> Glance / RemoteViews
        callback -> existing product request/Control boundary
        APP_WIDGET != PRODUCT_STATE_OWNER
        CALLBACK != LONG_RUNNING_WORK_OWNER

SUSTAINED PLATFORM REALIZATION
    concrete Android Service when a feature actually needs one
        keeps platform/provider realization alive beyond a shorter caller/UI lifetime
        semantic/provider truth stays with its rightful owner

    current source earns this composition relationship
    current source does NOT earn a generic VexService root,
    universal Service.current, or mandatory ServiceScope/VexExecution shape

CONSTRUCTION / LIFETIME
    VexDependency
        provider prerequisites
        lifetime memoization / parent-child reuse
        construction identity != semantic identity

CHRONOLOGY
    VexLogger
        what happened and in what order
        != active-work custody
        != semantic truth

EXPERIENCE / DISPLAY
    Family State
    -> Family Projection / DisplayState where earned
    -> concrete presentation
    -> manifestation such as Compose / Glance / Android platform
    -> human experience
~~~

## Worked relationship examples

### Direct Family + Method + Execution — ThemeMode

~~~text
human Select(mode)
-> ThemeModeFamily.Control.Select
-> CURRENT State + Control
-> Act.Accepted
   -> admitted State when authored
   -> realizationBegin = Latest(Effect.Save)
-> exact ThemeModeFamily.Act.Method.Save
-> Act.Accepted.launch(...)
-> VexExecution Job custody
-> ThemeModeStore.save(...)
-> Method-owned VexMethod.Result
-> independently observed durable ThemeMode truth
-> same ThemeModeFamily
-> DisplayState / VexThemePresentation
-> Compose
~~~

This example teaches semantic carriage, bound realization, active custody, durable observation, and
presentation without making any one layer own the others.

### Direct Family + Partner + Store — current VexStream Playback

~~~text
human Play/Pause
-> VexStreamPlaybackFamily
-> Act.Accepted
-> realizationBegin
-> exact Effect-bound Act.Method
-> VexExecution when the accepted relationship requires custody
-> VexStreamPlayerPartner.Connection.Request
-> provider acknowledgement
   != durable outside truth

later Player truth
-> VexStreamPlayerPartner.Signal.Playback
-> VexStreamPlayerStore
-> source-addressed Store observation
-> same VexStreamPlaybackFamily
-> DisplayState / presentation
~~~

Playback, Queue, and Repeat are current Direct Family consumers. Do not reconstruct their current
shape from older compatibility-Behavior VexStream source or historical PRs.

### Partner — CGM / connected pen / insulin pump

~~~text
BLE-like bytes -> CgmPartner.Connection.BleDecoder --\
                                                    +-> CgmPartner.Source / Signal
HTTP-like body -> CgmPartner.Connection.HttpDecoder-/
                                                    -> Observation
                                                    -> suspend Receiver
                                                    -> CgmStore
                                                    -> CgmFamily

ConnectedPenPartner
    same Partner grammar
    different domain meaning
    no imported CGM continuity law

InsulinFamily Control
-> Effect / exact Method
-> VexExecution
-> InsulinPumpPartner request
-> acknowledgement
   != physical delivery

later DeliveryObserved
-> Partner inbound truth
-> rightful consumer truth
~~~

Partner is earned by an independently-owned external-participant question, not by a class name or by
the fact that data crosses a network/device boundary.

### Widget — presentation custody, not product truth

~~~text
rightful product / provider / repository truth
-> feature-owned widget SurfaceState
-> foundation persistence / currentness
-> Widget Projection
-> Glance / RemoteViews

widget tap
-> ActionRef
-> bounded callback
-> feature resolver
-> existing product Control / request boundary
~~~

The Widget foundation does not choose whether its consumer uses Direct Family, compatibility
Behavior, or another lawful typed request boundary.

Currentness note:

~~~text
accepted Widget foundation relationship
    PR #174 / #185 provenance

publication request debt after persisted SurfaceState
    active correction: PR #255

Widget / Foundation-Index teaching currentization
    active correction: PR #253
~~~

The active correction does not turn Widget into product State or another execution root. When working
on Widget publication/currentness, descend into those owning candidates/current main rather than
assuming this overview proves the pending update-request lifecycle.

### Android Service — sustained realization law, reusable root still held

The original Service pressure was valid: make the relationship between a sustained platform
realization and the truth it participates in **perceivable** without allowing an Android Service to
become the semantic/current owner.

Current concrete witnesses:

~~~text
VexStreamPlaybackService
    MediaSession / ExoPlayer host lifetime
    Media3PlayerConnection -> VexStreamPlayerStore attachment
    Service-lifetime Widget publication custody
    does not require universal PlaybackFamily.current awareness

VexMapsNavigationService
    foreground-Service lifetime
    Android location / notification / speech realization
    borrows VexNavigationSession current truth because this realization needs it
~~~

The common relationship that survives both is **sustained realization hosting**:

~~~text
shorter caller / UI lifetime may end
-> concrete platform realization must remain available
-> Service hosts / attaches / maintains / releases realization resources
-> semantic / provider / Store / Session truth stays with its rightful existing owner
~~~

Permanent Service laws:

~~~text
SERVICE_CURRENT_AWARENESS
!= SERVICE_OWNED_CURRENT_TRUTH

SERVICE_ALIVE
!= EFFECT_SUCCESS
!= PROVIDER_SUCCESS

SERVICE
!= FAMILY
!= PARTNER
!= STORE
!= RELAY
!= VEXEXECUTION
~~~

What current source does **not yet** prove is that every materially different Service shares one
compiler-visible `Service <-> Current` contract or one reusable lifecycle algorithm. Playback does
not need the same semantic-current relationship that Navigation needs. Therefore the current
promotion boundary is:

~~~text
SUSTAINED REALIZATION RELATIONSHIP = EARNED

VexService<Service, Current>
    original intention = VALID PRESSURE
    universal reusable type = NOT YET EARNED

universal Service.current               = NOT YET EARNED
mandatory VexExecution in every Service = NOT EARNED
typed ServiceScope architecture law     = NOT EARNED
BackgroundServiceRelay by symmetry      = NOT EARNED
~~~

Do not erase the Service question merely because its first generic type did not survive. Preserve the
relationship law, pressure it with materially different sustained realizations, and promote a
`VexService` root only when a repeated executable invariant would otherwise be duplicated or become
easy to violate. A future shared Admission / Retention / Release law could earn a root even if the
earlier universal `current` getter does not.

### Compatibility Behavior remains real where current source still uses it

Direct `VexFamily` is the common new/materially-evolving Family authoring route. Existing
`VexBehaviorFamily` / `VexBehaviorBlueprint` remains accepted compatibility source where current
production still consumes it, including current VexMaps Distribution Mode / Delivery Route.

~~~text
CURRENT SOURCE EXAMPLE
    VexMaps Distribution
    -> compatibility Behavior owners
    -> Session
    -> VexExecution only for earned custody
    -> Relay / convergence for relationship truth

COMPATIBILITY
!= PEER NEW-FAMILY TEMPLATE
!= DEPRECATED-BY-ASSUMPTION
~~~

## Reuse the question, not the syntax

Two consumers may express the same ownership law with different Kotlin shapes. Two similar code
shapes may answer different questions.

Before adding or promoting a reusable root, ask:

~~~text
What exact question recurs?
Which current owner already answers part of it?
What remains after Family / Partner / Store / Relay / Execution / Dependency / platform ownership
are subtracted?
Which materially different consumers preserve the same semantic promise?
What correctness would be lost if the shared abstraction did not exist?
~~~

If the answer is only "the code looks similar" or "the architecture diagram would look cleaner", keep
the relationship local and document the negative result.

## Apply VexArchitecture in another environment

Do **not** port the Android/Kotlin class graph. Port the ownership questions and only then choose the
native mechanism available in the target environment.

~~~text
1. HUMAN / PRODUCT PRESSURE
   What is the person trying to do or understand?

2. RIGHTFUL CURRENT TRUTH
   Which owner can answer what is true now?

3. SEMANTIC DECISION
   Does one domain own CURRENT State + Control -> accepted consequence?
       yes -> Family-shaped meaning may be present

4. EXTERNAL PARTICIPATION
   Is there an independently meaningful outside participant with Source / Signal /
   Connection / Conversion / Observation relationships?
       yes -> Partner-shaped relationship may be present

5. ACTIVE CUSTODY
   Does work need overlap policy, exact-source replacement/cancellation,
   bounded recovery, or owner-isolated stop/join?
       yes -> VexExecution-shaped custody may be present
       no  -> ordinary native coroutine/task lifetime may remain rightful

6. RELATIONSHIP TRUTH
   Do several independently complete truths create a new question together?
       yes -> Relay / convergence may be present

7. REMOTE / HOSTED SURFACE
   Must current truth be reproduced on a platform-hosted surface after the
   foreground/session may be gone?
       yes -> Widget-style bounded SurfaceState / Projection may be present

8. SUSTAINED PLATFORM REALIZATION
   Must a concrete realization outlive the initiating caller/UI?
       yes -> preserve the Service sustained-realization law;
              do not manufacture a generic Service root before the lifecycle invariant repeats

9. CONSTRUCTION / LIFETIME
   What creates these owners, what are their prerequisites, and how long are they reused?

10. CHRONOLOGY / PROOF
    What happened belongs to Logger/proof; do not turn history into another runtime owner.
~~~

Examples translate by **question**, not by Android syntax:

~~~text
Android ThemeMode Family
    -> on another platform: one semantic owner still answers CURRENT choice + request meaning;
       persistence/task/UI mechanisms may be completely different

Android VexStream Player Partner
    -> on desktop/server/embedded: external participant may be a process, socket, daemon, device,
       SDK or transport; Partner is earned by the relationship, not by Bluetooth/HTTP vocabulary

Android Widget
    -> on another hosted surface: preserve bounded remote-surface truth and action return;
       do not copy Glance if the platform uses tiles, complications, tray surfaces or extensions

Android Service
    -> on another platform: preserve the sustained-realization question;
       use the platform's background/session/daemon mechanism without inventing VexService by name

VexExecution
    -> use the target runtime's task primitive underneath;
       preserve only the custody invariant that was actually earned
~~~

A fresh instance should be able to explain the target design without mentioning any Vex class name
first. If the ownership/relationship sentence is unclear without the class name, the abstraction has
not yet been understood well enough to copy.

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

After `VEXLOGGER.md`, continue through the current owners implicated by the work:

~~~text
README.md                         <- you are here
../behavior/FAMILY-CIRCUIT.md     current Direct Family / Method circuit
../behavior/README.md             Direct Family + accepted Partner + compatibility Behavior map
../execution/README.md            execution-custody patterns and necessity boundary
../widget/README.md               remote-system-surface / callback boundary when Widget is implicated
../relay/README.md                independently-owned truth -> relationship truth
../dependency/README.md           construction / lifetime topology
contexts/logger-readable-dependencies/TOPOLOGY-AND-ID-PROTOCOL.md
                                  current cross-root relationship projection
../../docs/foundation/VEX-BODY-SEMANTIC-BLUEPRINT.md
                                  purpose -> primitive -> relationship -> form orientation
exact owning production source + tests
~~~

Use `REFINED-BOUNDARIES.md` and `reference/` when historical refinement/provenance is needed. They
remain reference snapshots, not a competing current-entry map.

Do not use draft PR #206 as the entry point. PR #209 is the accepted implementation-origin /
reasoning record; PR #219 is accepted Partner provenance. Current repository source and tests own the
present body. The snapshot below is historical reconstruction context that led into the accepted
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

## Reference snapshot posture at refinement time

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
