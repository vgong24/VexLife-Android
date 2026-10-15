# VexLife Android Conversation — Feature Home

[VXG RealForever]

## Feature identity

~~~text
FEATURE / HOME:
    VexLife Android Conversation

CURRENT STATUS:
    FOUNDATION CANDIDATE
    no Conversation runtime/provider/storage implementation is claimed by this milestone

PORTABLE SEMANTIC OWNER:
    VexLife service.conversation

ANDROID RESPONSIBILITY:
    native participant surface + Android realization/proof
    not a second Conversation semantic owner

CURRENT NEXT EDGE:
    one synthetic, group-ready Android vertical slice that pressures:
      runtime message payloads
      private diagnostic handling
      current channel/authority revalidation
      dynamic message identity
    before a production Behavior family is named
~~~

## Question

> Can a person enter and continue one addressed conversation from Android with humans and/or Vex
> companions while preserving who spoke, who could witness, where durable history belongs, and what
> current authority permits — without turning the phone, Compose, or the model into a second
> Conversation owner?

This document is the durable feature home for that question.

It begins with meaning and owner boundaries before package taxonomy. It deliberately does not invent a
complete Conversation class hierarchy merely because the UI will eventually contain a timeline,
composer, participant surface, storage adapter, model integration, and federation path.

## Source status used to form this home

The sources below do not all have the same authority.

### Accepted Android source

Current accepted VexLife-Android main establishes:

- merged PR #82: Vex Behavior;
- merged PR #87: VexExecution;
- AppScreenBlueprint custom Screen realization;
- VexStableLazyColumn for runtime data with stable semantic identity;
- the existing TalkToVexScreen placeholder destination.

PR #82 is the primary Android architectural precedent for this feature.

Its accepted grammar is:

~~~text
STATE
    what is actually true now

CONTROL
    stable human-facing affordance

ACT
    what that Control means from current State

EFFECT
    runtime transformation that should be attempted
~~~

with the full equations:

~~~text
Control + CURRENT State
-> Act

CURRENT State + Act
-> Effect + admitted State

CURRENT State + Controls
-> Projection

Projection
-> optional feature DisplayState
-> UI treatment
~~~

A Behavior Family is compile-time semantic kinship only. It is not Vex Family, a service, a state
store, or a runtime owner.

### Developmental quarry, not accepted authority

These active/draft Android sources are useful pressure evidence but are not silently promoted to
accepted repository law here:

- PR #81: whole-product VexStream Experience / feature-authoring exploration;
- PR #86: VexMaps Distribution as a second production pressure consumer of PR #82;
- PR #88: dynamic runtime presentation/QA identity refinement.

In particular, PR #81's whole-product VexStreamExperience and PR #82's
VexStreamPlaybackExperience are different ideas today.

Merged PR #82 uses Experience as a UI-facing membrane around one admitted Behavior slice:

~~~text
Connection / VexPortFlow
-> Projection
+ provider read data when needed
-> DisplayState
-> Experience
-> Compose

Control
-> Experience
-> VexPort
~~~

The Experience is composition of the read/write boundary. It is not semantic truth.

No generic VexExperience abstraction is accepted yet.

### Portable VexLife source owners

Current VexLife source already owns the important Conversation semantics Android must consume rather
than recreate:

~~~text
Home Bridge
    authenticated human principal/device-session authority

Family Space
    membership, role, generation, principal binding, history policy

Conversation / service.conversation
    channel/message/context envelope semantics

Conversation Store
    durable immutable channel/message materialization in one Vex Home

Companion / Family Companion runtime
    optional AI attention/inference path
    separate from ordinary human message persistence
~~~

Current portable laws include:

~~~text
HUMAN_MESSAGE_PERSISTENCE != MODEL_INFERENCE
HUMAN_MESSAGE_DELIVERY != AI_ATTENTION
MODEL_BUSY != FAMILY_CHAT_BLOCKED

ADDRESSEE != AUDIENCE
DURABLE_CHANNEL_BINDING != CURRENT_FAMILY_MEMBERSHIP_AUTHORITY

