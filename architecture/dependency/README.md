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

## Current process / Service composition pressure

The current background-Service convergence uses the existing parent-reuse law directly:

~~~text
VexApplication dependency lifetime
    PlaybackFamily
    Navigation process truth
    BackgroundServiceRelay

PlaybackService composition
    -> reuses the exact parent PlaybackFamily
    -> resolves the exact parent BackgroundServiceRelay
    -> owns Media3/Player realization privately

NavigationService composition
    -> reuses the exact parent BackgroundServiceRelay
    -> owns location/speech realization privately
~~~

The shared parent object is not copied into each Service. A Service keeps its concrete behavior alive;
the Family remains the semantic State owner and the Relay remains the relationship owner.

The dependency foundation now makes that ancestry compiler-visible:

~~~text
VexApplicationScope
├── VexSessionScope
├── VexServiceScope<VexStreamPlaybackService>
└── VexServiceScope<VexMapsNavigationService>
~~~

The bindings are structural:

~~~text
VexDependencyProvider<VexApplicationScope, PlaybackFamily>
VexDependencyProvider<VexSessionScope, EventsConvergenceRelay>
VexDependencyProvider<VexServiceScope<VexStreamPlaybackService>, VexExecutionRoot>
~~~

`VexDependencyModule<Scope>` accepts declarations for one exact Scope, and
`VexDependencyFactory<Scope>.child(...)` accepts only a
`VexChildDependencyScope<Scope>`. A Service sibling therefore cannot be silently presented
as a Session dependency merely because the object is constructible.

The heterogeneous traversal common type is `VexDependencyNode`, not `Any`/star-projected dependency
declarations. Value storage inside the factory remains private implementation machinery; the
source-visible topology retains Scope + Value types.

## Typed scope topology

Scope is a dependency relationship, not a naming convention:

~~~text
A DEPENDENCY'S SCOPE
IS PART OF ITS TYPE.

A CHILD FACTORY'S PARENT
IS PART OF ITS TYPE RELATIONSHIP.
~~~

Android realizes the current scope relationships:

~~~text
VexApplicationScope
    process lifetime

VexSessionScope : VexChildDependencyScope<VexApplicationScope>
    retained VexLife session lifetime

VexServiceScope<ServiceType : android.app.Service>
    sibling Service child lifetime of VexApplicationScope
~~~

The concrete Service type remains in `VexServiceScope<ServiceType>` so Playback and Navigation
are not interchangeable siblings in source.

This type topology does not make Android framework classes semantic owners. It only makes the
dependency lifetime relationship locally traceable.

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
    authors State / Control / Act / Begin / Effect relationship

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
