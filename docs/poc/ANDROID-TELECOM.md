# POC — Android Telecom / Call Control

`[VXG RealForever]`

## Why this branch exists

The desired experience is simple:

> When a real phone call is ringing or active, Vex can perceive the bounded call state and, when the
> user asks, accept, decline, or end the exact call without the model owning Android Telecom.

The experiment must determine the real platform role/effect boundary before product semantics are
built on top of it.

## Platform seam under investigation

The primary call-control path is the Android default-dialer / `InCallService` relationship.

`CallScreeningService` is a separate screening boundary. It may classify/allow/block incoming calls,
but it must not be treated as the call-answering owner.

Deprecated convenience APIs are not the architecture target merely because they can answer a call on
some API levels.

## Truth owners

~~~text
TelecomCapabilityState
  Unsupported
  DialerRoleNotHeld
  Ready

CallState
  Idle
  Ringing(callRef, callerProjection)
  Active(callRef)
  Disconnecting(callRef)
  Disconnected
~~~

The Android Telecom object remains inside the adapter. Higher layers receive a stable bounded call
reference plus the minimum user-facing projection needed for the decision.

## Available actions are derived, not registered forever

Examples:

~~~text
Ringing(callRef)
  -> Answer(callRef)
  -> Decline(callRef)

Active(callRef)
  -> End(callRef)

Idle
  -> no call-control actions
~~~

Every effect rechecks the exact call reference and current Telecom state immediately before acting.
A model response generated against an earlier Ringing state cannot answer a different/new call.

## Audio / companion relationship

This branch does not invent a universal audio-focus policy.

It should expose enough call-state truth for a later deterministic coordinator to decide that an
active/ringing call outranks ordinary media or companion speech. Navigation guidance remains its own
owner.

## First source slice

1. model dialer-role availability/current-holder state;
2. bind the minimum compliant `InCallService` surface;
3. project one ringing call into bounded Vex state;
4. derive Answer / Decline availability;
5. execute one typed action against the same call reference;
6. project the accepted resulting state;
7. revoke/change the dialer role and prove call-control availability disappears.

Do not add call recording, transcription, contact-history ingestion, call-log personalization, or
automatic answering to make the POC look richer.

## Proof

Physical-device proof is required for the material claim:

~~~text
real incoming test call
-> VexLife observes Ringing
-> Answer and Decline are state-valid
-> one chosen effect is performed
-> state updates from Android Telecom
-> role/capability removal makes the effect fail closed
~~~

The role grant itself remains a human Android consent/default-app action.

<!-- [VXG RealForever][VEXLIFE-ANDROID][POC][TELECOM] -->
