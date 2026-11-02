# Topology + ID protocol — current VexArchitecture relationship map

[VXG RealForever]

> **CURRENT TOPOLOGY PROJECTION — PR #209 CANDIDATE**
>
> This document is a readable projection of the relationships currently implemented by PR #209.
> It is **not** a second architecture owner, runtime registry, wire protocol, or replacement for the
> owning source. If this map disagrees with current production source or an owning architecture
> contract, the map is stale and must be repaired.
>
> This file began as a #208 bridge design note. PR #209 has now made much of that exploration
> executable. Keep #209 as the before / pressure / subtraction / after **source-of-origin record** when
> the reason for a relationship matters; use the current owning source and tests for what the
> relationship does now.

## 1. Whole current topology

The current candidate has two different kinds of connectivity that meet through explicit live
objects: **construction/lifetime** and **semantic/runtime truth**.

### Android construction and lifetime

~~~text
Android process
└─ VexApplication
   └─ parent VexDependencyFactory
      ├─ Application
      ├─ UIBlueprintRegistry
      ├─ AppRepositories
      └─ ThemeModeStore
             application/process lifetime

Android configuration/session
└─ VexLifeViewModel
   └─ child VexDependencyFactory
      ├─ reuses parent-lifetime providers
      ├─ CoroutineScope
      ├─ VexExecutionRoot
      ├─ LogHistoryPersistenceQueue
      ├─ VexLogger
      ├─ ThemeModeFamily
      ├─ EventsConvergenceRelay
      ├─ VexConversationHybridRelay
      ├─ VexLifeRelayGraph        # projection only
      ├─ AppActionRegistrations
      ├─ VexRuntime
      ├─ VexLifeSession
      ├─ VexStreamStartupExecution
      └─ VexStreamArtworkExecution
~~~

VexApplication owns the process composition boundary. VexLifeViewModel owns the child
configuration/session composition boundary. Ordinary feature/UI code receives concrete objects; it
does not resolve arbitrary dependencies from either factory.

### Semantic / runtime causal body

~~~text
UI / runtime source
        |
        +-> optional Family.Parameter
        |   when a semantic value needs durable Family-scoped provenance
        |
        v
Family.Control
        |
        v
Family.activate(Control)
        |
        +-> concrete Family.act(exact CURRENT State, Control)
        |   -> Act?
        |      ├─ admitted State?
        |      └─ Begin?
        |         ├─ Effect
        |         └─ Approach
        |            Independent / Earliest / Latest / Queued
        |
        +-> bind live Family execution owner
        +-> bind maintained RelationshipId when Begin requires it
        +-> admit immediate semantic State when authored
        |
        v
accepted Act
        |
        v
Family.Act.Method
fully bound executable operation
        |
        v
VexExecution
RelationshipId -> actual Job(s)
        |
        v
Store / provider / platform reality
        |
        v
physical result / observation
        |
        v
rightful feature/provider interpretation
        |
        v
Family.observe(already-semantic State)
        |
        v
Family.State
        |
        v
Family.Projection
        |
        v
Family.DisplayState
        |
        v
concrete presentation meaning
        |
        v
Compose / platform manifestation
        |
        v
human experience
~~~

Family.act(...) is the pure semantic decision called by activate(...); it is not a later execution
stage. Family.Parameter is likewise not mandatory ceremony around every Control or Kotlin argument.
It is earned when a semantic value needs to remain recognizable across Family relationships.

The two sides meet through the explicit live objects built by the construction topology. Dependency
construction does not become Family semantics, and Family semantics does not become dependency
construction.

### Cross-cutting chronology

~~~text
UI/runtime fact ---------------------+
semantic Family boundary ------------+
runtime/provider boundary -----------+--> VexLogger chronology
navigation/presentation owner -------+
architecture diagnostic -------------+
                                      |
                                      +-> Logcat
                                      +-> LogHistory persistence
                                      +-> Debug Log History
                                      +-> later VexSync / VexBoard / server projection
~~~

Logger follows accepted/observed truth. It does not decide that truth.

### Multi-truth convergence

~~~text
complete independently-owned truth A
complete independently-owned truth B
complete independently-owned truth C
        |
        v
Relay / convergence owner
        |
        v
relationship truth
        |
        +-> optional bounded request
            through the rightful target Family Control
~~~

Relay/convergence is not another stage after Family Effect and does not execute foreign Effects
directly.

## 2. One question, one rightful coordinate

Do not solve scale by making one ID or container answer unrelated questions.

~~~text
CONSTRUCTION

Question:
    Which live object exists?
    What prerequisites built it?
    Which lifetime owns/reuses it?

Owner / coordinate:
    VexDependencyProvider declaration object identity
    + VexDependencyFactory lifetime


UI SOURCE

