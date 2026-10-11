# POC — Android Messaging

`[VXG RealForever]`

## Why this branch exists

The product intention has two materially different messaging experiences:

> Vex can help form a message and hand it to the user's messaging app.

and, only when the platform/default-role contract genuinely permits it:

> Vex can receive/send SMS through a direct, stateful messaging capability.

Those must not be collapsed merely because both end with visible text.

## Two capability levels

### Compose-only baseline

Use the ordinary Android messaging intent boundary for a pre-addressed/pre-filled draft.

~~~text
MessagingCapability.ComposeOnly
-> ComposeMessage(recipient, body)
-> Android messaging app / user confirmation
~~~

This is useful without claiming broad SMS database or send authority.

### Direct messaging

Direct SMS read/receive/send is a restricted/default-handler capability and is admitted only after
the exact current role/permission state is observed.

~~~text
MessagingCapability.Direct
-> current bounded inbox/conversation projection
-> SendMessage(...)
-> ReceiveMessage(...)
~~~

Do not infer Direct from the app merely holding the Assistant role in source. The real device and
current permission/default-handler state must establish it.

## Truth owners

~~~text
MessagingCapabilityState
  Unsupported
  ComposeOnly
  DirectRoleOrPermissionMissing
  DirectReady

MessageOperationState
  Idle
  Pending(operationRef)
  Accepted(resultRef)
  Failed(resultRef)
~~~

Conversation/message data, when direct access is earned, belongs to a messaging owner. Compose UI,
Vex conversation history, Android diagnostics, and VexLogger do not become copies of the SMS source
of truth.

## Model boundary

The model may propose recipient + body, but the Android owner decides which effect is currently
available.

~~~text
ComposeOnly
  -> "send" means open a user-visible compose handoff

DirectReady
  -> direct SendMessage may be offered when product policy also permits it
~~~

A capability downgrade while the model is thinking must invalidate a previously advertised direct
send action.

Sensitive message bodies should not be copied into general VexLogger diagnostics by default.

## First source slice

1. implement/prove the compose-only intent path;
2. expose the distinction between compose-only and direct capability;
3. model direct capability without silently requesting restricted permissions;
4. only on an explicitly configured test device/default-role path, prove one direct send/receive
   interaction;
5. remove the direct role/permission and prove the system falls back rather than pretending direct
   access remains.

No RCS, notification scraping, Accessibility control, contact-graph personalization, or background
message summarization is required to answer this POC.

## Proof

~~~text
ComposeOnly:
typed request
-> ACTION_SENDTO / sms handoff
-> visible destination/body
-> user remains in control

DirectReady, when actually admitted:
test conversation
-> bounded incoming projection
-> exact typed send
-> observed result
-> revoke role/permission
-> direct action disappears/fails closed
~~~

<!-- [VXG RealForever][VEXLIFE-ANDROID][POC][MESSAGING] -->
