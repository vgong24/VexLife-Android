# Family Circuit — direct readable semantics

[VXG RealForever]

## Human question

> What does this Control mean against exact CURRENT semantic State, what State may be admitted
> immediately, and what runtime transformation may be requested without making Family own execution,
> persistence, logging, dependency construction, or provider mechanics?

The current direct Family is deliberately semantic-only.

~~~text
caller / Family-Store binding
-> Family.activate(Control)
-> concrete Family.act(CURRENT State, Control)
-> Act?
   -> admitted State?
   -> Effect?
-> immediate State admission
-> Act returned to caller

Family/Store binding
-> realizes Effect
-> reconciles provider/runtime truth
-> Family.observe(already-semantic State)
~~~

## Family vocabulary

One exact Family coordinate binds:

~~~text
VexFamily<Family, State, Control, Effect>

State
    current semantic truth

Control
    semantic request / affordance

Act
    lawful result of exact CURRENT State + Control

Effect
    requested runtime transformation
    != resulting truth
~~~

A concrete Family owns its vocabulary and native Kotlin relationship:

~~~kotlin
override fun act(
    currentState: ThemeMode,
    control: Control,
): VexFamily.Act<ThemeMode, Effect>? =
    when (control) {
        is Control.Select -> {
            val effect = Effect.Save(control.mode)

            if (currentState == control.mode) {
                stay(effect)
            } else {
                admit(
                    state = control.mode,
                    effect = effect,
                )
            }
        }
    }
~~~

There is no Acts table, custom rule builder, execution trigger DSL, Store callback, or Logger call
inside this semantic decision.

## State ownership

Family keeps one live State holder:

~~~text
MutableStateFlow<State>
-> currentState
-> states

activate(Control)
    resolves against exact CURRENT State
    admits immediate semantic State when authored

observe(State)
    publishes already-reconciled outside semantic truth
~~~

The caller of observe(...) must already own provider/repository interpretation, stale-result gating,
and construction of the truthful semantic State. Family does not become a provider adapter merely
because it can admit observed truth.

## Act / Effect boundary

~~~text
CURRENT State + Control
-> Act?

Act
-> optional admitted State
-> optional Effect
~~~

Effect is a request, not proof of a result.

For ThemeMode:

~~~text
Control.Select(Dark)
-> immediate ThemeMode.Dark semantic admission
-> Effect.Save(Dark)

VexFamilyStoreBinding<ThemeModeFamily,...>
-> realizes Save
-> Store/runtime result
-> VexLogger accepted/observed facts
~~~

Selecting the already-current mode may still lawfully return Save because explicit durable selection
is a runtime consequence even when semantic State does not change.

## Display interpretation

Family owns the bounded outward interpretation of its own State:

~~~text
VexFamily
-> DisplayState
   -> StateOnly by default

Concrete Family
-> richer DisplayState only when repeated consumer interpretation earns it

State snapshot
-> Projection(State, DisplayState)
~~~

DisplayState is derived interpretation, not another truth owner.

## Runtime relationship identity belongs outside Family

The refined direct Family does not author execution identity.

~~~text
Family
    Control / State / Act / Effect meaning

Family/Store runtime binding
    knows the accepted Control
    knows the Effect it must realize
    derives the smallest lawful active-work RelationshipId
    asks VexExecution to own Job custody
~~~

Connection is not a Vex core Family layer. Store may depend directly on third-party SDK/service
objects. When a lower-level session/adapter object is genuinely required for protocol lifecycle,
threading, authentication, reconnect, callback registration, or shared teardown correctness, name
that object after the concrete system it owns (for example Media3Session) rather than introducing a
generic <Feature>Connection abstraction.

For ThemeMode the current runtime law is:

~~~text
one live typed ThemeMode Family/Store binding owner
+ Control.Select kind
-> RelationshipId

selected Light / Dark
-> operation payload
-> not another active-work identity
~~~

A Family with a real repeated domain resource may contribute that typed ResourceId through its
Control/domain vocabulary, but the runtime bridge—not pure Family—owns the decision that the
coordinate is required for active execution.