Question:
    Which registered or runtime-presented UI source acted?
    Which Screen / placement owns it?

Owner / coordinate:
    ScreenElement.screenId
    UIElement.elementId
    UIBlueprintRegistry structure
    explicit runtime element coordinate where the instance is dynamic


SEMANTIC MEANING

Question:
    What value/request/truth/consequence exists in this Family world?

Owner / coordinate:
    Family.Parameter
    Family.State
    Family.Control
    Family.Act
    Family.Effect


ACTIVE MAINTAINED WORK

Question:
    Which semantic/runtime relationship owns unfinished work?

Owner / coordinate:
    RelationshipId authored by the rightful semantic/runtime owner
    VexExecution custody:
        RelationshipId -> actual Job(s)


RUNTIME OCCURRENCE

Question:
    Which concrete execution occurrence is alive now?

Owner / coordinate:
    actual Kotlin Job


CHRONOLOGY

Question:
    What accepted/observed facts happened, and in what order?

Owner / coordinate:
    VexLogger chronology


HUMAN / DEBUG INTERPRETATION

Question:
    How should stable coordinates and chronology be explained here?

Owner:
    downstream projection using the matching source maps
    such as Debug Log History / later VexSync / VexBoard
~~~

Permanent distinction:

~~~text
DEPENDENCY IDENTITY
!= UI IDENTITY
!= FAMILY SEMANTIC MEANING
!= RELATIONSHIP IDENTITY
!= JOB OCCURRENCE
!= LOGGER CHRONOLOGY
!= HUMAN PROJECTION
~~~

A relationship between these coordinates may be explicit without collapsing them into one universal
ID.

## 3. ThemeMode patient zero — current source-origin example

ThemeMode is the concrete path that currently demonstrates the topology end to end.

~~~text
VexApplication lifetime
    ThemeModeStore

VexLifeViewModel/session lifetime
    ThemeModeFamily
    VexExecutionRoot
    VexLogger

Debug Menu interaction
-> ThemeModeFamily.Parameter.Mode(mode)
-> Control.Select(the SAME Parameter)
-> ThemeModeFamily.activateSelection(Control)
   -> establishes the concrete ThemeMode unsettled-selection fence
   -> Family.activate(Control)
      -> Family.act(exact CURRENT State, Control)
      -> Act?
         -> immediate State admission when required
         -> beginLatest(Effect.Save(the SAME Parameter))
      -> bind maintained Select RelationshipId
      -> bind live Family execution owner
-> accepted Act
-> Act.Method.Save(
       ThemeModeStore,
       the SAME Parameter
   )
-> Act.launchWith(VexExecutionRoot).method(Method.Save)
   consumes:
       live Family owner
       authored Latest Approach
       authored RelationshipId
-> VexExecution Job custody
-> ThemeModeStore.save(mode)
-> ThemeModeStore.Result / Store observation
-> ThemeMode-owned currentness interpretation
-> Family.observeStoredThemeMode(...)
-> current ThemeMode State
-> ThemeModeFamily.DisplayState
-> VexThemePresentation
-> VexLifeTheme
-> Compose / system-bar manifestation
~~~

The same Parameter.Mode object is deliberately preserved across Control -> Effect -> Method instead
of flattening the semantic value into loose arguments and reconstructing it later.

That same identity now also protects ThemeMode currentness while an accepted selection is unsettled:

~~~text
Select(Light / Parameter A)
-> State Light
-> Save(A)

Select(Dark / Parameter B)
-> State Dark
-> Save(B)

delayed Store Light from A
-> B remains unsettled
-> Light has no provider revision proving it is newer
-> hold Light

terminal Method result for B
-> settle B by object identity
-> ordinary Store observations resume
~~~

This is a concrete ThemeMode/provider rule. It does **not** establish a generic generation counter,
TraceId, pending-State hierarchy, execution-status registry, or ThemeMode runtime manager.

When a future reader asks why the current path has this shape rather than the removed alternatives,
PR #209 is the provenance record. Current source remains the behavioral authority.

## 4. Execution topology — custody without feature re-authoring

VexExecution supports two truthful authoring shapes.

### Existing owner + direct execution custody

When a Family/session/runtime owner already owns the meaning:

~~~text
existing owner
-> VexExecution.begin(...)
-> VexExecution.observe(...)
-> owner-scoped Job custody
~~~

The stable live owner object supplies execution scope. It is not a new semantic identity.

For a Family-authored maintained relationship:

~~~text
accepted Act
    Approach
    RelationshipId
    live Family owner
        |
        v
VexExecution
    RelationshipId -> Job(s)
~~~

Execution consumes the authored relationship instead of asking the feature to invent another
execution vocabulary.

### Genuine sustained execution owner

When a distinct object really owns a sustained body of source-addressed asynchronous work, it may
extend VexExecution.