CURRENT_MEMBERSHIP_CHANGE != HISTORICAL_CHANNEL_REWRITE
CURRENT_MEMBERSHIP_CHANGE != HISTORICAL_MESSAGE_REWRITE
~~~

The SDK also contains participant-owned friend/group replica comparison and reconciliation semantics.
That is future federation pressure for Android. It does not mean Android currently has a federated
Conversation transport or replica store.

## Experience sentence

A participant comes to Conversation because they want to continue an addressed relationship with
humans and/or Vex companions from the device they are holding.

The experience should let them perceive:

- which conversational continuity they are in;
- who the current audience is;
- who authored each message;
- what history they are currently allowed to see;
- whether the current Home/authority path can accept a request;
- whether a message is admitted locally, awaiting remote delivery, failed, or otherwise unresolved;
- when AI participation is unavailable without blocking ordinary human conversation.

Android projects and accepts interaction. The rightful Home / Conversation owners decide identity,
authority, durable append, and currentness. A model may participate in a conversation; it does not own
the conversation merely because it can produce a reply.

## Human question map

These are questions first, not required Kotlin classes.

~~~text
HOME:
    Conversation

WHO:
    a current participant
    human and companion authors remain distinguishable

WHY:
    continue an addressed durable conversation from the current device

WHAT:
    current thread/channel/message projections already owned by service.conversation
    plus participant/audience facts supplied by their rightful owners

WHERE:
    the participant's rightful Vex Home / authorized Conversation source
    remote Android surface today is not automatically another canonical store

HOW:
    portable Conversation + authority/currentness contracts
    plus Android Behavior families only where an independent behavioral question is earned

NOW / WHEN:
    current channel generation
    current audience/membership
    current authenticated authority
    Home/source reachability
    append/delivery/currentness truth

WHAT CAN I DO?:
    read current conversation
    compose a private local draft
    request Send
    later controls only when their semantic owner is real

WHO / WHAT REALIZES IT?:
    Home Bridge/session authority
    Conversation service/store
    optional companion runtime for AI participation
    future federation/transport owner when admitted

WHAT DO THEY SEE?:
    Android timeline/composer/current audience/currentness/failure projections
~~~

## Identity grammar

Do not collapse these identities:

~~~text
personRef
principalRef
deviceRef
homeRef
companionLineageRef

threadRef
channelRef
messageRef

speakerRef
recipientRefs[]
witnessRefs[]
~~~

### Thread versus channel

A stable thread may cross several immutable channel generations.

Family membership changes do not rewrite an existing channel audience. They create a successor
channel generation while preserving the stable thread.

Android should therefore be able to present one understandable conversational continuity while
preserving exact historical channel identity underneath.

~~~text
threadRef = stable conversational continuity

channelRef = one exact audience/currentness envelope generation

messageRef = one immutable message event
~~~

### Addressing versus audience

~~~text
recipientRefs = addressed delivery targets
witnessRefs   = immutable audience snapshot for that event
~~~

Addressing a subset inside a GROUP channel does not create a private conversation.

Private conversation requires a distinct private channel/current audience boundary.

That distinction must remain visible in any Android request/projection contract.

## Owner map

~~~text
SEMANTIC MESSAGE / CHANNEL MEANING
    VexLife service.conversation

CURRENT FAMILY MEMBERSHIP / ROLE / GENERATION
    Family Space

AUTHENTICATED HUMAN PRINCIPAL / DEVICE SESSION
    Home Bridge

DURABLE MESSAGE / CHANNEL LEDGER
    Conversation Store inside the rightful Vex Home

MODEL / AI TURN
    Companion / Family Companion runtime
    separate from ordinary Conversation persistence

ANDROID UI / NATIVE INPUT / ACCESSIBILITY / LIFECYCLE
    VexLife-Android

ANDROID COROUTINE CUSTODY WHEN EARNED
    VexExecution
    mechanics only; no business retry/currentness authority

CROSS-FAMILY REACTION
    convergence above complete local truths
    no foreign Effect bypass

