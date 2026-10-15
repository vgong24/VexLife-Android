# VexLife Android Conversation — Feature Home

[VXG RealForever]

## Start here

Question:

> How can Android participate in one durable Conversation with humans and/or Vex companions without
> becoming a second Conversation owner, Home, membership authority, model runtime, or history store?

Current draft:

~~~text
VexLife-Android PR #89
state = architecture / consumer-contract formation
production Screen wiring = HELD
real Home adapter = HELD
~~~

Code navigation:

~~~text
VexConversationDocs
~~~

Current source map:

~~~text
conversation/
├── VexConversationState.kt
├── VexConversationRepository.kt
├── InMemoryVexConversationRepository.kt
├── VexConversationSurfaceProjection.kt
├── VexConversationDocs.kt
└── README.md
~~~

Read this file for the whole map. Then descend to the exact source owner for the question you are
changing.

## What Android owns

Android owns the native consumer and presentation realization.

It may own:

~~~text
typed consumer projections
native input / Compose realization
Android lifecycle integration
provider adapter implementation
test / Journey / device proof
~~~

It does not thereby own:

~~~text
Family membership
authenticated human principal
Home authorization
portable Conversation semantics
durable Home Conversation Store
Companion/model runtime
federated replica semantics
~~~

## Portable owner map

~~~text
Home Bridge / authenticated session
    current human principal + device/session authority

Family Space
    membership / role / generation / history policy

service.conversation
    channel / message / context meaning

Conversation Store
    durable immutable channel + message lineage inside the rightful Home

Family membership runtime
    immutable successor channel formation
    stable-thread cross-generation history

Companion runtime
    optional AI participation
    separate from ordinary human message persistence
~~~

Permanent non-collapses:

~~~text
HUMAN_MESSAGE_PERSISTENCE != MODEL_INFERENCE
HUMAN_MESSAGE_DELIVERY != AI_ATTENTION
MODEL_BUSY != FAMILY_CHAT_BLOCKED

ADDRESSEE != AUDIENCE
recipientRefs != witnessRefs

CURRENT_MEMBERSHIP_CHANGE != HISTORICAL_MESSAGE_REWRITE
CURRENT_MEMBERSHIP_CHANGE != HISTORICAL_WITNESS_REWRITE
~~~

## Android construction precedent

The consumer/test shape follows VexStream rather than inventing a Conversation-only fixture system.

~~~text
VexStreamMusicRepository
    production-facing Android consumer contract

InMemoryVexStreamMusicRepository
    same contract
    one instance per test / Preview

real providers
    same contract

Screen
    receives repository from app composition

Journey signal
    changes fake external/provider truth

human Journey
    uses real Screen controls
~~~

Conversation currently mirrors only the parts it has earned:

~~~text
VexConversationRepository
    Android consumer contract

InMemoryVexConversationRepository
    same contract
    state-only tests/Preview implementation
    one explicit instance per scenario

VexConversationState
    Conversation-owned state only

VexConversationSurfaceProjection
    pure UI-facing composition over several owner projections
~~~

Rules:

~~~text
IN_MEMORY_PROVIDER != ARCHITECTURE
TEST SIGNAL != PRODUCT EFFECT
COMPOSE != REPOSITORY LIFETIME OWNER
~~~

## Typed identity

Conversation uses distinct types where cross-wiring would be costly:

~~~text
VexConversationSpaceRef
VexConversationThreadRef
VexConversationChannelRef
VexConversationMessageRef
VexConversationParticipantRef
VexConversationIdempotencyKey
~~~

Do not replace them with generic String plumbing merely to shorten code.

Important lattice:

~~~text
personRef
!= principalRef
!= deviceRef
!= homeRef
!= companionLineageRef
~~~

Conversation identity:

~~~text
threadRef
    stable conversational continuity

channelRef
    one immutable audience/currentness segment

messageRef
    one immutable message event
~~~

## Conversation-owned thread state

VexConversationThreadState currently owns only:

~~~text
threadRef
spaceRef?                  # Family/space-scoped when present
currentChannelRef
currentMembershipGeneration
ordered segmentRefs[]
knownParticipants
currentAudienceRefs
immutable messages[]
~~~

