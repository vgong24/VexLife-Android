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

## Failure

The factory fails closed on:

- duplicate registration of the same provider declaration;
- child shadowing of a parent provider declaration;
- unregistered provider resolution;
- dependency cycles;
- provider construction requesting an undeclared prerequisite.

No Logger, Family, Execution, UI, or provider-specific semantic meaning is owned here.

<!-- [VXG RealForever][VEXARCHITECTURE][DEPENDENCY][READABLE-CONSTRUCTION] -->