FEDERATED PARTICIPANT REPLICA / MIRROR RECONCILIATION
    portable SDK pressure/source
    no Android production effect claimed yet
~~~

## The word Family has two different meanings here

This feature must keep these concepts explicit because both are real:

### Vex Behavior Family

~~~text
compile-time kinship:
    State / Control / Act / Effect
~~~

It has no runtime identity or social meaning.

### Vex Family

~~~text
product/social domain:
    Family Space
    membership
    Family companion lineage
    Family conversation audience/currentness
~~~

Do not name a Conversation Behavior family merely ConversationFamily without enough context if that
would make source ambiguous beside the Vex Family product domain.

Naming must let a new human or AI know which meaning is intended without reconstructing history.

## Behavior formation — do not force the blueprint

PR #82 does not require every feature to manufacture one Behavior family. It says to split by an
independently meaningful behavioral question.

Conversation currently exposes several pressures that must not be collapsed prematurely.

### Pressure A — addressed message append

Candidate human question:

> From the current conversation truth, can this participant request that this exact addressed message
> be appended?

This is a strong Behavior candidate, but it is not yet formed.

The reason is important: Send carries runtime request data.

A useful request may contain:

~~~text
thread/channel identity
content
recipientRefs
replyToMessageRef?
contextRangeRefs?
idempotency/currentness data
~~~

Merged PR #82 deliberately leaves generic runtime request payload architecture unresolved.

Its current VexPort shape activates stable Control identity:

~~~text
activate(Control)
~~~

Conversation must not smuggle private message content into Control identity, global static elements,
or an Effect merely to make the existing generic API fit.

The first vertical slice should pressure this boundary and preserve the result. Plausible outcomes
include a feature-local payload seam or a reusable foundation evolution, but neither is admitted in
advance.

### Pressure B — message delivery / federation

A successful local Conversation append is not the same question as remote mirror/delivery.

~~~text
MESSAGE_APPENDED
!= MIRRORED_COMPLETE
!= SEMANTIC_ACKNOWLEDGEMENT
!= UNDERSTANDING
~~~

Do not make one Send state machine pretend all four are one provider outcome.

Federated delivery may later earn its own semantic owner/family or remain transport/reconciliation
truth beneath a feature projection. Real consumer pressure must decide.

### Pressure C — membership

Join/leave/remove/role mutation belongs to Family Space / Family lifecycle authority.

Conversation may project the result. Android must not create a second membership Behavior family just
because a participants sheet eventually has buttons.

### Pressure D — AI attention

AI attention/inference is separate from human message append.

A human message may be terminally persisted while Vex is:

~~~text
busy
unavailable
not addressed
not requested
cancelled
~~~

If an AI later responds, that response enters Conversation as its own admitted message event through
the rightful owner.

The Android feature should not make model availability the gate for ordinary human conversation.

## Experience membrane — candidate, not yet minted

Merged PR #82 establishes a useful UI law:

> Compose should not repeatedly assemble Connection + Projection + DisplayState + activation plumbing.

Conversation will likely earn one UI-facing membrane because the Screen will need a coherent timeline,
current audience/currentness, composer treatment, and activation path.

However this feature home does not yet declare the final interface or generic abstraction.

The first implementation should demonstrate what Compose actually needs before naming the membrane.

If it earns an Experience, that Experience must remain:

~~~text
UI-facing composition boundary
!= semantic Conversation owner
!= Home authority
!= durable message store
!= model runtime
!= federation coordinator
~~~

## Storage and platform modes

### Remote Android surface

For the first intended personal path:

~~~text
Android phone
    -> authenticated Home Bridge request/projection

participant's canonical Vex Home
    -> Conversation Store
    -> optional local model/companion runtime
~~~

The phone is not automatically another canonical Conversation writer.

Android may later earn a bounded encrypted cache, local draft/outbox, or offline read projection.
Those are held until their retention/currentness/reconciliation semantics are explicit.

### Local sibling Home

If Android later hosts a true local sibling Home, that is a different mode:

