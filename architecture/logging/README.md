# VexLogger — chronology and observed-fact boundary

[VXG RealForever]

> **PR #206 review candidate — VexLogger is part of the architecture being reviewed**
>
> This PR does not treat the existing logger implementation as pre-vetted infrastructure to reuse
> unchanged. VexLogger is included so Family, Connection, Execution, chronology, debug expectation,
> persistence, and human projection can be reviewed as one end-to-end responsibility graph.

## Whole architecture

~~~text
Control / external fact
-> rightful semantic/runtime owner
-> accepted decision / observed fact

VexFamily
    owns current semantic State
    owns Control -> Act meaning
    does not log, persist or execute by itself

Connection / concrete runtime owner
    realizes Effect
    interprets provider/runtime facts
    owns stale/currentness decisions
    forms the rightful execution RelationshipId when work is required

VexExecution
    owns present coroutine custody only
    RelationshipId -> active occurrence(s)
    overlap policy / cancellation / joining
    not historical sequence

VexLogger
    records accepted / observed facts in chronology
    does not decide whether the fact is true
    does not own runtime custody
    does not create another semantic State

LogHistory / Logcat / VexBoard / debug
    projections of logger + semantic/runtime truth
    never become another truth owner
~~~

The architecture should be readable from those responsibilities without reconstructing an event's
parent from implementation-only IDs.

## Core logging law — record facts once

The logger records facts that actually happened.

Examples:

~~~text
Screen A arrived
Button X clicked
Effect Y attempted
Effect Y succeeded
Screen A disappeared
Screen B arrived
Song A ended
AutoAdvance selected Song B
Song B started
~~~

The log does not need to additionally announce every relationship that can already be derived from
adjacent facts.

~~~text
Screen A disappeared
Screen B arrived

already reveals:
A -> B
~~~

Do not also persist:

~~~text
Transitioned from A to B
~~~

unless that transition is itself an independent semantic action/fact that cannot be recovered from
the surrounding records.

Likewise:

~~~text
Song A ended
Song B started
~~~

already reveals temporal succession.

If an actual decision exists between them, **that decision is a fact and should be logged**:

~~~text
Song A ended
AutoAdvance selected Song B
Song B started
~~~

This preserves causality without duplicating narration.

## Arrival / disappearance are separate facts

Screen/lifecycle chronology should prefer explicit facts:

~~~text
screen.home Arrived
screen.home Disappeared
screen.debug-menu Arrived
~~~

rather than attaching both "before" and "after" to every unrelated interaction record and later
reverse-constructing the visit tree.

The runtime/navigation owner remains the authority for which Screen is current. Logger records the
accepted arrival/disappearance observations; it does not become navigation state.

The same principle applies to other stateful experiences where the boundaries are independently
meaningful:

~~~text
playback started
playback ended

service connected
service disconnected

conversation opened
conversation closed
~~~

## Actions and results are independently meaningful

An action is not duplicated merely because a later result exists.

~~~text
Save clicked/submitted
Save attempted
Save succeeded
State arrived / UI showed result
~~~

Each line answers a distinct question.

A failure is likewise a fact:

~~~text
Save attempted
Save failed
Failure presentation arrived
~~~

The logger should not collapse those into a synthetic "from -> to" event that hides which layer made
the decision.

## Chronology belongs to Logger, not Execution

VexLogger owns event ordering.

That does **not** mean a monotonically increasing number is semantic identity.

A storage implementation may keep a local ordinal, timestamp, database key, or other deterministic
ordering mechanic where required. Those mechanics are not reasons for VexExecution to expose a
process-global InstanceId.

~~~text
LOGGER ORDER
!= EXECUTION IDENTITY
~~~

If two genuinely concurrent occurrences of the same RelationshipId must be distinguished for deep
debugging, a logger/debug span or correlation token may be earned for that diagnostic need.

~~~text
RelationshipId
    architectural meaning

debug/log correlation
    optional diagnostic mechanics
~~~

Do not promote the debug token into reusable execution ontology merely because it helps one
diagnostic projection.

