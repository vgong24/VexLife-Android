# Conversation design record — Visible reactive Conversation Lab

[VXG RealForever]

Feature ref: `feature.vexlife.conversation`  
Recorded at: **2026-10-15T23:41-07:00**  
Record ref: `design.feature.vexlife.conversation.2026-10-15.001`

## Discussion / pressure

The merged Conversation foundation had meaningful source and tests, but the feature was not yet
perceptible in the running app. Victor could see Home and Debug Menu, but could not experience the
Conversation state model in action.

The design pressure was:

> Make the architecture visible and breakable by hand before a real remote Home/model/human provider
> is available.

## Prior design

~~~text
Conversation foundation
    generic + Family-bound state/contracts
    in-memory provider pressure double
    currentness/idempotency/history semantics
    no production Screen
    no Conversation Lab
~~~

The Debug Menu already had a useful precedent: Events Simulator creates synthetic external conditions
through explicit actions rather than directly painting an Events result.

## New understanding

A useful Conversation mock should not be a fake UI.

~~~text
Debug Menu / feature lab
-> publish simulated external/provider conditions
-> normal feature repository/state changes
-> normal product screen reacts
~~~

And product actions must remain product actions:

~~~text
Victor types ordinary content
-> real feature Send contract
-> provider admission
-> state
-> UI
~~~

This lets the same visible screen first consume an in-memory provider and later a Home-backed
provider without changing the conceptual interaction model.

## Decisions crystallized

### 1. Debug Menu is a doorway, not Conversation owner

Conversation-specific scenario semantics stay feature-scoped. Debug Menu may expose controls that
invoke the feature lab, but it must not own participants, Family generation, message truth, or a
second Conversation state store.

### 2. Simulated remote speech enters through provider truth

Debug-only syntax such as `[Maya]{test message}` means publish one external Maya-authored message
while Lab mode is active. It must not become production protocol syntax.

### 3. Mirror mode replaces the missing remote/model side

Victor sends ordinary content through the real feature Send path. A selected simulated participant
responds by mirroring that content through external/provider publication.

### 4. Membership is state/lifecycle truth, not a fake chat message

Add/remove/invite scenarios advance the Family fixture/currentness shape. Do not create synthetic
Conversation messages saying a participant joined or left merely to make the UI easy.

### 5. Human labels stay outside Conversation identity

The lab can map `Maya` to a synthetic stable principal such as `person.debug.maya`, but display
metadata does not move into generic Conversation participant truth.

### 6. The lab must expose failure/currentness cases

The lived practicum deliberately pressures stale Send, exact retry, changed retry, Companion
unavailability, Home unavailability and unknown speaker rejection—not just the happy path.

## Scenario set formed in this refinement

~~~text
C00 Empty
C01 Direct
C02 Family room
C03 Add participant
C04 Remote speech
C05 Victor Send
C06 Mirror response
C07 Multi-human
C08 Member leaves
C09 Generation boundary
C10 Stale Send
C11 Exact retry
C12 Changed retry
C13 Vex unavailable
C14 Home unavailable
C15 Unknown speaker
C16 Reset/reseed
~~~

The current design synthesis owns the latest details for these cases.

## What remains open

- exact screen/component design after first render;
- whether invite pending/accept/decline belongs in the first slice or second;
- final product name if Conversation is later renamed;
- real authenticated cross-generation history adapter;
- when generic/direct Send is earned;
- which scenario transitions warrant Journey/device proof.

## Current design consequence

The next implementation should build one visible Conversation room plus Conversation Lab controls
against one app/session-owned in-memory repository instance.

Do not add another generic architecture layer before that lived pressure exists.

See current synthesis: `../README.md`

<!-- [VXG RealForever][feature.vexlife.conversation][DESIGN-RECORD-001] -->
