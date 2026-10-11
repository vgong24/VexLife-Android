# Events Attention Practicum — Convergence and Reverse-Protocol Map

[VXG RealForever]

## The question

> Can one mastered Android node prove how information arriving outside the current Screen becomes
> truthful, addressable, composable human attention — then let future real services reverse-build
> only the adapter/protocol pieces they actually own?

This practicum deliberately starts from the **finished human-facing convergence** rather than
inventing Messaging, Calls, Maps, Health or other service APIs independently.

The current synthetic provider is a fixture. The convergence contract is the reusable result.

## One lattice node

~~~text
external provider / fixture
  owns transport + provider-native truth
        |
        | normalized source snapshot
        v
EventsRepository
  one current normalized feed
  per-source revision admission
        |
        +--------------------------+
        |                          |
        v                          v
stable dynamic rows        EventsAttentionPreferenceRepository
eventRef identity          ack / defer / suppress
        |                          |
        +------------+-------------+
                     |
current Screen ------+
visible target set --+
transient occupied --+
                     |
                     v
            EventsAttentionRelay
              processStates(...)
                     |
                     v
       candidate identity + availability
         UNOBSERVED
         NONE
         HELD_OTHER_SCREEN
         HELD_TARGET_UNREACHABLE
         HELD_TRANSIENT_OCCUPIED
         READY
                     |
                     v
           VexAnchoredTooltip
       pure placement / renderer
                     |
                     v
      stable dynamic interaction ref
                     |
                     v
          VexRuntime / VexLogger
                     |
                     v
         Journey + ScreenCrawler
~~~

The purpose is not to make every service look like Events internally. It is to establish one narrow
boundary where independently owned service meaning can become chronological human-awareness input.

## Current source map

~~~text
architecture/relay/
  VexRelay
  VexCompoundRelay
  VexState / VexCompoundState

app/.../data/events/
  VexExternalEvent
  VexExternalEventSourceSnapshot
  EventsFeedState
  EventsRepository / RelayEventsRepository
  EventsAttentionPreferences
  SyntheticExternalEventSource
  SyntheticExternalEventController

app/.../screens/primary/events/
  EventsScreen
  EventsPresentationRefs
  EventsAttentionRelay

architecture/elements/
  VexStableLazyColumn
  VexAnchoredTooltip

testing/journey/catalog/primary/events/
  EventsJourneys
~~~

The shortest debugging route is:

~~~text
"What did this provider say?"
-> EventsRepository / source snapshot

"Why is this event the candidate?"
-> EventsAttentionRelay.processStates(...)

"Why is the tooltip held?"
-> EventsAttentionOutputState.availability

"Where did the tooltip render?"
-> resolveVexAnchoredTooltip(...)

"What exact row/action did the proof address?"
-> EventsPresentationRefs + Journeys.events.externalAttention
~~~

## Normalized provider boundary

A provider projects one **current bounded source snapshot**:

~~~text
sourceRef
sourceRevision
events[]
  eventRef
  sourceRef
  kind
  title
  detail
  occurredAtEpochMillis
~~~

Current invariants:

~~~text
SOURCE_REVISION_IS_SCOPED_TO_SOURCE_REF
MESSAGING_REVISION_12 != MAPS_REVISION_12

OLDER_SOURCE_REVISION
-> cannot overwrite newer retained source truth

SAME_SOURCE_REVISION + DIFFERENT_CONTENT
-> contract failure

EVENT_REF
-> globally stable across current normalized feed

EVENT_POSITION
-> presentation metadata only
-> never semantic identity
~~~

A source snapshot is not required to be the provider's complete database/history. It is the bounded
current event projection that provider chooses to expose to Events. A later snapshot may add,
replace or remove entries according to the real provider contract while preserving source revision
truth.

## What a future real service must reverse-build

Do **not** start by copying the synthetic source.

For a real service, answer these questions from its actual domain:

1. **What owns the source truth?**
   Messaging repository, telephony adapter, maps/route owner, VexStream runtime, health provider, etc.

2. **What is one stable source identity?**
   Form a stable `sourceRef` for that service/account/device relationship.

3. **What makes a source snapshot newer?**
   Map the service's actual sequence/version/currentness semantics into a monotonically comparable
   `sourceRevision` *inside that source only*. Do not invent a global app revision.

4. **What is one stable human-awareness event?**
   Derive `eventRef` from service-owned identity, never list position or arrival slot.

5. **What bounded display projection is safe/useful?**
   Map only the information Events needs for chronological human awareness. Events does not acquire
   the provider's whole domain model.