## Human readability comes from semantic identity

A human surface should prefer:

~~~text
ThemeMode
└── Persistence
    └── Running / Succeeded
~~~

over:

~~~text
OwnerToken ...
CoordinationKey ...
SourceRef com.foo.PersistenceExecution.save
InstanceId 184
~~~

The latter may contain useful developer detail, but it is not the primary human meaning.

VexBoard and readable history should project the semantic RelationshipId / action / result labels
already owned by the architecture. Do not manufacture a second "human readable" identity from
callable names when the relationship itself is readable.

## Production facts versus debug expectations

Production logging records what was accepted/observed.

Debugging may additionally compare that fact stream with an expectation:

~~~text
actual:
    Control.Select(Dark)

debug expectation:
    expect Effect.PersistSelection(Dark)
    expect State.Dark / presentation Dark

actual:
    persistence succeeded
    State.Dark arrived
~~~

A mismatch can then be surfaced:

~~~text
EXPECTED  State.Dark
OBSERVED  State.Light
~~~

Expectation is diagnostic metadata, not production truth.

~~~text
PRODUCTION
    observed facts

DEBUG
    observed facts
    + optional expectations
    + discrepancy detection
~~~

This keeps speculative/revision-style bookkeeping out of the production architecture while still
allowing deep inconsistency detection.

## Failure isolation

Logging is observational.

~~~text
accepted product/runtime fact
-> logger attempt
-> logger sink/projection failure

must not become:
product/runtime fact rejected
~~~

Logcat, Room/LogHistory persistence, VexBoard projection, export, and debug expectation processing are
downstream surfaces. Failure in one must not rewrite the already-accepted fact or suppress another
projection.

## Persistence and projection

~~~text
VexLogger fact stream
├── in-session readable history
├── LogHistoryRepository / Room
├── Android Logcat
├── VexBoard projection
└── debug expectation/discrepancy tooling
~~~

Those are projections/retention mechanisms.

They do not own navigation, Family State, provider truth, execution custody, or the semantic identity
that generated the event.

## Current implementation gap being reopened by this PR

The current implementation predates the whole-architecture review above.

Today VexLogRecord contains fields such as:

~~~text
sequence
durationSincePreviousSeconds
screenBeforeId
screenAfterId
section
~~~

and VexRuntime commonly records an interaction together with before/after Screen IDs before applying
navigation. VexLogger.screenScopes then derives visit ranges from those transition-shaped records.

That source is **implementation evidence**, not automatically the mastered target.

Before fresh human vetting, the implementation should be reconciled against the simpler law:

~~~text
log each independently meaningful fact once
derive adjacency/transition when the fact stream already proves it
keep chronology in Logger
keep current custody in Execution
keep semantic truth in Family / rightful runtime owner
keep expectations in Debug
~~~

In particular, this PR must decide which current fields are still required as storage/projection
mechanics and which are duplicate architecture that should be removed rather than renamed.

## Current rooted structured event source

The existing VexLogger.Event / Sink tree remains useful evidence that structured accepted facts can
enter one logger root without every foundation inventing a new logger.

However, the current Family-specific event vocabulary is also under review because the mastered
Direct Family no longer owns logging/runtime mechanics. A concrete Connection/runtime owner may be
the rightful producer of a runtime fact even when that fact concerns Family State/Control meaning.

Do not preserve an Event namespace merely because it already exists. Preserve the fact and its
rightful producer.

## Human-vetting questions

Before marking VexLogger architecture as vetted, review:

~~~text
What facts are truly independent events?

Which "from -> to" fields can be derived from adjacent records?

Does any production record contain expectation rather than observation?

Does execution duplicate logger chronology?

Does logging duplicate semantic State?

Does VexBoard project semantic labels, or implementation IDs?

Can logging/projection failure change product behavior?

Does a current storage field exist only because an older projection needed it?
~~~

<!-- [VXG RealForever][VEXLOGGER][OBSERVED-FACTS][CHRONOLOGY][PR206-REVIEW-CANDIDATE] -->