~~~text
distinct Home
distinct companion lineage by default
distinct replica/currentness
~~~

It must not be described as the same uninterrupted companion merely because selected Score/history
can synchronize.

### Participant-owned federation

The portable SDK already models participant-owned friend/group replicas, heads, checkpoints,
mirror receipts, mismatch states, and reconciliation proposals.

Useful existing distinctions include:

~~~text
relationshipRef != conversationRef
shared record != participant private Memory
mirror delivery != acknowledgement
alignment checkpoint != history rewrite
~~~

This is design pressure, not proof that Android already supports multi-Home federation.

No centralized cloud Conversation store is introduced by this feature home.

## Android realization principles

### Screen

TalkToVexScreen is currently a placeholder destination.

Long-term meaning:

~~~text
Talk to Vex
    = one entry / selected conversation

Conversation
    = broader feature
~~~

The Android feature must remain able to represent:

~~~text
human <-> Vex
human <-> human
human + human + Vex
larger group channels
~~~

without changing the fundamental Conversation identity model.

A custom AppScreenBlueprint renderer is appropriate when the real timeline/composer surface is
formed. The Screen should retain static semantic identities while runtime messages remain data.

### Timeline

VexStableLazyColumn is a strong existing Android primitive for a runtime message feed because it
supports stable item identity without making visual index into product identity.

Candidate realization:

~~~text
listRef = stable feed semantic identity
itemRef = messageRef
~~~

Dynamic message rows must not become one permanent static blueprint element per message.

### Composer draft

Typed message text is presentation-local draft state until an explicit Send boundary.

Keystrokes are not semantic runtime actions.

### Private diagnostics — required pressure

Current VexRuntime submit handling serializes FormSubmission values into generic diagnostic JSON.

That is not automatically acceptable for private Conversation text.

Therefore a real Conversation Send path must prove:

~~~text
generic semantic log can record:
    Send requested
    stable semantic element/action identity
    non-content currentness/result metadata when appropriate

generic semantic log must not copy by default:
    private message body
    credentials/tokens
    private transcript content
~~~

This is a concrete reusable-foundation pressure.

The feature should prefer the smallest rightful fix at the submit/diagnostic boundary rather than a
Conversation-only logging bypass.

## VexExecution use

Conversation may need both maintained observation and finite work.

Potential shapes, only when the owning feature/provider seam is real:

~~~text
launchForCollect(conversationSource::states) { projection ->
    accept(projection)
}

launchOnce(conversationSource::send, request)
~~~

VexExecution owns coroutine custody only.

It does not own:

~~~text
whether Send is lawful
message idempotency
speaker identity
membership currentness
business retry
federation retry
model retry
~~~

A downstream consumer failure must not become invisible Flow resubscription that replays a
non-idempotent message send.

## First vertical slice

The first implementation milestone should be deliberately synthetic and group-ready.

It should pressure the architecture with at least:

~~~text
one direct Companion thread
one multi-participant thread containing humans + a Vex companion
stable threadRef/channelRef/messageRef identity
immutable speaker/recipient/witness projection
one membership-generation boundary in fixture history
a private local composer draft
Home available / unavailable treatment
AI available / unavailable independently of human chat
~~~

The slice should not claim:

~~~text
real Home transport
real durable Android Conversation database
real federation
real remote human delivery
real model invocation
real Family membership mutation
~~~

Synthetic proof must remain labeled synthetic.

## Acceptance walks to earn before real integration

### Open an existing conversation

~~~text
GIVEN
    one current authorized Conversation projection

WHEN
    the participant enters its Android surface

THEN
    the thread/audience/messages are projected with exact stable identities

EXPERIENCED AS
    one understandable timeline
~~~

### Human conversation remains usable without AI

~~~text
GIVEN
    a group conversation with human participants and one Vex companion
    AI runtime unavailable

WHEN
    an admitted human message is appended

THEN
    ordinary Conversation truth advances without waiting for model inference

EXPERIENCED AS
    human chat continues
    Vex availability is separately perceptible
~~~

### Membership history remains honest