### Known participants versus current audience

~~~text
knownParticipants
    identities required to attribute retained history

currentAudienceRefs
    current audience of the active segment
~~~

A participant who leaves may disappear from currentAudienceRefs while remaining known so historical
messages and witnesses are still attributable.

### Presentation identity is separate

VexConversationParticipant contains:

~~~text
participantRef
Human | Companion kind
~~~

Conversation does not own:

~~~text
displayName
avatar
relationship label
Family role
human-facing room title
~~~

Those belong to identity / Relationships / Family / product presentation owners.

Friendly UI is expected. Friendly labels simply do not become semantic authority.

## Canonical Family thread history

Current VexLife source already owns cross-generation history:

~~~text
family-membership-runtime.readFamilyThreadHistory(...)
~~~

It validates:

~~~text
exact current Family generation
exact current channel binding
all immutable channel segments for one threadRef
segment membership snapshots
message source binding
FROM_JOIN history visibility
historical witnessRefs
~~~

and returns a bounded projection equivalent to:

~~~text
spaceRef
threadRef
currentChannelRef
membershipGeneration
segmentRefs[]
messages[]
truncated
~~~

Android therefore models:

~~~text
ordered channel segments
+
immutable messages bound to their original channel/generation/witnesses
~~~

It does not manufacture a Conversation event such as:

~~~text
Maya joined
Maya left
~~~

A future surface may render a visual divider when exact segment/generation truth changes, or when a
separately owned Family lifecycle projection supplies that event.

~~~text
VISIBLE_MEMBERSHIP_DIVIDER != CONVERSATION_MESSAGE_EVENT
~~~

## Message truth

VexConversationMessage preserves:

~~~text
messageRef
threadRef
spaceRef?
channelRef
membershipGeneration
sequence
speakerRef
recipientRefs[]
witnessRefs[]
content
~~~

Laws:

~~~text
speakerRef ∈ witnessRefs
recipientRefs ⊆ witnessRefs
message channelRef ∈ thread segmentRefs
message generation <= current thread generation
~~~

recipientRefs answer who the provider addressed for delivery.

witnessRefs preserve the immutable event audience.

A recipient subset never creates private audience truth.

## Current Family Send contract

Android Send is aligned to current VexLife VF-02C rather than an Android-designed protocol.

For the Family path the caller supplies:

~~~text
spaceRef
channelRef
content
expectedMembershipGeneration
idempotencyKey
~~~

VexConversationSendRequest also retains threadRef as an Android-local routing/current-thread
coordinate.

Android must not author:

~~~text
speakerRef
Family recipient authority
witnessRefs
membership / role truth
principal binding
capability lease
Home authority
model availability
~~~

The provider derives those from trusted current source.

## Family recipient / Companion distinction

Current VF-02C derives the other active human principals as ordinary human-message recipients.

Family Vex may remain part of the immutable Family channel witness truth while AI attention remains a
separate downstream concern.

Therefore:

~~~text
Family Vex is in witness audience
DOES NOT IMPLY
ordinary human message append blocks on model work
~~~

## Currentness and idempotency

A draft/request is bound to the Conversation truth the human saw.

Example:

~~~text
request formed:
    channel = g2
    membershipGeneration = 2
    idempotencyKey = K1

provider advances:
    channel = g3
    membershipGeneration = 3

send old request
    -> CURRENTNESS_CHANGED
    -> no append
    -> no silent upgrade

form new request from current truth
    -> provider may admit it
~~~

Exact retry law:

~~~text
same idempotency key + same request meaning
    -> same accepted message
    -> no duplicate append

same key + changed request meaning
    -> IDEMPOTENCY_CONFLICT
~~~

## Repository boundary

VexConversationRepository currently exposes:

~~~text
state: StateFlow<VexConversationState>

send(request)
    -> Accepted(messageRef)
    -> Rejected(reason)
~~~

The repository is an Android consumer boundary, not the portable semantic owner.

A real Home-backed implementation remains held until the current producer requirements below are
satisfied.

Do not hide initial network loading in Compose or manufacture constructor-side asynchronous magic.
When the real adapter is admitted, its load/refresh operation should be formed from the actual
bootstrap/history provider semantics and their real failure classes.

