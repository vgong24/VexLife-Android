# POC — Android Assistant Role + Agent Tool Surface

`[VXG RealForever]`

## Why this branch exists

The question is not "how do we put an LLM in an Android Service?"

The question is:

> What is the smallest Android-owned boundary that lets Vex participate as the device assistant,
> stay available without making the model itself a long-lived Android runtime, and discover/execute
> app capabilities only when the platform says those capabilities are actually available?

This is an experiment. It does not change the accepted VexLife Android foundation merely by existing.

## Platform seam under investigation

Primary Android surfaces:

- `RoleManager.ROLE_ASSISTANT`;
- a lightweight `VoiceInteractionService` / session boundary;
- current AppFunctions discovery/execution where the exact device/runtime permits it.

The selected assistant service must remain lightweight. Model/session UI and heavier work belong behind
a separately owned session/runtime boundary.

AppFunctions are treated as an experimental platform integration surface, not as proof that arbitrary
cross-app actions are available. Exact device/runtime permission and allowlist behavior must be
observed before an action is advertised to Vex.

## Truth owners

Do not collapse these states:

~~~text
AssistantRoleState
  Unavailable
  AvailableNotHeld
  Held

VoiceInteractionState
  Inactive
  Starting
  Active
  Stopping
  Failed

AppFunctionCapability
  Unsupported
  PermissionUnavailable
  DiscoveryUnavailable
  Discoverable
  Executable
~~~

Role state does not imply function execution capability.

## Model boundary

The model receives only an approved tool projection derived from current capability state.

~~~text
platform observation
-> Android capability owner
-> currently valid tool/action projection
-> Vex reasoning
-> typed action request
-> Android owner revalidates preconditions
-> effect
-> accepted result
~~~

A stale model/tool catalog must not authorize an effect after the role, permission, package, or
function availability changes.

## First source slice

Prefer the smallest vertical proof:

1. expose current assistant-role state;
2. request the role only through the Android-owned user-consent flow;
3. bind a minimal voice-interaction service/session;
4. enumerate a bounded AppFunctions test surface when supported;
5. execute one harmless test function when the exact platform grants it;
6. revoke/remove the role and prove the projected capability disappears.

Do not add SMS, call answering, navigation, media playback, or model transport to this branch merely
because the assistant role touches them elsewhere.

## Proof

Source/unit proof should establish state transitions and stale-action rejection.

Physical-device proof should establish:

~~~text
role available
-> user grants assistant role
-> Android reports VexLife as holder
-> voice/session boundary can activate
-> current app-function capability is observed
-> one safe function is discovered/executed OR exact platform denial is preserved
-> role is removed
-> dependent capability/action projection fails closed
~~~

A denial is useful POC evidence. It must not be bypassed with Accessibility, notification scraping,
or another unrelated privilege merely to make the demo work.

<!-- [VXG RealForever][VEXLIFE-ANDROID][POC][ASSISTANT-ROLE] -->