~~~text
GIVEN
    one stable thread spanning two immutable channel generations

WHEN
    the current audience changes

THEN
    old messages retain their original channel/witness generation
    new messages use the successor current channel

EXPERIENCED AS
    one conversational continuity with a visible membership boundary
~~~

### Remote Home unavailable

~~~text
GIVEN
    Android is a remote surface
    canonical Home is unreachable

WHEN
    the participant attempts to continue Conversation

THEN
    Android does not silently become the Home writer
    Android does not substitute a different local model as the same Vex

EXPERIENCED AS
    current unavailability / unsent draft truth
~~~

### Private message content is not generic diagnostics

~~~text
GIVEN
    a private composer draft

WHEN
    Send crosses the semantic action boundary

THEN
    the rightful Conversation request receives the real content
    generic Vex diagnostic history does not persist the body by default
~~~

## Proof map

~~~text
portable Conversation/Family invariants
    VexLife source/tests remain owner

Android behavior/request transformation
    focused JVM tests

Android projection -> Compose
    render/Compose proof

human interaction walk
    Journey

coroutine lifetime/currentness
    VexExecution-focused tests

real Home Bridge / Conversation server integration
    integration proof against exact provider/runtime

real Android lifecycle/input/accessibility
    instrumentation/device proof

lived product feel
    Victor physical review
~~~

Proof follows the claim. A synthetic timeline does not prove remote Conversation delivery.

## Deliberate holds

~~~text
HELD:
    final Conversation Behavior-family names

WHY:
    runtime message payload pressure is unsolved in accepted Behavior

HELD:
    generic VexExperience abstraction

WHY:
    merged PR #82 explicitly holds promotion from one feature;
    draft later consumers are evidence, not accepted generic law

HELD:
    Android durable Conversation database / offline outbox

WHY:
    remote-surface retention/reconciliation semantics are not yet earned

HELD:
    participant-owned multi-Home federation transport

WHY:
    portable replica reconciliation exists, Android transport/provider does not

HELD:
    local-sibling Android Home mode

WHY:
    distinct Home/lineage establishment is a separate product/lifecycle effect

HELD:
    Family membership management UI

WHY:
    portable Family lifecycle authority remains the owner

HELD:
    automatic Vex/model reply

WHY:
    AI attention is separate from human message persistence

HELD:
    generic dynamic message-action identity scheme

WHY:
    current accepted main supports stable list item identity;
    broader runtime action addressing is still under separate draft pressure
~~~

## Review test for another human or AI

Before implementing Conversation, a fresh worker should be able to answer all of these without
predecessor chat:

1. Why is Talk to Vex an entry into Conversation rather than the definition of the feature?
2. What is the difference between a stable thread and an immutable channel generation?
3. Why are recipientRefs not the same as witnessRefs?
4. Who owns current Family membership and speaker authority?
5. Where does durable conversation history live in remote-surface mode?
6. Why can human chat continue when the model is unavailable?
7. Why is a PR #82 Behavior Family not the same thing as Vex Family?
8. Why has no Conversation Behavior family been minted yet?
9. What runtime-payload pressure does Send introduce?
10. Why must generic diagnostics not persist private message text by default?
11. What does VexExecution own, and what does it explicitly not own?
12. Which federation facts are current portable source and which Android effects remain held?

If those answers are unclear, do not compensate by adding another manager, repository, state machine,
or abstraction. Re-enter the owner/source that is unclear.

## Next meaningful milestone

Do not split the next work into file-shaped commits.

The next commit should represent one architectural/product milestone:

> Form one synthetic, group-ready Android Conversation vertical slice that makes the timeline,
> composer, current audience and Home/AI availability perceivable, while pressure-testing private
> Send payload handling against the accepted Behavior/VexRuntime boundaries.

That milestone should include the source + focused proof needed to explain the result as one coherent
change.

Do not widen into real transport, model invocation, federation, or durable Android storage merely to
make the first surface look complete.

<!-- [VXG RealForever][VEXLIFE-ANDROID][CONVERSATION][FEATURE-HOME] -->