## In-memory provider

InMemoryVexConversationRepository:

~~~text
implements VexConversationRepository
one instance per test/Preview
no global/singleton state
no network
no filesystem
no Home mutation
no Family membership mutation
no model effect
no federation effect
~~~

Provider fixture input supplies admittedSpeakerRef to stand in for authenticated server truth.

The caller still cannot author the speaker.

publishThread(...) means:

~~~text
external/provider truth changed in the test fixture
~~~

It does not mean:

~~~text
product UI may mutate Family membership
~~~

## Surface projection

Conversation truth is not enough to render the whole Android experience.

The surface eventually also needs:

~~~text
authenticated current participant
Home availability
Companion availability
friendly presentation metadata
~~~

Those are different owner projections.

Current pure boundary:

~~~text
projectVexConversationSurface(
    Conversation thread,
    authenticated participant,
    Home availability,
    Companion availability
)
-> VexConversationSurfaceProjection
~~~

This projection owns no state or effect.

### Send eligibility versus provider admission

The surface may derive:

~~~text
Ready
HomeUnobserved
HomeUnavailable
AuthenticatedParticipantUnobserved
AuthenticatedParticipantUnknown
AuthenticatedParticipantNotCurrent
~~~

Ready means the UI may truthfully offer the attempt.

It does not mean the provider has accepted Send.

~~~text
SURFACE_ELIGIBLE != PROVIDER_ADMITTED
~~~

Repository Send still revalidates currentness and authority.

### Companion availability

~~~text
Companion unavailable
!=
human Conversation unavailable
~~~

A current human participant with current Home access may still be eligible to send ordinary human
Conversation messages while Vex is unavailable.

## Why this is not called Experience

Two repository lines currently use Experience differently.

Draft PR #81:

~~~text
Experience
    broad human/product orientation root
    explains why several rightful owners belong together
~~~

Merged PR #82:

~~~text
<Name>Experience
    Behavior-local UI membrane
    displayState + activate(Control)
~~~

Conversation needs concepts near both scales, so it does not silently choose one definition.

Current role-closing name:

~~~text
VexConversationSurfaceProjection
~~~

Second-consumer vocabulary pressure has been routed to PR #81.

## Why there is no Conversation Behavior Family yet

Merged PR #82 says a Behavior family is earned by an independently meaningful behavioral question.

Current Send still carries runtime request data and crosses several authority/currentness boundaries.

The accepted Behavior kernel deliberately holds generic runtime payload architecture.

Do not force message content into:

~~~text
Control identity
Effect identity
static UI element identity
generic diagnostics
~~~

No VexConversationFamily / State / Control / Act / Effect / Behavior family is admitted by symmetry.

## Vex Behavior Family versus Vex Family

These are different meanings:

~~~text
VexBehaviorFamily
    compile-time State / Control / Act / Effect kinship

Vex Family
    product/social Family Space + membership + Family Vex domain
~~~

Do not use ambiguous ConversationFamily naming that forces a future reader to reconstruct which
"Family" was intended.

The broader naming collision is foundation feedback, not something this feature should locally
rename around.

## Current VexLife producer

Family conversation route:

~~~text
POST /api/v1/family/conversation
~~~

Owner:

~~~text
VexLife issue #427
VF-02C Conversation Bridge
~~~

The route is model-free and composes trusted Home + Family + Conversation owners.

### Producer predicate 1 — production authority reconnection

The core server requires:

~~~text
resolveFamilyConversationAuthority
~~~

Ordinary production startup currently does not bind that resolver.

Default production Family requests therefore fail closed with:

~~~text
FAMILY_SESSION_AUTHORITY_UNAVAILABLE
~~~

Accepted Home Bridge Session Authority and authenticated-session owners already exist.

Android must not invent another authentication route around this hold.

### Producer predicate 2 — authenticated thread history

Canonical cross-generation history already exists as readFamilyThreadHistory(...).

Current VF-02C HTTP READ is narrower/current-channel scoped, and its bounded message projection omits
historical witnessRefs.

Android therefore holds the real network adapter until VF-02C exposes the existing bounded
authenticated thread-history projection or an exactly equivalent safe projection providing:

~~~text
stable threadRef
current channel/generation
ordered authorized segmentRefs
bounded messages with canonical historical witnessRefs
~~~

Android consumer feedback for both predicates is recorded on VexLife issue #427.

## App-graph coordination

Current PR #89 deliberately does not modify:

~~~text
AppRepositories
VexLifeSession
TalkToVexScreen
TalkToVexJourneys
VexRuntime
VexTestEngine
~~~

Parallel Android work currently occupies shared app/runtime/Journey paths.

When those paths clear, production integration should follow the established VexStream shape:

~~~text
AppRepositories
    -> VexConversationRepository

VexLifeSession / rightful owner adapters
    -> Home/session projection
    -> Companion projection

Conversation Screen
    -> repository state
    -> projectVexConversationSurface(...)
    -> native Compose realization

VexTestEngine
    -> InMemoryVexConversationRepository

journeySignal(...)
    -> external/provider state change

human Journey
    -> real Screen controls
~~~

## Testing / proof architecture

Governing law:

~~~text
TEST THE TRANSFORMATION AT ITS OWNER
~~~

Current proof:

~~~text
repository/domain JVM
    identity/currentness/idempotency/history transformations

surface projection JVM
    owner inputs -> UI-facing eligibility/projection
~~~

Not yet earned:

~~~text
Compose render contract
Journey
real Home integration
device/lived proof
~~~

No VexExecutionTestRule is used because no Conversation VexExecution consumer exists yet.

If a real execution bridge is later admitted, test its coroutine lifetime with the shared
VexExecution fixture at that owner.

## Deliberate holds

~~~text
HELD:
    production Home-backed Conversation repository

WHY:
    production authority reconnection + authenticated thread-history projection remain producer gaps

HELD:
    production Screen / Compose wiring

WHY:
    app graph/shared paths remain occupied and real provider/lifetime inputs are not connected

HELD:
    Journey integration

WHY:
    canonical test graph cannot yet inject Conversation without shared-path overlap

HELD:
    Conversation Behavior Family

WHY:
    payload/authority/currentness grammar has not earned one

HELD:
    generic VexConversationExperience

WHY:
    Experience vocabulary differs between #81 and merged #82

HELD:
    Android durable Conversation database / outbox

WHY:
    remote-surface retention/reconciliation semantics are not established

HELD:
    multi-Home federation transport

WHY:
    portable replica reconciliation exists; Android provider effect does not

HELD:
    local-sibling Android Home

WHY:
    distinct Home/lineage establishment is a separate lifecycle effect

HELD:
    Family membership mutation UI

WHY:
    Family Space / membership runtime remain the semantic owners

HELD:
    automatic Vex/model reply

WHY:
    AI attention remains separate from human message persistence
~~~

## Review checklist

A fresh human or AI should be able to answer from source:

1. Why is Talk to Vex only one entry into Conversation?
2. What is stable thread identity versus immutable channel segment identity?
3. Why are segmentRefs canonical while a Joined/Left Conversation event is not?
4. Why do knownParticipants and currentAudienceRefs differ?
5. Why are recipientRefs and witnessRefs different?
6. Why does Android not supply Family recipients or speaker identity?
7. What happens when membership changes between request formation and Send?
8. Why is the in-memory repository not the architecture?
9. Why are display names and room titles outside Conversation truth?
10. Why can human chat remain available while Companion runtime is unavailable?
11. Why is the cross-owner surface a pure projection rather than a Relay today?
12. Why is it not called VexConversationExperience?
13. What exact VexLife producer predicates block the real Android adapter?
14. Why are Screen/Journey/app-graph changes held?

If those answers require predecessor chat, this feature home or its linked source has lost the map.

## Next edge

Do not add another abstraction merely because source work is available.

Resume when one of these material predicates changes:

~~~text
VF-02C exposes authenticated Family thread history
production Family authority resolver is reconnected
AppRepositories / Journey shared ownership clears
Experience vocabulary is reconciled by its rightful foundation lane
~~~

Then descend only to the affected owner and continue the next truthful vertical slice.

No human action is currently required.

<!-- [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][FEATURE-HOME] -->