## Store boundary

Store means the **Family-facing outside-truth boundary**, not "database" or "persistence".

A Store may realize any combination of:

~~~text
temporary / in-memory truth
cache
local persistence
service/backend fetches
network mutations
repository/database access
platform APIs
observable provider state
~~~

Permanent:

~~~text
STORE != DATABASE
STORE != PERSISTENCE
STORE != CACHE
STORE != NETWORK CLIENT

Store
    = the Family-facing capability that obtains, observes, or changes outside truth
~~~

Family itself remains semantic and does not receive Store/provider mechanics directly.

~~~text
Family
    Control + CURRENT State
    -> Act
    -> optional Effect

typed Family/Store runtime binding
    -> realizes Effect through Store under rightful VexExecution custody
    -> admits accepted/observed facts to VexLogger
    -> reconciles already-semantic outside truth back through Family.observe(State)
~~~

The binding must preserve exact Family kinship. A Store/runtime belonging to another Family must not
be composable merely because its methods happen to have compatible Kotlin signatures.

ThemeMode now uses the reusable VexFamilyStoreBinding directly. There is no ThemeMode-specific runtime bridge class. The feature declares its exact ThemeModeFamily + ThemeModeStore
relationship once; standard Execution/Logger/lifetime mechanics are derived by the binding provider.
The feature must explicitly supply overlap approach, typed execution RelationshipKey, and outside-State
admission policy; the binding does not guess those decisions.

Do not infer Store by class name, reflection, package scanning, or naming convention. Declare the
Family/Store relationship once; mechanically derive only what that declaration actually implies.

## Logger boundary

Family does not receive VexLogger.

~~~text
Family.activate(Control)
-> returns accepted semantic Act

Family/Store binding boundary
-> knows which semantic/runtime fact became true
-> VexLogger.Sink
-> one chronology
~~~

This preserves inspectability: asking Family whether a Control is available cannot emit Logger facts
or perform runtime work.

## Dependency construction boundary

Family does not implement Injectable/build/container mechanics.

~~~text
provider declaration
-> constructs Family
-> constructs typed Family/Store binding from Family + Store + standard runtime dependencies
-> consumer receives Family or the typed binding only when activation is required
~~~

The dependency system can construct a Family without the Family knowing a dependency system exists.

## ThemeMode patient zero

Current production relationship on PR #209:

~~~text
VexApplication dependency lifetime
    ThemeModeStore

VexLifeViewModel/session lifetime
    ThemeModeFamily
    VexExecutionRoot
    VexLogger
        |
        v
    VexFamilyStoreBinding<ThemeModeFamily,...>
        |
        +-> Family
        +-> Store
        +-> VexExecution
        +-> Logger Sink

Debug Menu action
-> Family/Store binding.activate(Control.Select)
-> Family semantic Act
-> Save runtime Job
-> accepted/observed Logger chronology
~~~

The Family itself remains reusable and testable without Android Store, Execution, Logger, or
dependency-factory construction.

## Compound / Relay remains separate

Direct Family/Store runtime binding is not a replacement for Compound Relay.

~~~text
Direct Family
    one semantic State / Control / Act / Effect question

Compound Relay
    several independently owned changing truths
    -> coherent processed relationship State
~~~

Do not invent CompoundFamily or route Relay through Family merely because both manipulate semantic
State.

## Proof

Focused Family tests protect:

- exact State/Control kinship;
- pure act resolution;
- immediate State admission;
- Effect return without realization;
- availability without side effect;
- already-semantic observe(...);
- exact Projection/DisplayState pairing;
- serialized concurrent activation.

ThemeMode tests separately pressure same-State Save effect and explicit NotSet resolution.

Repository-owned Gradle/JVM proof is required on exact PR #209 source before the refined Family
candidate may be called proven.

<!-- [VXG RealForever][VEX-FAMILY][PURE-STATE-CONTROL-ACT-EFFECT] -->