6. **What happens when the service reconnects/restarts?**
   Preserve or re-establish source revision/currentness honestly. Do not replay an older snapshot as
   newer merely because the Android adapter restarted.

7. **What actions remain with the service?**
   Replying to a message, answering a call, changing a route, controlling playback, or acknowledging
   a medical alert remains a domain action owned by that service. Events presentation does not gain
   that authority automatically.

Target shape:

~~~text
RealService
  canonical service truth
        |
RealServiceEventsAdapter
  translate current service meaning only
        |
VexExternalEventSourceSnapshot
        |
same Events convergence node
~~~

A second real adapter may eventually justify a reusable adapter interface. The first practicum does
not create one speculatively.

## Truth versus attention preference

~~~text
EVENT_HAPPENED
!=
TOOLTIP_SHOULD_SHOW_NOW

ACKNOWLEDGED
!= EVENT_DELETED

DEFERRED
!= EVENT_FALSE

SUPPRESSED
!= MEMORY
~~~

`EventsRepository` owns the normalized current event projection.

`EventsAttentionPreferenceRepository` separately owns session presentation preference:

~~~text
ACKNOWLEDGED
DEFERRED
SUPPRESSED
~~~

The Compound Relay consumes both. A preference can change proactive presentation without rewriting
event truth.

## Candidate identity versus presentation availability

Candidate selection happens **before** target reachability.

The current policy first retains the already-selected candidate while that exact event still exists
in the provider projection and remains unhandled. This means insertion, reorder, timestamp change,
or a newer arrival cannot steal the active attention identity.

When no retained candidate remains, the Relay chooses the oldest unhandled event from normalized
event truth. A provider removal of the retained event is therefore allowed to advance the candidate.

Then availability is classified:

~~~text
UNOBSERVED
  required relationship input has not been observed yet

NONE
  dependencies are observed and no unhandled candidate exists

HELD_OTHER_SCREEN
  candidate exists but Events is not current

HELD_TARGET_UNREACHABLE
  same candidate exists but its stable row is not currently visible

HELD_TRANSIENT_OCCUPIED
  same candidate exists but another transient owns presentation attention

READY
  exact candidate + exact visible target may render
~~~

Therefore:

~~~text
SCROLL != NEW_CANDIDATE_IDENTITY
INSERT != NEW_CANDIDATE_IDENTITY
REORDER != NEW_CANDIDATE_IDENTITY
PROVIDER_REMOVAL_MAY_ADVANCE_CANDIDATE
TARGET_UNREACHABLE != EVENT_DISAPPEARED
TRANSIENT_BLOCKED != EVENT_REJECTED
~~~

## Stable dynamic-list component boundary

Events is the first real consumer of a reusable Android-native list primitive:

~~~text
VexStableLazyColumn
  stable itemRef -> Lazy list key
  optional contentType -> compatible Compose reuse family
  listRef -> stable list test tag
  visible item refs -> bounded current visibility projection
  caller-owned row Composable
~~~

It deliberately does **not** know:

~~~text
EventsRepository
EventsAttentionRelay
event kind/title/detail
ack/defer/suppress
tooltip policy
provider ordering semantics
~~~

Events supplies its own private `EventRow` Composable. That is product meaning and stays beside the
Screen.

This separation answers two different developer questions:

~~~text
"How does a dynamic Android list preserve repeated identity and expose what is visible?"
-> VexStableLazyColumn

"What does one Events row look like and what can the human do with it?"
-> EventRow in EventsScreen.kt
~~~

The list primitive fails closed on blank/duplicate item refs. Stable string keys are also valid
Android Bundle-saveable lazy-list keys, so Compose can preserve item state across insertion/reorder
instead of falling back to visual position.

## Stable repeated presentation identity

Current rows use:

~~~text
eventRef
-> LazyColumn key

eventRef
-> EventsPresentationRefs.row(eventRef)

eventRef + acknowledge/later/suppress
-> dynamic action elementRef

eventRef
-> tooltip presentationRef
~~~

The static AppScreens registry does not enumerate every possible future message/call/map row.

~~~text
RUNTIME_DATA_ITEM != STATIC_SCREEN_REGISTRY_MEMBER
DYNAMIC_ELEMENT_REF != LIST_INDEX
~~~

Dynamic interactions still enter the ordinary VexRuntime/VexLogger semantic history through their
stable formed refs.

## Geometry is not arbitration

`EventsAttentionRelay` answers:

> should this exact candidate be presentable now?

