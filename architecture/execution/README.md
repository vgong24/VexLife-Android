# Vex Execution

[VXG RealForever]

## The question comes first

> I started asynchronous work. What is it called, who owns it, how do I stop it, how do I know it
> failed, and when is it allowed to restart?

That is the job of architecture/execution.

Relay owns current state relationships.
Execution owns coroutine lifetime mechanics.

~~~text
VexRelay
  current truth

VexExecutionScope
  named asynchronous work
  cancellation
  failure visibility
  sibling supervision
  explicit bounded restart
~~~

## Named work

Every launched execution has a VexExecutionName.

Examples:

~~~text
relay.event-screen.observe.screen
relay.event-screen.observe.items
home.connection.listener
voice.session.reader
server.local-api.listener
~~~

The execution scope exposes a StateFlow registry so diagnostics or a later Design Lab can answer:

~~~text
what is running?
which attempt is current?
did it fail?
was it restarted?
was it cancelled?
~~~

## Scope ownership

VexExecutionScope never uses GlobalScope.

It is always created as a child of an explicit owner scope.

Typical ownership:

~~~text
application
  -> application VexExecutionScope

session
  -> session VexExecutionScope

feature runtime
  -> feature VexExecutionScope
~~~

Koin may construct and retain those scope owners. Koin is composition, not execution semantics.

## Supervision

A VexExecutionScope uses SupervisorJob so one failed execution does not automatically kill unrelated siblings.

This is useful for Home connection listeners, local server listeners, device observers, and Relay dependency collectors.

## Cancellation

launch(...) returns VexExecutionHandle.

The handle supports:

~~~text
cancel(reason)
join()
state
~~~

Cancellation is never treated as ordinary failure and never triggers automatic restart.

## Restart

Restart is opt-in:

~~~kotlin
VexRestartPolicy.OnFailure(
    maxRestarts = 3,
    delayMillis = 1_000,
)
~~~

Default is Never.

This matters because:

~~~text
restart coroutine mechanism
!= retry semantic operation
!= resurrect durable work
~~~

A network listener may legitimately restart.
A user payment/action/message operation may require a completely different semantic retry contract.

## Relay dependency observation

Relay collectors are executions too.

Instead of VexCompoundRelay hiding List<Job>, dependency observation should launch named executions through an injected VexExecutionScope.

That lets us monitor:

~~~text
relay.event-screen.observe.screen
relay.event-screen.observe.items
relay.event-screen.observe.interaction
~~~

without making VexRelay itself the coroutine owner.

## Historical lineage

The exact old PenSDK class name has not been recovered from durable source.

The preserved VexLife Android architecture does clearly require:

~~~text
COROUTINE != OPERATION
JOB != DURABLE_IDENTITY
HAS_A_COROUTINE_SCOPE != OWNS_THE_SEMANTIC_OPERATION

every launched attempt:
  explicit owner
  parent
  lifetime
  cancellation path
  failure route

supervision local to FailureDomain
one semantic retry-policy owner
clean stop requires positive proof
~~~

This module is the first small reconstruction of that architecture category.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ARCHITECTURE][EXECUTION] -->
