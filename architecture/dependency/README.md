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

## Typed Family composition

Repeated Family construction should not require every feature author to remember and duplicate the
same provider scaffolding.

The intended direction is:

~~~text
one explicit typed Family binding
    -> exact Family
    -> exact Family-facing Store when outside truth exists
    -> feature-specific Store prerequisites

architecture derives standard mechanics
    -> rightful VexExecutionRoot
    -> VexLogger/Sink
    -> dependency-first construction
    -> memoization/lifetime reuse
    -> Family/runtime kinship

ordinary feature author
    -> declares the truthful feature-specific relationship once
    -> does not hand-author ThemeModeFamilyProvider + ThemeModeRuntimeProvider + repeated standard roots
~~~

Permanent:

~~~text
DECLARE ONCE; DERIVE MECHANICALLY; NEVER GUESS.

automatic construction = allowed
automatic semantic inference = not allowed
reflection/name/package scanning = not architecture authority
~~~

A Store is the Family-facing outside-truth capability. It may be temporary, local, remote, service,
repository, platform, or a composition of those mechanics. Store is not synonymous with persistence.

The dependency system may supply standard runtime architecture dependencies automatically only after
the typed Family binding establishes the relationship. Feature-specific prerequisites remain explicit.

A concrete <FeatureName>Connection is not required merely because a Family has outside truth. Reserve
Connection for a genuinely distinct live protocol/session/provider boundary. Cross-Family composition
belongs to Relay/convergence, not dependency construction.

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