`resolveVexAnchoredTooltip` answers:

> given the current anchor/window/tooltip geometry, where can it fit?

Current placement family:

~~~text
ABOVE
BELOW
START
END
FALLBACK
~~~

START/END respect layout direction. A bottom/edge anchor may reorient without changing `eventRef`.
If no preferred anchored side safely fits, the resolver emits explicit FALLBACK rather than claiming
an unsafe preferred placement.

~~~text
PRESENTATION_ELIGIBILITY != GEOMETRY
GEOMETRY_CHANGE != SEMANTIC_EVENT_CHANGE
AUTOMATIC_REPOSITION != HUMAN_PREFERENCE
~~~

## Lived attention contract

The attention popup is an **interactive anchored rich tooltip / popover**, not another Events row.

A lived-device review exposed two requirements that are now part of the practicum:

~~~text
TRANSIENT_SURFACE_MUST_LOOK_TRANSIENT
PROVIDER_UPDATE != VISIBILITY_OBSERVER_RESTART
~~~

The tooltip therefore uses an inverse/outlined surface rather than the ordinary event-card surface.

The second lived review showed that keeping the visibility observer alive was necessary but not
sufficient: a five-second provider emission still legitimately changes the Compound Relay input and
the live Events list order. Even when candidate identity is unchanged, collecting the whole Compound
state and relaying out rows under an anchored Popup can create visible flicker.

Current anti-flicker ownership is therefore:

~~~text
VexCompoundRelay.states
  retained input may change every provider emission

EventsAttentionRelay.outputStates
  map to derived EventsAttentionOutputState
  distinctUntilChanged()
  presentation observes this projection, not every compound revision

EventsScreen presentation snapshot
  while the same candidate is active:
    provider truth continues advancing
    queue counts continue advancing
    displayed row ordering stays stable
  candidate changes:
    refresh displayed snapshot from current provider truth
~~~

This means:

~~~text
CANONICAL_PROVIDER_UPDATE
!=
MOVE_THE_ANCHORED_ROW_UNDER_THE_HUMAN
~~~

A blocking lab scenario adds one stronger condition on top of the common presentation snapshot:
the target is auto-scrolled into view, user scrolling is disabled, outside dismissal is ignored and
Acknowledge releases the lock.

The rich tooltip is intentionally compact. The durable/Debug surfaces retain deep metadata; the
popover shows only the information needed for the current human decision:

~~~text
event title
active/current/next queue counts
scenario step/target/placement when present
random replay seed when present
one expected press
one short expected result
relevant action button(s)
~~~

Compose does not recompute queue semantics. If the compact metadata is wrong, the Relay relationship
is wrong.

### Button outcomes are executable expectations

~~~text
ACKNOWLEDGE
  event row remains
  terminal proactive handling
  attention advances

LATER
  event row remains
  moves to NEXT sequence in human defer order
  current queue drains first

SUSPEND
  event row remains
  stays in CURRENT sequence
  moves to the current-queue tail in human suspend order

SUPPRESS
  event row remains
  terminal proactive suppression for this lab/session
~~~

At this practicum stage, Later means the next modeled sequence, not a wall-clock snooze. If repeated
deferral eventually needs next-next sequence depth or time-based re-entry, a real consumer must earn
that distinction.

If any stated next state above does not occur, preserve it as a failing lived case / bug rather than
reinterpreting the button after the fact.

### Repeatable lab reset

`Reset lab`:

~~~text
stop timed producer
-> publish an empty current source projection at a newer sourceRevision
-> clear attention dispositions
-> preserve monotonic provider/event identity for later emissions
~~~

This gives a human a reproducible clean starting point without teaching the mock provider that reset
means returning to revision 1.

## Synthetic fixture and live condition lab

The Debug Menu owns a session-scoped deterministic fixture. All controls operate the **same**
SyntheticExternalEventSource and normalized EventsRepository ingress; there is no second fake test
architecture.

| Control | Condition it is meant to provoke |
|---|---|
| Start 5-second feed | information arriving asynchronously while another Screen is current |
| Stop feed | producer cancellation / no further admission while stopped |
| Emit next | immediate one-event cause/effect without waiting for wall clock |
| Emit 12-event burst | enough stable rows to exercise lazy virtualization, scrolling and target reachability |
| Insert older event | provider insertion/reorder pressure without changing existing eventRefs |
| Remove oldest | provider removal of a current event so retained-candidate advancement can be observed |

Useful live play:

