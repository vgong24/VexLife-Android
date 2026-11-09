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
    Navigation / CurrentScreen

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

`detailsJson` is not a generic metadata bag. It may carry only bounded detail that the producer
already owns as part of that accepted/observed fact. It must not be used to copy dependency graphs,
execution identity, Blueprint topology, arbitrary object dumps, or expected future results into
Logger.

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

## Automatic routing versus explicit fact formation

Some boundaries already know a complete fact and therefore route it automatically.

`VexRuntime` owns the accepted UI/navigation boundary and automatically records facts such as:

~~~text
AppOpened
Clicked / Submitted
DynamicInteraction
Back / Dismissed
CurrentScreen after navigation mutation
settled pager Screen
~~~

A Screen or feature that calls the normal `VexRuntime.activate(...)`, `activateDynamic(...)`,
`submit(...)`, `goBack(...)`, or external-navigation surface should not emit another duplicate
interaction fact.

Other foundations deliberately do not infer Logger meaning from their method calls:

~~~text
VexFamily.activate(...)
VexMethod.invoke()
VexExecution.begin/observe(...)
VexPartner.Connection.receive(...)
VexCompoundRelay.processStates(...)
~~~

Those owners know semantic State, active custody, provider observations, or relationship current; they
do not necessarily know the truthful human/runtime action name to record. Automatically deriving
facts from callable names, class names, or reflected types would make chronology depend on naming and
would recreate a second semantic vocabulary inside Logger.

The accepted Family continuation supplies the mechanical seams without inventing facts:

~~~text
accepted Act
-> launchWith(root)
-> method(
       beforeOperation = { rightful owner may record EffectRequested },
       onResult = { rightful owner may record Success / Failure / Error meaning }
   )
~~~

The concrete realization boundary forms those facts because it knows the exact Control/Effect,
provider result meaning, and currentness policy. ThemeMode is the current complete example. A feature
may omit a semantic/runtime Logger event when no consumer or proof requires it; omission is not
permission to create a feature-local chronology root.

For process-lived Services/widgets, first resolve which canonical Logger lifetime should receive the
fact. Do not inject a session Logger into a longer-lived process owner by convenience, and do not mint
a process logger or feature sequence merely to avoid that lifetime question. A feature-local Logcat
trace may remain temporary diagnostic evidence, but it is not automatically accepted chronology.

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
Dark outside State observed
Dark presentation arrived
~~~

is different from:

~~~text
request Dark
Save succeeded
Dark outside State observed
Light presentation remained
~~~

A future debug surface may compare expected vs actual, but an expected destination/result must not be
stored as though it was already observed.

For navigation, production records only what is current at each boundary:

~~~text
DebugMenu Clicked
    current Screen = Home

navigation owner applies the request
    -> current navigation Screen becomes DebugMenu

DebugMenu CurrentScreen
    current Screen = DebugMenu
~~~

Chronology is sufficient for tests/debug projections to derive Home -> DebugMenu when that
interpretation is useful. Production does not store an expected destination or a redundant
before/after transition pair.

The navigation fact proves navigation-owner truth, not rendered pixels. A future presentation owner
may add an actual visual observation only when it can truthfully observe that separate fact.

Pager settlement likewise records only the Screen that is current when the settled callback fires;
the prior Screen remains derivable from chronology.

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

The current Room schema retains legacy physical columns such as sequence, duration,
screen-before/after and section. PR #209 does not perform a database migration merely to remove
those columns. Current production facts populate only the Screen coordinate that is current at the
fact boundary; before/after transition interpretation belongs downstream in test/debug projection.

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
