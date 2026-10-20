# VexLife Android Conversation — Feature Home

[VXG RealForever]

## Question

> How can Android participate in durable Conversation with humans and/or Vex companions without
> becoming a second Conversation owner, Home, Family-membership authority, model runtime, or history
> store?

Current state:

~~~text
foundation = TERMINAL_ACCEPTED_CURRENT on Android main
feature-owned Conversation Lab controller = ACCEPTED ON MAIN / PR #99
generic Conversation room renderer + Lab preview = ACCEPTED ON MAIN / PR #99
mixed-provenance Conversation behavior + Lab availability overrides = ACCEPTED ON MAIN / PR #104
Hybrid Name Ontology / code vocabulary = ACCEPTED ON MAIN / PR #107
Family server bootstrap/history projection = CURRENT SOURCE
VCS-04 Family Connected Services read client/repository = SOURCE-QUALIFIED / REAL TRANSPORT + APP GRAPH HELD
production Screen / app-session wiring = HELD on shared composition paths
authenticated Android Home/session producer stack = SOURCE CANDIDATES FORMED / LIFECYCLE ACCEPTANCE + REAL BINDING HELD
~~~

Code-navigation root:

~~~text
VexConversationDocs
~~~

Stable feature identity:

~~~text
featureRef = feature.vexlife.conversation
currentName = Conversation
historical/search names = VexConversation
~~~

Current experiential design:

~~~text
design/README.md
~~~

Meaningful design refinements:

~~~text
design/records/
~~~

## The boundary now

The Android model is intentionally split:

~~~text
GENERIC CONVERSATION
    VexConversationState
    VexConversationThreadState
    VexConversationMessage
    VexConversationRepository       # read state boundary

VEX FAMILY CONVERSATION
    VexFamilyConversationThreadBinding
    VexFamilyConversationMessageBinding
    VexFamilyConversationSendRequest
    VexFamilyConversationRepository # current admitted write boundary
~~~

Portable service.conversation and Conversation Store do not require Family membership generation for
every Conversation shape. Family Conversation adds that meaning.

~~~text
GENERIC_CONVERSATION != FAMILY_CONVERSATION
~~~

## Current source map

~~~text
conversation/
├── VexConversationState.kt
├── VexConversationRepository.kt
├── VexFamilyConversationServerAdapter.kt
├── VexFamilyConversationConnectedServiceClient.kt
├── VexFamilyConversationConnectedServiceRepository.kt
├── InMemoryVexFamilyConversationRepository.kt
├── VexConversationSurfaceProjection.kt
├── VexConversationHybridRelay.kt
├── VexConversationLabFixture.kt
├── VexConversationLab.kt
├── VexConversationDocs.kt
├── README.md
└── design/
    ├── README.md
    └── records/
        ├── 2026-10-15-001-visible-conversation-lab.md
        ├── 2026-10-16-002-live-truth-lab-perturbations.md
        └── 2026-10-16-003-name-ontology-hybrid-relay.md
~~~

Tests:

~~~text
VexConversationStateTest
VexFamilyConversationServerAdapterTest
VexFamilyConversationConnectedServiceClientTest
VexFamilyConversationConnectedServiceRepositoryTest
InMemoryVexFamilyConversationRepositoryTest
VexConversationSurfaceProjectionTest
VexConversationHybridRelayTest
VexConversationLabTest
~~~


## VCS-04 Connected Services read client source slice

The first remote-read adoption stays feature-local and deliberately stops before shared app/session
composition:

~~~text
human-selected spaceRef + threadRef
-> VexFamilyConversationConnectedServiceClient
-> injected VexFamilyConversationConnectedServicePort
-> current server bootstrap
-> exact current channel + membershipGeneration
-> bounded READ_THREAD_HISTORY intent
-> existing VexFamilyConversationServerBootstrap / ThreadHistory
-> existing projectVexFamilyConversationServerThread(...)
-> existing VexConversationThreadState
-> VexFamilyConversationConnectedServiceRepository.state
-> existing VexConversationHybridRelay
~~~