~~~text
Debug Menu
-> Emit 12-event burst
-> Events
-> note the current tooltip candidate
-> scroll that row off-screen
   => same candidate becomes HELD_TARGET_UNREACHABLE
-> scroll it back
   => same candidate becomes READY again
-> open another transient
   => same candidate becomes HELD_TRANSIENT_OCCUPIED
-> dismiss transient
   => same candidate becomes READY
-> Insert older event
   => existing candidate does not get stolen by reorder
-> Remove oldest
   => if that removes the retained candidate, Relay advances to the next unhandled event
~~~

The source cycles through message-like, call-like, map-like and media-like information. Those labels
are **fixtures only** and do not claim those real integrations exist.

Stop/restart reuses the same synthetic source instance so its provider revision continues rather than
restarting at 1 over newer retained truth. An immediate Start while cancellation is still completing
cannot create a second producer loop.

Normal production composition does not silently auto-start the fixture.

### Android Studio visual play

Events keeps two feature-owned previews beside `EventsScreen.kt`:

~~~text
EventsScreenPreview
  empty/default state

EventsAttentionPreview
  populated message/call/map rows
  attention candidate active
  rendered through all @VexScreenPreviews device/theme/font forms
~~~

The preview fixture uses the real Events repository contract and real Screen content. It does not add
Preview-only behavior to VexRuntime or create another scenario registry.

### Test-time customizability

The same fixture is parameterized where tests actually need control:

~~~text
intervalMillis
clock
maxEvents
sourceFactory
emitNext()
emitBurst(count)
insertHistorical()
removeOldest()
~~~

Journey uses deterministic `signal(...)` snapshots instead of wall-clock sleeps, so causal
instrumentation remains fast/repeatable while the Debug Menu preserves a human live-play path.

## Attention expectation scenario lab

The Debug fixture now owns a separate scenario planner. This is intentionally **not** external event
truth and not part of a real service adapter.

~~~text
SyntheticExternalEventSource
  -> produces external facts

EventsAttentionRelay
  -> owns queue + candidate + availability

EventsAttentionLabRepository
  -> chooses which edge condition the human is trying to prove next
~~~

Two modes are available:

~~~text
ROTATION
  stable five-scenario cycle

SEEDED_RANDOM
  chooses from the same scenario templates
  displayed seed + scenarioRef make the run reproducible
~~~

Current deterministic rotation:

~~~text
1. ANCHORED_TOP
   target = row 1
   preferred placement = BELOW
   expected action = ACKNOWLEDGE

2. ANCHORED_MIDDLE
   target = middle row
   preferred placement = ABOVE
   expected action = LATER

3. ANCHORED_BOTTOM
   target = final row
   preferred placement = ABOVE
   expected action = SUSPEND

4. ANCHORED_MIDDLE
   target = middle row
   preferred placement = BELOW
   expected action = SUPPRESS

5. BLOCKING_FREEZE_TOP
   target = row 1
   preferred placement = BELOW
   expected action = ACKNOWLEDGE
   displayed list projection freezes
   external provider truth continues updating
   outside/back dismissal is ignored
   acknowledge releases the frozen projection
~~~

The planner stores the exact target eventRef, plan-time row position, list count, seed, ordinal,
preferred placement, expected action and scenario ref. Row position is test expectation metadata;
the attention relationship still addresses the event by eventRef.

~~~text
SCENARIO_POSITION = TEST_EXPECTATION_METADATA
SCENARIO_TARGET = STABLE_EVENT_REF
VISUAL_INDEX != SEMANTIC_IDENTITY
~~~

VexStableLazyColumn(requestedVisibleItemRef=...) brings a planned stable item into the viewport.
VexAnchoredTooltip(preferredPlacements=...) tries the scenario's preferred side first and may use
a documented safe fallback when the requested side does not fit.

### Queue action law

The lived buttons now exercise different queue semantics rather than three versions of 'not now':

~~~text
ACKNOWLEDGE
  terminal handled for proactive attention
  row remains canonical event truth

SUPPRESS
  terminal proactive suppression for this lab/session
  row remains canonical event truth

SUSPEND
  remain in CURRENT sequence
  move behind unsuspended current work
  multiple suspends preserve human action order

LATER
  move into NEXT sequence
  deferred items become eligible only after current queue drains
  multiple Later actions preserve human action order
~~~

The Relay exposes:

~~~text
currentQueueCount
nextSequenceCount
queuedAfterCurrentCount
acknowledgedCount
deferredCount
suspendedCount
suppressedCount
~~~

These values are rendered directly in the attention popup. Compose does not maintain a second queue.

