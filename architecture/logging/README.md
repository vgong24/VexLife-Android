# VexLogger — chronological traceability + readable interpretation

[VXG RealForever]

## Human question

> What accepted or observed facts happened, in what order, and how can a human/debug surface interpret
> their stable coordinates without turning Logger into another semantic, UI-topology, execution, or
> persistence owner?

VexLogger is the one chronological traceability root.

~~~text
rightful owner accepts / observes fact
-> VexLogger chronology
-> downstream projections:
     Logcat
     LogHistory persistence
     Debug Log History
     later VexSync / VexBoard / server interpretation
~~~

Logger failure cannot rewrite the product fact it observed.

## Ownership

VexLogger owns:

~~~text
accepted / observed fact chronology
fact ordering
stable fact coordinates
diagnostic visibility when expected interpretation is missing
~~~

It does not own:

~~~text
Family State / Control / Act / Effect meaning
RelationshipId meaning
active Job custody
Blueprint placement
navigation/presentation truth
content/resource mapping
provider success policy
database/network delivery
~~~

Those owners form the facts. Logger records them.

## One chronology, many fact producers

The same Logger receives facts from different rightful boundaries:

~~~text
UI/runtime
    AppOpened
    Interaction
    DynamicInteraction
    Gesture
    Presentation / Screen Arrived

Family/semantic boundary
    ControlAccepted
    StateAdmitted

runtime/provider boundary
    EffectRequested
    EffectSucceeded
    EffectFailed

architecture diagnostics
    BlueprintMappingMissing
~~~

Rightful owners submit already-accepted facts through VexLogger.Sink. The Logger event envelope
carries only the fact coordinate, action, and bounded owner-formed detail; it does not infer meaning
from Kotlin callables or create another semantic hierarchy.

~~~text
semantic owner
    -> Event.Semantic(ControlAccepted / StateAdmitted)

runtime/provider owner
    -> Event.Runtime(EffectRequested / EffectSucceeded / EffectFailed)

-> one VexLogger chronology
~~~

There is no parallel FamilyLogger, ExecutionLogger, trace bus, completed-execution history, or
callable-derived operation identity. Pure semantic decision code such as VexFamily.act(...) does not
receive Logger.

## Stable coordinate -> readable fields

The stored interaction fact keeps its compact stable coordinate:

~~~text
subjectRef = element.debug-menu.body.dark-mode
~~~

Logger does not parse dot-separated text to rediscover architecture.

When the matching UIBlueprintRegistry is available:

~~~text
subjectRef
-> UIBlueprintRegistry
-> owner Screen
-> parent / placement path
-> element kind
-> declared UI intent
~~~

The Debug Log History surface derives fields such as:

~~~text
fact.kind = Interaction
fact.action = Clicked
source.ref = element.debug-menu.body.dark-mode
source.resolution = RESOLVED
source.screen = screen.debug-menu
source.kind = Toggle
source.path = element.debug-menu.body -> element.debug-menu.body.dark-mode
source.intent = Invoke(action.toggle-dark-mode)
~~~

Those fields are a read-time interpretation of the current matching map, not a second metadata
registry copied into every log record.

## Static vs runtime UI coordinates

Two lawful element.* shapes exist.

### Static Blueprint element

~~~text
registered elementId
-> registry resolves structure
-> RESOLVED
~~~

If a normal static interaction uses an element.* ref that the supplied registry cannot resolve:

~~~text
Interaction
-> Diagnostic / BlueprintMappingMissing
~~~

The missing expected interpretation becomes observable instead of silently degrading to unknown.

### Intentional runtime instance

Repeated provider/domain data cannot all be permanent static Blueprint members.

Examples include:

~~~text
one Events row
one VexStream track row/menu occurrence
one dynamic collection/queue instance
~~~

Those facts enter through recordRuntimeInteractionRef(...) and use:

~~~text
kind = DynamicInteraction
resolution = RUNTIME_ELEMENT
~~~

A lawful runtime coordinate is therefore not misreported as missing Blueprint architecture.

Where a runtime instance also has a registered static action element, that static declaration remains
the owner of action/placement meaning; the runtime coordinate carries only the exact presented
instance identity.

## Expectation is not observation

Production chronology records accepted/observed facts.

~~~text
request Dark
Save succeeded
Dark presentation arrived
~~~

is different from:

~~~text
request Dark
Save succeeded
Light presentation remained
~~~

A future debug surface may compare expected vs actual, but an expected destination/result must not be
stored as though it was already observed.

For navigation, production records the request and observation separately:

~~~text
DebugMenu Clicked
    screenBefore = Home
    screenAfter  = null

navigation owner applies the request
    -> current Screen actually becomes DebugMenu

DebugMenu Arrived
    screenBefore = Home
    screenAfter  = DebugMenu
~~~

Pager gestures may carry before/after on the gesture record because the callback fires only after the
pager has actually settled. Intended targets calculated before mutation do not qualify.

## Chronology before correlation identity

Prefer:

~~~text
rightful semantic identity
+ RelationshipId while active
+ ordered Logger facts
~~~

before inventing another occurrence/correlation ID.

The actual Job is the runtime occurrence while active. Completed history belongs here, not in
VexExecution.

## Persistence and projections

The current app projects accepted VexLogRecord values downstream:

~~~text
VexLogger
-> LogHistoryPersistenceQueue
-> LogHistoryRepository
-> Room / in-memory implementation

VexLogger
-> VexLogcat

VexLogger + matching Blueprint registry
-> structured Debug Log History
~~~

Persistence/Logcat/debug failure is non-authoritative.

The current Room record retains historical fields such as sequence, duration, screen-before/after and
section. PR #209 does not perform a database migration merely to remove legacy columns. The stable
architecture remains: primary accepted/observed facts first; derived narration should not become new
upstream truth simply because persistence currently stores it.

## Form submissions

Draft typing is not logged.

An explicit submit boundary may carry one immutable submitted snapshot:

~~~text
Save Submitted
{
  "element.user-profile.body.profile.display-name": "Victor"
}
~~~

Stable element refs remain the keys instead of localized labels.

## Session boundary

VexLifeSession owns one live Logger for the current in-process app session. Android configuration
recreation reuses that session through VexLifeViewModel; it does not create a second AppOpened merely
because Compose rebuilt.

Durable history remains a downstream repository projection.

## Proof

Focused Logger source tests protect:

- ordinary chronological UI history;
- downstream observer failure isolation;
- Blueprint field resolution from stable IDs;
- missing static mapping diagnostics;
- runtime-element distinction;
- structured semantic/runtime events entering the same chronology.

Repository-owned Gradle/JVM proof is still required on the exact PR #209 candidate before this
refinement may claim proven integration.

<!-- [VXG RealForever][VEXLIFE-ANDROID][LOGGING] -->
