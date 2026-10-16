# Conversation — Current Design

[VXG RealForever]

Feature ref: `feature.vexlife.conversation`  
Current human name: **Conversation**  
Historical/search name: **VexConversation**  
Last meaningfully updated: **2026-10-15T23:41-07:00**  
Current design record: `records/2026-10-15-001-visible-conversation-lab.md`

## Role of this file

This is the **current intended experiential design** for Conversation.

It is not the source/implementation contract. For what is actually accepted in code now, start at
`../README.md` and `VexConversationDocs`.

~~~text
../README.md
    what source truth exists now

design/README.md
    what experience we currently intend to build next

design/records/*
    how meaningful design refinements changed that intention
~~~

When an older design record disagrees with this file, this current synthesis wins. When accepted
implementation has moved beyond this file, accepted source + the feature README win and this design
must be currentized.

## Current intention

Make Conversation perceptible as a living Android feature rather than an invisible state model.

The first experiential milestone should let Victor:

~~~text
Home
-> Conversations
-> Family room
-> type an ordinary message
-> see Victor-authored truth
-> see simulated remote truth arrive through the provider side
~~~

and use Debug Menu as a controlled laboratory for participant/currentness/provider conditions.

The lab exists because a phone cannot presently host the real remote humans/Home/model environment.
It simulates those external conditions without becoming a second Conversation architecture.

## Product versus laboratory

Permanent shape:

~~~text
CONVERSATION LAB
    simulates external/provider conditions
        participant joins/leaves
        remote participant speaks
        Vex availability
        Home availability
        currentness/generation changes
            ↓

VexFamilyConversationRepository / Conversation state
            ↓

normal Conversation screen
    renders the same product state a real provider would expose
~~~

When Victor sends an ordinary message:

~~~text
Conversation composer
-> VexFamilyConversationRepository.send(...)
-> provider admits/rejects
-> Conversation state changes
-> UI reacts
~~~

The Debug Menu must never directly paint fake chat bubbles.

~~~text
DEBUG_SIMULATION != PRODUCT_PRESENTATION_SHORTCUT
~~~

## Feature surfaces

### Home

Conversation should become a first-class Vex-system destination rather than making `Talk to Vex`
own the whole domain.

Candidate direction:

~~~text
Vex systems

[ Conversations ]
[ VexMaps      ]
[ Talk to Vex  ]
~~~

`Talk to Vex` remains one entry/selected conversation, not the universal Conversation owner.

### Conversation list

First useful shape:

~~~text
Conversations

Family
Victor · Maya · Vex
"Want me to grab dinner?"

Victor + Vex
"Can you help with..."
~~~

### Conversation room

First useful shape:

~~~text
Family
Victor · Maya · Vex

Victor
Hey are we still going?

Maya
Yep, leaving now

Vex
I can keep track of the plan.

[ Type a message... ] [ Send ]
~~~

When simulation is active, show a bounded developer-facing signal such as:

~~~text
Conversation Lab active
3 simulated participants · Auto mirror: Maya
~~~

The signal explains the environment; it does not change Conversation semantics.

## Conversation Lab

The reusable Debug Menu supplies the doorway. Conversation owns the lab's domain-specific conditions.

First useful controls:

~~~text
Simulation mode              ON/OFF

Room
    Reset lab
    Seed Family room

Participants
    Add Maya
    Add Alex
    Add Vex
    Remove selected

Mock speaker
    Maya / Alex / Vex / current participant

Response behavior
    Auto mirror ON/OFF
    Vex available ON/OFF
    Home available ON/OFF

Currentness
    Advance membership generation
    Prepare stale-send case
    Retry last Send
~~~

These controls publish simulated provider truth through the same feature repository/state boundary.
They do not create a second screen-local truth model.

## Debug-only message injection

When Conversation Lab mode is enabled, a debug parser may accept:

~~~text
[Maya]{test message}
~~~

Meaning:

~~~text
simulate one provider-authored external message
speaker = Maya
content = "test message"
~~~

This syntax is **not** a Conversation protocol and is never sent to the production provider.

Ordinary input remains ordinary product Send:

~~~text
hello
-> authenticated Victor Send
~~~

Unknown simulated speakers fail visibly rather than minting identity.

## Mirror mode

Mirror mode provides model-free round-trip pressure.

~~~text
mock responder = Maya
auto mirror = ON

Victor sends:
    Are you coming?

simulated external/provider truth publishes:
    Maya: You said: "Are you coming?"
~~~

The response enters through simulated remote/provider publication. The product Send path must not
manufacture Maya's message.

## Invite / membership simulation

The lab should let Victor exercise human membership without pretending Conversation owns Family
lifecycle truth.

First direction:

~~~text
Invite "Maya"

AUTO ACCEPT
    simulated external acceptance
    -> Family generation advances
    -> Maya enters current audience

MANUAL
    pending synthetic lifecycle condition
    -> Accept as Maya
    -> Decline as Maya
~~~

Pending invitation state belongs to the Family/lab lifecycle fixture until a real producer projection
is consumed. It must not be inserted into `VexConversationState` merely because the UI needs it.

A membership change is not a Conversation message:

~~~text
ADD/REMOVE MEMBER != SYNTHETIC "Maya joined" MESSAGE
~~~

A future visible divider may be derived from exact generation/segment truth.

## Identity and human labels

The lab may let a human type a friendly label such as `Maya`, but Conversation still consumes stable
identity.

~~~text
human label
    Maya

synthetic principal identity
    person.debug.maya
~~~

Presentation may map that identity back to `Maya`.

Do not add display name/avatar/relationship label back into `VexConversationParticipant` merely for
the lab.

## First scenario matrix

| Ref | Scenario | Visible truth to pressure |
|---|---|---|
| C00 | Empty | No room/messages are invented |
| C01 | Direct | Victor + Vex generic Conversation renders without Family leakage |
| C02 | Family room | Victor + Maya + Vex current audience is visible |
| C03 | Add participant | Audience changes and Family generation/channel advances |
| C04 | Remote speech | `[Maya]{test message}` renders as Maya |
| C05 | Victor Send | Victor is speaker; provider derives current recipients |
| C06 | Mirror response | Remote mirror arrives through simulated provider truth |
| C07 | Multi-human | 3–5 participants remain readable and separately attributed |
| C08 | Member leaves | Removed member leaves current audience; old messages stay attributable |
| C09 | Generation boundary | Successor channels remain one stable thread |
| C10 | Stale Send | Currentness change rejects old request; no ghost bubble |
| C11 | Exact retry | Same accepted retry produces no duplicate |
| C12 | Changed retry | Changed meaning under same scoped identity conflicts |
| C13 | Vex unavailable | Human chat stays usable while Companion is unavailable |
| C14 | Home unavailable | Existing projection may remain visible; Send fails closed |
| C15 | Unknown speaker | Lab rejects unknown identity rather than minting a member |
| C16 | Reset/reseed | Deterministic starting scenario can be reproduced |

These are design scenarios. They become Journey/device obligations only as visible product controls
are actually admitted.

## Current implementation strategy

First vertical slice:

~~~text
one shared Conversation repository instance
+ Conversation screen
+ Victor composer
+ remote message injection
+ auto mirror
+ add/remove participant
+ Debug Menu Conversation Lab controls
~~~

The same repository instance must be owned by the app/session composition graph.

~~~text
NO_DEBUG_SINGLETON
NO_COMPOSE_OWNED_REPOSITORY
~~~

Today:

~~~text
Conversation UI
-> VexFamilyConversationRepository
-> InMemoryVexFamilyConversationRepository + Conversation Lab
~~~

Later:

~~~text
Conversation UI
-> same feature contract
-> Home-backed Family adapter
~~~

The product surface should not require conceptual replacement when the real provider arrives.

## Meaningful progression to this design

1. Generic Conversation was separated from Vex Family-specific currentness.
2. Sequence, generation and idempotency semantics were aligned to the real VF-02C producer.
3. Historical audience/witness truth became explicit through Family channel segments.
4. The foundation received exact source/build proof and merged to Android main.
5. VexLife session/server authority progressed to accepted lived proof; authenticated cross-generation
   history exposure remains the real producer gap.
6. The feature focus moved from invisible architecture to a visible, reactive Conversation Lab that
   pressures the same repository/state path future production will consume.

## Current holds

~~~text
HELD
    real Home-backed adapter
WHY
    authenticated cross-generation history is not yet exposed through the server boundary

HELD
    generic/direct Send
WHY
    no real direct write provider contract has earned it

HELD
    production app/Journey integration
WHY
    integrate only through current shared graph ownership; do not collide blindly

HELD
    model-backed device response
WHY
    the first lived UI pressure can be satisfied with deterministic external mock truth
~~~

## Next experiential slice

~~~text
Home -> Conversations
Conversation room + composer
Debug Menu -> Conversation Lab
seed Family room
add/remove participant
debug remote speech
auto mirror
Vex/Home availability controls
~~~

Then use the scenarios above to decide what additional behavior is actually missing before adding
another architecture layer.

<!-- [VXG RealForever][feature.vexlife.conversation][CURRENT-DESIGN] -->