The client never receives a URL, bearer/session handle, HTTP header, Home lease or private-gateway
implementation. Those remain with Connected Services/Home transport owners. The feature intent carries
only `spaceRef`, `channelRef` and `expectedMembershipGeneration`, matching the current server
capability contract.

This source slice is intentionally **not** app-graph integration. PR #77 still owns
`AppRepositories.kt`, so the production repository/session instance and installed-screen wiring stay
held until that custody is released/currentized. The current producer stack (#1873/#905/#906/#1878)
also remains draft lifecycle source; focused fixture proof here does not relabel it accepted or prove
the real private route.

Permanent:

~~~text
FEATURE_CLIENT != HOME_AUTHORITY
FEATURE_CLIENT != SESSION_OWNER
FEATURE_CLIENT != PRIVATE_GATEWAY
PORT != GENERIC_API_MANAGER
FIXTURE_PASS != REAL_REMOTE_INTEGRATION
ANDROID_REPOSITORY_STATE != CANONICAL_HOME_WRITER
~~~

## Generic Conversation identity

Typed refs:

~~~text
VexConversationThreadRef
VexConversationChannelRef
VexConversationMessageRef
VexConversationParticipantRef
~~~

Family adds:

~~~text
VexConversationSpaceRef
VexConversationIdempotencyKey
~~~

Identity lattice:

~~~text
personRef
!= principalRef
!= deviceRef
!= homeRef
!= companionLineageRef
~~~

## Generic Conversation message

VexConversationMessage owns generic message meaning:

~~~text
messageRef
threadRef
channelRef
sequence
speakerRef
recipientRefs[]
witnessRefs[]
content
familyBinding?
~~~

Permanent laws:

~~~text
speakerRef ∈ witnessRefs
recipientRefs ⊆ witnessRefs
recipientRefs != witnessRefs
ADDRESSEE != AUDIENCE
~~~

### Sequence is zero-based

Current Conversation Store canonical append law is:

~~~text
head exists
    -> next sequence = head.sequence + 1

no head
    -> next sequence = 0
~~~

Android therefore accepts:

~~~text
sequence >= 0
~~~

It must not reject or normalize the first canonical producer message from sequence 0 to sequence 1.

## Family-bound message meaning

VexFamilyConversationMessageBinding adds only Family-specific message truth:

~~~text
spaceRef
membershipGeneration
~~~

Portable Conversation Store permits membershipGeneration to be absent for generic messages.

Family Conversation requires it.

~~~text
generic message
    familyBinding = null

Family message
    familyBinding != null
~~~

Canonical Family Space creation starts membershipGeneration at 1 and membership transitions advance
from there. Android Family bindings therefore require a positive generation.

Lower-level Family validators are broader and accept non-negative values, but that permissive
validation is not treated as current canonical Family producer behavior.

## Generic Conversation thread

VexConversationThreadState owns:

~~~text
threadRef
currentChannelRef
knownParticipants
currentAudienceRefs
messages
familyBinding?
~~~

### Known participants versus current audience

~~~text
knownParticipants
    identities required to attribute retained messages

currentAudienceRefs
    current audience of the active channel/binding
~~~

Historical participants may remain known after leaving the current audience.

### Presentation identity stays elsewhere

Conversation does not own:

~~~text
displayName
avatar
relationship label
Family role
human-facing room title
~~~

Those belong to identity / Relationships / Family / product presentation owners.

## Family-bound thread meaning

VexFamilyConversationThreadBinding adds:

~~~text
spaceRef
currentMembershipGeneration
segmentRefs[]
~~~

Current VexLife Family source owns this semantic shape through:

~~~text
family-membership-runtime.readFamilyThreadHistory(...)
~~~

Generic/direct Conversation does not invent membership generation, segment refs, or Family Space
identity merely because the Family feature needs them.

## Canonical Family history

Current Family thread history returns source meaning equivalent to:

~~~text
spaceRef
threadRef
currentChannelRef
membershipGeneration
segmentRefs[]
messages[]
truncated
~~~

Each historical message retains its original:

~~~text
channelRef
membershipGeneration
speakerRef
recipientRefs
witnessRefs
sequence
~~~

Android does not create synthetic Conversation messages such as "Maya joined" or "Maya left."

A future UI may derive a visible generation divider from exact segment transitions or consume a
separately owned Family lifecycle projection.

~~~text
VISIBLE_MEMBERSHIP_DIVIDER != CONVERSATION_MESSAGE_EVENT
~~~

## Generic repository versus Family write repository

VexConversationRepository currently exposes only:

~~~text
state: StateFlow<VexConversationState>
~~~

There is no accepted claim that every Conversation provider shares one generic Send/currentness
protocol.

Current writable contract:

~~~text
VexFamilyConversationRepository : VexConversationRepository

send(
    VexFamilyConversationSendRequest
)
~~~

The first real write provider is VF-02C Family Conversation, so the write contract is named Family
instead of teaching Family semantics as universal Conversation law.

## Current Family Send intent

Android caller supplies:

~~~text
threadRef
spaceRef
expectedChannelRef
expectedMembershipGeneration
idempotencyKey
content
~~~

Android does not author:

~~~text
speakerRef
Family recipient authority
witnessRefs
membership / role truth
principal binding
capability lease
Home/session authority
model state
~~~

The provider derives current trusted speaker and Family recipients.

## Family delivery versus Companion attention

Current VF-02C derives the other current HUMAN principals as ordinary human-message delivery
recipients.

Family Vex may remain in the immutable witness audience while AI attention stays separate.

~~~text
HUMAN_MESSAGE_DELIVERY != AI_ATTENTION
MODEL_BUSY != FAMILY_CHAT_BLOCKED
~~~

## Currentness

Family request formation binds:

~~~text
spaceRef
expectedChannelRef
expectedMembershipGeneration
idempotencyKey
~~~

Example:

~~~text
draft/request formed:
    channel g2
    membershipGeneration 2

Family advances:
    channel g3
    membershipGeneration 3

send old request
    -> CURRENTNESS_CHANGED
    -> no append
    -> no silent upgrade
~~~

Exact retry identity follows VF-02C's producer scope:

~~~text
spaceRef
+ channelRef
+ authenticated principalRef
+ idempotencyKey
-> deterministic message identity
~~~

Therefore:

~~~text
same space + same channel + same principal + same key + same meaning
    -> same accepted message
    -> no duplicate append

same scoped identity + changed meaning
    -> IDEMPOTENCY_CONFLICT

same key reused in a different lawful successor channel
    -> different message identity
    -> independently admissible if current
~~~

Message sequence is channel-local, so the first canonical message in both g2 and g3 may be sequence 0
while their messageRefs remain distinct.

## In-memory Family provider

InMemoryVexFamilyConversationRepository follows the existing VexStream test-provider pattern.

~~~text
one explicit instance per test / Preview
same VexFamilyConversationRepository contract
state-only fake provider
no global/singleton state
no Home/network/filesystem/model/federation effect
~~~

publishThread(...) means external/provider truth changed in the fixture.

It is not a product membership mutation API.

The admitted speaker is provider fixture input. The request cannot override it.

## First visible Conversation Lab construction

PR #99 is the accepted first implementation consumer of the visible-Conversation design.

Feature-owned synthetic conditions:

~~~text
VexConversationLab
    deterministic Family-room seed/reset
    add/remove participant -> successor Family generation/channel
    remote provider-authored speech
    [Speaker]{message} debug injection resolves only known fixture labels
    optional mirror only after an accepted Victor Send
    explicit Home / Companion availability overrides outside Conversation state
~~~

Product-shaped rendering:

~~~text
screens/shared/conversation/VexConversationRoom.kt
    generic Conversation projection/rendering only
    stable dynamic message identity = messageRef
    human labels supplied by presentation/identity owner
    optional developer-environment label
    no second room-state model
    generic room does NOT choose Family Send semantics
    owning Screen supplies the durable composer/control content

screens/shared/conversation/VexConversationRoomPreview.kt
    Lab + Family-specific visual pressure only
    local raw composer is explicitly not the production action contract
~~~

The room renderer is intentionally not a registered app Screen yet. A real Screen needs the same
app/session-owned Conversation repository instance to cross the existing render bridge. Current
shared app/data composition paths are being changed by other active Android work, so PR #99 keeps
that exact integration effect held instead of constructing a Compose-owned repository or Debug
singleton.

~~~text
FEATURE_LOCAL_CONSTRUCTION != APP_GRAPH_INTEGRATION
VISIBLE_COMPONENT != REGISTERED_PRODUCTION_SCREEN
~~~

## Hybrid relationship over observed truth and Lab overrides

Conversation now reuses the accepted Compound Relay foundation for the question:

> When Conversation truth, authenticated-participant truth, Home/Companion observations and
> developer Lab overrides change independently, what do they mean together right now?

~~~text
Conversation repository -----\
authenticated participant ----\
Home availability ------------+-> VexConversationHybridRelay.processStates(...)
Companion availability -------/
Conversation Lab ------------/
                                      |
                                      v
                           effective current facts
                                      |
                                      v
                         VexConversationSurfaceProjection
~~~

The responsibilities are intentionally distinct:

~~~text
repository/provider
    owns admitted Conversation truth

Lab
    owns explicit developer perturbations
    and synthetic-provider controls

Hybrid Relay
    owns override precedence + retained current relationship

Surface Projection
    maps already-effective facts into UI-facing shape
~~~

~~~text
CONVERGENCE != PROJECTION
SOURCE_BINDING != LAB_PERTURBATION
LAB_OVERRIDE != REPOSITORY_TRUTH
~~~

Availability overrides do not mutate `VexConversationState`. If a real observation changes while an
override is active, clearing the override reveals that latest observation rather than restoring an old
snapshot.

Synthetic provider truth remains on the normal ingress path:

~~~text
Lab remote message
-> InMemoryVexFamilyConversationRepository.publishThread(...)
-> Conversation state changes
-> Hybrid observes repository truth
~~~

The Relay does not manufacture messages, Family generations or provider admission.

Developer/debug surfaces may use Hybrid provenance to explain whether an effective availability
is `OBSERVED`, `LAB_OVERRIDE` or `UNOBSERVED` without adding debug flags to Conversation domain
objects.

## Generic/direct Conversation is represented, not overclaimed

Current generic state can represent a direct human <-> Companion Conversation:

~~~text
familyBinding = null
first canonical message sequence = 0
~~~

That proves the generic data model does not require Family currentness.

It does not prove a current direct-message provider/write protocol.

Current Family Send projection therefore returns NotFamilyConversation for a generic direct thread.

Direct Send must be earned from a real direct-Conversation provider contract rather than copied from
Family or invented in the in-memory implementation.

## Surface projection

Generic Android presentation composition:

~~~text
Conversation thread
+ authenticated participant projection
+ Home availability
+ Companion availability
-> VexConversationSurfaceProjection
~~~

This projection owns no source truth or effects and does not itself claim Send is available.

Family-only write affordance is layered separately:

~~~text
projectVexFamilyConversationSendAvailability(surface)
-> Ready
-> NotFamilyConversation
-> HomeUnobserved
-> HomeUnavailable
-> AuthenticatedParticipantUnobserved
-> AuthenticatedParticipantUnknown
-> AuthenticatedParticipantNotCurrent
~~~

~~~text
SURFACE_ELIGIBLE != PROVIDER_ADMITTED
~~~

A Ready surface still crosses VexFamilyConversationRepository, which revalidates currentness.

## Why no Experience yet

Draft PR #81 uses Experience for a broad human/product orientation root.

Merged PR #82 uses <Name>Experience for a Behavior-local UI membrane.

Conversation does not resolve that foundation vocabulary locally. Current role-closing name:

~~~text
VexConversationSurfaceProjection
~~~

Second-consumer feedback is recorded on PR #81.

## Why no Conversation Behavior Family yet

Current Family Send carries private runtime payload plus provider currentness and authority.

The accepted Behavior kernel still holds generic request-payload architecture.

Do not force content into Control identity, Effect identity, static element identity, or generic
diagnostics.

No generic Conversation Behavior family is admitted by symmetry.

## Current real producer

The VexLife producer chain needed by Android has materially advanced beyond the original #89
formation snapshot.

Accepted/current producer owners now include:

~~~text
VF-02C model-free Family Conversation bridge
    accepted

durable canonical Family channel binding
    accepted

same-origin Family Conversation server
    accepted

real VEX_CORE -> Family session-authority adapter
    accepted

#481 lived authority proof
    REAL_SESSION_AUTHORITY_RECONNECTED_CURRENT
~~~

The real lived #481 proof traversed:

~~~text
Windows current-user verification
-> process-local authenticated Home session
-> fresh bounded security/Home leases
-> accepted VEX_CORE current projection
-> accepted Family authority adapter
-> same-origin Family HTTP APPEND / READ
~~~

and proved provider-derived speaker identity for Victor, forged browser authority rejection,
no model invocation, session revocation and bounded cleanup.

Therefore FAMILY_SESSION_AUTHORITY_UNAVAILABLE remains the correct fail-closed response when no
current authority resolver is available, but missing producer implementation is no longer the
standing Android blocker.

Current accepted browser surfaces include:

~~~text
GET  /api/v1/family/bootstrap
POST /api/v1/family/conversation
     operation = APPEND | READ | READ_THREAD_HISTORY | LIST
~~~

### Cross-generation Family history producer is accepted

VexLife PR #892 accepted the bounded authenticated `READ_THREAD_HISTORY` projection while keeping
`readFamilyThreadHistory(...)` as the canonical history owner.

The accepted response carries:

~~~text
spaceRef
threadRef
currentChannelRef
membershipGeneration
ordered segmentRefs[]
bounded historical messages with original:
    channelRef
    membershipGeneration
    recipientRefs[]
    witnessRefs[]
truncated
~~~

The producer still validates current Family membership/current channel, immutable historical channel
segments, FROM_JOIN visibility and historical witnessRefs. Android consumes that truth; it does not
reconstruct old audiences from current membership.

The current-channel `READ` response remains intentionally narrower.

## Real adapter composition

Thread history alone is not the whole current Family-room producer projection.

The intended real Android adapter composition is:

~~~text
GET /api/v1/family/bootstrap

    currentPrincipalRef
    current room / spaceRef
    current channelRef
    current membershipGeneration
    current human audience
    Family Companion lineage

+

authenticated Family thread history

    threadRef
    currentChannelRef
    current membershipGeneration
    ordered segmentRefs
    immutable historical messages / witnessRefs

-> Android Conversation state + Family binding
~~~

The two sources have different jobs:

~~~text
BOOTSTRAP
    current room/current audience/current principal/Family Companion identity

THREAD HISTORY
    cross-generation immutable historical message truth
~~~

[projectVexFamilyConversationServerThread] now owns this pure Android composition step. It checks
that current channel/generation/space/thread coordinates agree before forming domain state.

knownParticipants are formed from current human audience plus historical speaker/recipient/witness
refs; the exact Family Companion lineage identifies the Companion participant, while the other
accepted Family channel member refs remain human principals. The bootstrap must also preserve
`familyCompanionIncluded=true` before that lineage enters the current Android audience; lineage
identity alone is not treated as current inclusion.

Canonical Family history preserves the original producer speaker identity. Human browser APPEND
derives an authenticated human speaker, while the accepted Family Companion runtime may durably append
the exact Family Companion lineage as speaker. READ_THREAD_HISTORY preserves that speakerRef unchanged,
and the adapter keeps the lineage classified as Companion while retaining the immutable witness
snapshot. It fails closed if those producer meanings are contradicted.

The producer's `truncated` fact is preserved in [VexFamilyConversationThreadBinding.historyTruncated]
so a bounded history window never looks complete.

This is still projection only. Android has no generic Home/session transport owner yet, so HTTP,
authentication/session realization and app-session repository wiring remain held rather than being
invented inside Conversation.

## Testing / proof

Current focused proof:

~~~text
VexConversationStateTest
    generic/direct state
    sequence zero
    no invented Family generation

InMemoryVexFamilyConversationRepositoryTest
    canonical Family generation starts at 1 while first message sequence starts at 0
    VF-02C-scoped deterministic message identity
    same-key retry survives repository reconstruction from current state
    same key across successor channels yields distinct sequence-0 messages / refs
    Family provider currentness/idempotency
    provider-derived recipients
    historical Family segments/witnesses
    independent repository instances

VexFamilyConversationServerAdapterTest
    bootstrap/history coordinate agreement
    historical participant attribution
    Companion lineage classification
    truncation preservation
    malformed producer truth fails closed

VexConversationSurfaceProjectionTest
    generic surface projection
    Family-only Send eligibility
    Companion availability independence
~~~

No VexExecutionTestRule is used because no Conversation VexExecution consumer exists yet.

## Production app integration held

The accepted Conversation foundation deliberately did not modify AppRepositories, VexLifeSession,
TalkToVexScreen, TalkToVexJourneys, VexRuntime, or VexTestEngine.

PR #99 landed the feature-local Lab / fixture / generic-room / Preview slice. The accepted
feature-local Hybrid refinement keeps the same discipline: it reuses VexCompoundRelay and does
not claim shared app/session/Debug ownership.

Shared app/data, Debug Menu and Journey integration remain separate. Parallel Android work still changes some of
those composition paths.

When the exact shared paths reconcile, integrate one app/session-owned Conversation repository and
the Debug Menu doorway through the established Screen/action-registration shape. Do not replay the
feature locally through a second repository just to avoid coordination.

## Deliberate holds

~~~text
HELD:
    production Home-backed Family Conversation repository / authenticated transport
WHY:
    the producer data contract is now accepted and projectable, but Android has no generic
    Home/session transport owner and shared app-session composition paths remain under active work

HELD:
    generic/direct Conversation Send
WHY:
    no real direct write provider contract has been consumed

HELD:
    production Screen / app-session wiring
WHY:
    feature-local lab/room construction exists, but shared app/data composition must be reconciled
    before one repository instance can reach the registered Screen render bridge

HELD:
    Debug Menu Conversation Lab doorway
WHY:
    feature-specific lab semantics now exist; shared Debug/AppAction composition remains a separate
    integration seam and must not absorb those semantics

HELD:
    Journey integration
WHY:
    canonical test graph cannot yet inject Conversation without overlap

HELD:
    Conversation Behavior Family
WHY:
    payload/authority/currentness grammar has not earned one

HELD:
    generic VexConversationExperience
WHY:
    Experience vocabulary differs between #81 and merged #82
~~~

## Review checklist

A fresh worker should be able to answer:

1. Which parts of the Android model are generic Conversation?
2. Which exact fields are Vex Family Conversation-specific?
3. Why can the first canonical message have sequence 0?
4. Why does generic/direct Conversation not require membershipGeneration?
5. Why is current write support VexFamilyConversationRepository rather than generic Send?
6. Why are recipientRefs and witnessRefs different?
7. Why does Android not author Family recipients or speaker identity?
8. What happens when Family currentness changes between request formation and Send?
9. Why is InMemoryVexFamilyConversationRepository not the architecture?
10. Why do bootstrap and thread history both matter to the real adapter?
11. Why is a visible membership divider not a Conversation event?
12. Which exact producer/app-graph predicates still hold production integration?

## Next edge

Current implementation edge:

~~~text
reconcile app/session repository ownership when the shared composition paths clear
-> register one real Conversation Screen over the accepted Hybrid output
-> connect Debug Menu doorway to the existing feature-owned Lab controls
-> add Journey/device proof only for the visible controls actually admitted
~~~

The feature-local Lab, mixed-provenance behavior, Hybrid Name Ontology and focused JVM/build proof are
already accepted on main. Do not replay them merely because production app wiring remains held.

Independent future predicates remain:

~~~text
authenticated Family thread history is exposed through the accepted server authority chain
Experience vocabulary is reconciled by its owner
a real non-Family/direct write provider contract appears
~~~

No human action is currently required.

<!-- [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][GENERIC-PLUS-FAMILY-BINDING] -->
