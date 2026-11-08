# VexDependencyFactory — readable construction topology

[VXG RealForever]

## Human question

How do live objects get built in prerequisite order, reused at the rightful lifetime, and delivered
explicitly to consumers without hiding architecture behind reflection/container lookup?

The foundation is deliberately small:

~~~text
named provider declaration
    -> explicit prerequisite provider declarations

VexDependencyFactory
    -> recursively resolves prerequisites first
    -> builds requested provider
    -> memoizes one instance for this factory lifetime

child factory
    -> reuses parent-lifetime providers
    -> owns only its newly declared providers

consumer
    -> receives concrete object explicitly
~~~

## App-scope background coordination

Current sustained/background coordination uses the ordinary dependency-lifetime law directly:

~~~text
VexApplication lifetime
    Process VexExecutionRoot
    VexStreamPlaybackFamily
    VexNavigationSession
    BackgroundServiceRelay
~~~

VexApplication resolves the Relay once as an explicit process root. The Relay then observes the
rightful current StateFlows through the process VexExecutionRoot:

~~~text
PlaybackFamily.states --------\
                               +-> BackgroundServiceRelay
NavigationSession.states -----/
                                      |
                                      v
                         coherent app-scope current truth
                         - exact Playback State
                         - compact Navigation background projection
~~~

The Relay carries typed current meaning without hauling unrelated heavyweight source payloads through
every app-scope update. Navigation contributes phase / route identity + label / location disposition /
pending reroute ref / latest issued cue rather than full route geometry. The Relay does not maintain
Service IDs, an "activeServices" enum/set, or a Room-backed synchronization bus.

A concrete Android Service may resolve the same memoized Relay when it has a real cross-domain
decision to make, then observe/filter only the State it needs. Merely existing as a Service is not a
reason to depend on the Relay.

Dependency construction still owns only construction/reuse. The Relay owns live convergence;
individual Services own their concrete sustained realization; VexExecution owns coroutine Job
custody.

## Identity

Provider declaration **object identity** is the construction coordinate.

Provider class/name is only a readable label.

~~~text
PROVIDER_LABEL != PROVIDER_IDENTITY
DEPENDENCY_IDENTITY != EXECUTION_RELATIONSHIP_ID
DEPENDENCY_IDENTITY != LOGGER_TRACE_ID
~~~

This permits two differently configured declarations to use the same implementation class without
inventing a global qualifier namespace.

## Lazy construction

The factory does not eagerly instantiate every registered provider.

~~~text
resolve requested root
-> resolve only its prerequisite closure
-> memoize each built provider
~~~

This lets application startup request only the roots it actually needs while preserving deterministic
dependency-first construction.

## Access boundary

The factory is a composition-root tool, not a service locator.

~~~text
VexApplication / ViewModel / explicit composition owner
    may resolve roots

provider create(...)
    may require only declared prerequisites

ordinary feature/UI consumer
    receives concrete dependency
    does not call global get<T>()
~~~

## Family composition without a wrapper node

The dependency system constructs and reuses the objects that already own meaning or lifetime. It
does not create a peer object merely to bridge Family to VexExecution.

ThemeMode currently composes:

~~~text
ThemeModeStore provider      # application lifetime
VexExecutionRoot provider    # session lifetime
VexLogger provider           # session lifetime
        \
         -> ThemeModeFamily provider
              starts Store observation through direct VexExecution.observe

AppActions provider
    receives ThemeModeFamily + ThemeModeStore + VexExecutionRoot + VexLogger
    invokes the Family-authored Act consequence directly
~~~

Permanent:

~~~text
CONSTRUCT EXPLICITLY; REUSE AT THE RIGHTFUL LIFETIME; NEVER GUESS.

dependency graph
    constructs / memoizes

Family
    authors State / Control / Act / Effect + Approach relationship

VexExecution
    owns active Job custody

dependency identity != execution RelationshipId
reflection/name/package scanning = not architecture authority
~~~

A provider node is earned by a real lifetime/construction relationship, not because two existing
objects need a named bridge between them.

## Provider declaration boilerplate

Provider declaration object identity remains the construction coordinate, but a named provider
subclass is not itself semantic architecture. A future refinement may expose typed provider-builder
syntax that preserves explicit prerequisites/object identity while eliminating one-off
<Thing>Provider subclasses whose only job is boilerplate.

Do not trade that boilerplate reduction for ambient get<T>(), reflection, constructor scanning, or a
global qualifier namespace.

## Failure

The factory fails closed on:

- duplicate registration of the same provider declaration;
- child shadowing of a parent provider declaration;
- unregistered provider resolution;
- dependency cycles;
- provider construction requesting an undeclared prerequisite.

No Logger, Family, Execution, UI, or provider-specific semantic meaning is owned here.

<!-- [VXG RealForever][VEXARCHITECTURE][DEPENDENCY][READABLE-CONSTRUCTION] -->