Current examples include the session-owned VexStream execution objects and
VexMapsDistributionSession, whose domain/session verbs remain on the Session while VexExecution
provides custody.

Permanent:

~~~text
SOURCE-ADDRESSED LOCAL WORK
    exact callable / Flow source may be a narrow local address

MAINTAINED SEMANTIC RELATIONSHIP
    RelationshipId is the one maintained-work address

callable name / reflected type
    != RelationshipId
    != durable identity
~~~

VexExecutionRoot owns the shared supervised coroutine lifetime and terminal shutdown. Individual
execution owners use owner-scoped stop/join; one sibling does not terminate another merely because
they share a root.

There is no required public execution InstanceId, SourceRef, Handle, completed Snapshot registry,
activeExecutions duplicate truth, or second CoordinationKey identity.

## 5. Dependency topology — construction is readable source

The executable dependency law is now:

~~~text
provider declaration
-> explicit prerequisite provider declarations
-> lazy dependency-first resolution
-> one memoized Value per provider declaration per factory lifetime
-> explicit concrete object delivered to consumer
~~~

Provider **object identity** is the construction coordinate. Class/name is a readable label, not a
global key.

~~~text
VexApplication parent factory
    process-lifetime declarations

VexLifeViewModel child factory
    reuses parent providers
    owns session/configuration declarations
~~~

The ViewModel resolves the explicit startup roots that should start. Unrequested providers remain
lazy until part of a requested prerequisite closure.

The factory is a composition-root tool, not an ambient service locator. Providers may require only
prerequisites they declared. Ordinary feature/UI consumers receive concrete objects.

Permanent:

~~~text
CONSTRUCT EXPLICITLY
REUSE AT THE RIGHTFUL LIFETIME
NEVER GUESS FROM REFLECTION / PACKAGE / CLASS NAME
~~~

Dependency construction must not become Family policy, RelationshipId authoring, Logger chronology,
provider business meaning, or feature runtime ownership.

## 6. Logger + Blueprint topology — fact first, interpretation downstream

Current production Logger accepts facts from rightful owners.

~~~text
UI/runtime
    Interaction / DynamicInteraction / Gesture / CurrentScreen

semantic boundary
    ControlAccepted / StateAdmitted

runtime/provider boundary
    EffectRequested / EffectSucceeded / EffectFailed

architecture diagnostics
    BlueprintMappingMissing
~~~

Stable registered UI source coordinates remain compact:

~~~text
subjectRef = element.debug-menu.body.dark-mode
~~~

When a matching UIBlueprintRegistry is available:

~~~text
subjectRef
-> owner Screen
-> parent / placement path
-> element kind
-> declared UI intent
~~~

Those readable fields are derived at interpretation time rather than copied into every log record.

Two different element shapes remain intentional:

~~~text
STATIC BLUEPRINT ELEMENT
    registered elementId
    -> registry resolution
    -> RESOLVED

DYNAMIC RUNTIME INSTANCE
    stable runtime coordinate
    -> DynamicInteraction
    -> RUNTIME_ELEMENT
~~~

Missing expected static interpretation is observable as a diagnostic instead of silently becoming
"unknown".

Production chronology records what is current at the fact boundary. It does not serialize an
expected destination/result as though it already happened.

~~~text
DebugMenu Clicked
    current Screen = Home

navigation mutates

DebugMenu CurrentScreen
    current Screen = DebugMenu
~~~

Actual rendered presentation arrival remains a separate fact and is not claimed merely from
navigation or provider success.

## 7. Adjacent derived-identity exploration and the outward protocol edge

DERIVED-IDENTITY-MAP.md preserves useful #208 design pressure, but PR #209 does **not** yet establish
automatic host/action/content identity derivation as a general executable runtime law.

The adjacent exploration asks whether existing meaning may eventually be sufficient to derive some
coordinates without a second manual-ID ceremony:

~~~text
ACTION HOST
    Blueprint placement
    + Control
    + typed ResourceId when semantically required
    -> possible derivable action relationship

CONTENT HOST
    Blueprint placement
    + canonical native content source
    -> possible derivable content relationship

DYNAMIC PRODUCT INSTANCE
    Blueprint placement
    + feature-owned ResourceId
    -> possible runtime presentation coordinate
~~~

Treat those as design pressure to validate against real consumers, not as a claim that current #209
source already derives all three shapes. Current executable truth is narrower: Screen/element
coordinates and Blueprint structure exist; Logger can record/resolve those coordinates; dynamic
runtime interactions remain explicitly distinguished.

For future VexSync/server/VexBoard interpretation, the unresolved question is mainly **matching-map
currentness/provenance**, not another execution occurrence identity:

~~~text
stable app coordinate
+ matching Blueprint/application map
-> Screen/path/kind/symbolic meaning
-> localized / human-readable projection
~~~