One current limitation is deliberate and visible: if an item is already in the deferred next
sequence and the human presses Later on that same item again after the next sequence becomes active,
there is not yet a modeled next-next sequence. A real recurrence requirement should earn that
additional sequence depth instead of silently inventing it.

### Blocking freeze

The blocking scenario proves a stronger distinction:

~~~text
CANONICAL_PROVIDER_TRUTH_CONTINUES
!=
DISPLAYED_EVENTS_PROJECTION_MUST_UPDATE_NOW
~~~

On scenario entry, Events captures the currently displayed row projection. The synthetic provider
continues admitting newer snapshots underneath it. The blocking popup remains anchored to the exact
planned event and exposes queue metadata while the visible list stays frozen.

Acknowledge:

~~~text
mark blocking event ACKNOWLEDGED
-> complete current lab scenario
-> release frozen displayed projection
-> reveal provider events accumulated while blocked
-> normal attention processing may then continue
~~~

The timed source is source-lock gated so Reset can disable timed emission before publishing an empty
projection. A waking five-second tick cannot legally repopulate the lab after Reset.

### Human failure rule

The attention popup displays the expected action and expected result contract. For a lab run:

~~~text
EXPECTED_NEXT_STATE != OBSERVED_NEXT_STATE
-> FAILING_LIVED_CASE / BUG
~~~

Do not reinterpret a mismatch as a different button meaning after the fact. Preserve the scenario
ref, random seed when present, event ref, queue metadata and screenshot so the exact condition can be
replayed.
## Executable causal walk

Human/IDE path:

~~~text
Journeys.events.externalAttention
~~~

The journey uses two distinct kinds of step:

~~~text
signal(...)
  deterministic outside-world cause
  no fake human click
  no semantic VexLogger interaction by itself

tapDynamic(...)
  click a real runtime-data action by its production-formed stable elementRef
~~~

Current story:

~~~text
Home
-> external message snapshot arrives
-> Events
-> acknowledge message

Home
-> external call snapshot arrives
-> Events
-> Later

Home
-> external map snapshot arrives
-> Events
-> Suppress
~~~

The same declaration is consumable by Android instrumentation and ScreenCrawler.

Useful ScreenCrawler selections:

~~~kotlin
journey(Journeys.events.externalAttention)

screens(EventsScreen)
~~~

This is the intended causal evidence route for font/locale/geometry pressure. It is not a separate
screenshot-only scenario.

## Proof state

Current draft candidate: GitHub PR #15.

Current pure-domain checkpoint:

~~~text
local Kotlin/coroutines compile + semantic smoke = PASS
  - stale source revision rejection
  - bounded normalized synthetic projection
  - candidate HELD/READY progression
  - concurrent sibling dependency patch composition
  - single-producer stop/restart revision continuity
  - candidate retention across provider insert/reorder
  - provider removal advances the retained candidate
~~~

This is a pure-domain reproduction using the current source contracts with a tiny
`EventsScreen.screenId` stub because the local container does not carry the Android/Compose
dependency graph. It is evidence for the Relay/data law only; it is not relabeled as repository
Gradle or Android UI proof.

Exact mutable candidate SHA belongs in the PR/checkpoint surface rather than this architecture
document, so a documentation-only commit cannot make its own proof reference stale.

Not yet truthfully claimed:

~~~text
repository Gradle :architecture:relay:test PASS
app JVM/assemble PASS on exact current candidate
connected Android Journey PASS
ScreenCrawler screenshot return PASS for Events lattice
physical-device lived tooltip/reorder/scroll review
~~~

This environment cannot resolve github.com from its execution container, so it cannot clean-clone
the exact remote branch for repository Gradle proof. Source mutation and provider inspection use the
connected GitHub surface; that is not relabeled as Gradle/device execution proof.

## Completion direction

The practicum is architecturally complete enough to reverse-engineer service requirements **only
after exact Android proof establishes the current candidate**.

After that proof, future service work should descend from this map in reverse:

~~~text
desired human event row/attention
-> required normalized event identity
-> required source snapshot/currentness
-> provider-specific adapter semantics
-> real service repository/transport/auth/lifecycle
~~~

Do not make Events drive provider architecture that the provider's real truth cannot support.
Instead, preserve the convergence contract and refine the adapter boundary when a real service
reveals a missing distinction.

<!-- [VXG RealForever][VEXLIFE-ANDROID][EVENTS-ATTENTION-PRACTICUM][REVERSE-PROTOCOL] -->