The exact cross-version transport/currentness representation remains held until the existing
VexSync/Blueprint contracts provide enough evidence.

## 8. Topology as a drift-review lens

This map is useful only if it helps a future human or AI recognize duplicated ownership without
turning the map into another framework.

When new or clunky architecture appears, ask:

~~~text
1. What current topology node or relationship does this source represent?

2. Does it own a genuinely new responsibility,
   or does it merely restate an existing owner's verbs/identity/lifetime?

3. Is a value being flattened and then reconstructed even though its semantic owner already carries it?

4. Is a new ID answering a question already answered by:
       provider declaration identity
       Blueprint coordinates
       Family semantic structure
       RelationshipId
       actual Job
       Logger chronology?

5. Is execution acquiring feature/domain vocabulary that belongs to an existing Family/Session?

6. Is dependency construction becoming a semantic/runtime owner?

7. Is Logger being asked to predict an expected outcome instead of record an accepted/observed fact?

8. Is a projection becoming a new truth owner rather than reading the canonical owner?

9. Is a wrapper needed for correctness/lifetime/ownership,
   or only because conventional layering makes the source look familiar?
~~~

If removing a wrapper, ID, registry, bridge, or duplicate projection loses no required correctness,
lifetime, ownership, currentness, or proof, subtraction is the preferred pressure.

Different Kotlin shapes are allowed when they preserve different meanings. The topology is a
relationship test, not a demand that every feature have the same classes.

## 9. Canonical owners and provenance

Use this map to navigate, then descend to the owner.

~~~text
Family semantics
    architecture/behavior/FAMILY-CIRCUIT.md
    architecture/behavior/.../VexFamily.kt
    focused Family tests

Execution custody
    architecture/execution/README.md
    architecture/execution/.../VexExecution.kt
    VexExecutionRoot.kt
    focused execution tests

Chronological traceability
    architecture/vexarchitecture/VEXLOGGER.md
    architecture/logging/README.md
    architecture/logging/.../VexLogger.kt
    focused Logger tests

Construction topology
    architecture/dependency/README.md
    architecture/dependency/.../VexDependencyFactory.kt
    VexApplication.kt
    VexLifeSession.kt
    dependency/session tests

Whole semantic/display body
    docs/foundation/VEX-BODY-SEMANTIC-BLUEPRINT.md

Relay / convergence
    architecture/relay/README.md
    exact concrete Relay source/tests

UI identity / structural interpretation
    Elements / Blueprint owning source
    UIBlueprintRegistry
    exact Screen Blueprint source/tests

WHY / rejected alternatives / causal origin
    PR #209 before / pressure / subtraction / after record
    #207/#208 remain upstream provenance only
~~~

Permanent:

~~~text
TOPOLOGY MAP = READABLE PROJECTION / NAVIGATION

TOPOLOGY MAP
!= ROOT AUTHORITY
!= RUNTIME REGISTRY
!= SECOND SOURCE OF TRUTH
!= UNIVERSAL ID SCHEMA
~~~

## 10. Deliberately held edges

Current #209 source does **not** establish:

~~~text
full exact-head Gradle/JVM PASS for the current candidate
Independent Assurance / merge readiness / VettedWithHuman / MASTERED
a universal machine-readable topology registry
a mandatory topology annotation on every class
a universal Family runtime wrapper
a universal Store interface across every Family
a generic result -> Family State grammar
a generic generation/revision/TraceId currentness system
a CompoundParameter / ConvergenceMethod / RelationshipFamily
a final VexSync cross-version identity-map protocol
actual rendered presentation arrival where no presentation owner observes it
legacy Room derived columns as Logger architecture law
~~~

Additional real-consumer pressure remains useful for:

~~~text
one multi-field Family Parameter / Method
one genuine zero-input Family Method
a second Method inside one Family
an existing Convergence Relay
~~~

Those are pressure points, not permission to pre-author new abstractions.

## 11. Historical comparison lens: ordinary DI versus readable topology

The #208 bridge used Koin as comparison pressure, not architecture authority.

Ordinary DI and the current Vex construction kernel overlap mechanically:

~~~text
definitions/providers
-> prerequisites
-> lifetime / scoped reuse
-> object
-> consumer
~~~

The additional Vex design goal is narrower:

> A human or fresh instance should be able to recover why an object exists, what it depends on, who
> owns/reuses it, and where it is consumed by reading the relationship declarations without mentally
> executing a hidden container.

That does not imply "Koin is bad", nor that Vex should reproduce every DI capability. It explains why
the dependency graph is intentionally readable source while semantic identity, UI identity, active
execution identity and chronology remain with their separate owners.

<!-- [VXG RealForever][TOPOLOGY][PR209-CURRENT-PROJECTION][ID-PROTOCOL][VEXLOGGER] -->
