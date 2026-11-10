# Family Circuit — direct readable semantics

[VXG RealForever]

## Human question

> What does this Control mean against exact CURRENT semantic State, what State may be admitted
> immediately, and what runtime transformation may be requested without making Family own execution,
> persistence, logging, dependency construction, or provider mechanics?

The current direct Family keeps semantic resolution pure while authoring the execution relationship
as inert Act data at the same place the consequence is understood.

~~~text
caller
-> Family.activate(Control)
-> concrete Family.act(CURRENT State, Control)
-> Act?
   -> admitted State?
   -> Begin?
      -> Effect
      -> Independent / Earliest / Latest / Queued
-> immediate State admission
-> Act.Accepted returned
   -> authored Act preserved
   -> maintained RelationshipId when required
   -> realizationBegin? while a new occurrence remains owed

later realization boundary
-> Act.Accepted.realizationBegin + Act.Accepted.relationshipId
-> bound Act.Method
   -> specializes VexMethod
-> VexExecution
-> concrete realization
-> VexMethod.Result
   -> Success / Failure / Error
-> rightful result interpretation

outside/provider truth
-> rightful observation / reconciliation
-> already-semantic State
-> Family.observe(State)
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

Act.Begin
    inert authored execution consequence
    Effect + overlap Approach

Effect
    requested runtime transformation
    != resulting truth

Act.Method
    Family-owned specialization of VexMethod
    fully-bound realization of the authored Effect

VexMethod.Result
    reusable outcome topology for a Method attempt
    contract-owned Success / Failure carry domain meaning
    Error is the shared exceptional result
    != resulting State
    != cancellation
~~~

A concrete Family owns the relationship in ordinary Kotlin:

~~~kotlin
override fun act(
    currentState: ThemeMode,
    control: Control,
): VexFamily.Act<
    ThemeModeFamily,
    ThemeMode,
    Effect
>? =
    when (control) {
        is Control.Select -> {
            val save =
                beginLatest(
                    Effect.Save(
                        control.mode,
                    ),
                )

            if (currentState == control.mode.mode) {
                stay(save)
            } else {
                admit(
                    state = control.mode.mode,
                    begin = save,
                )
            }
        }
    }
~~~

begin / beginEarliest / beginLatest / beginQueued do not launch work inside act(...).
isAvailable(...) can inspect the same relationship without starting a Job, calling a Store, or
emitting Logger facts.

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

ThemeMode carries one concrete example of that law without adding a revision/trace identity. The same
`Parameter.Mode` instance already flowing through Control -> Effect -> Method remains the local
currentness fence until that exact Method reaches a terminal result.

While that Parameter is unsettled, a Store observation that disagrees with its mode has no revision
evidence proving it is newer than the accepted selection, so ThemeMode holds it. A matching
observation may be read without changing semantic State, but only the terminal result of the exact
bound Method clears that Parameter fence. Parameter identity therefore protects both delayed
bootstrap truth and delayed observation from a prior local Save.

~~~text
Parameter.Mode(Light)
-> Control.Select(same Parameter)
-> immediate State = Light
-> Method.Save(same Parameter)
        |
        +-- delayed old Store observation = Dark
        |       -> held while Light Parameter is unsettled
        |
        v
exact Method result
-> settle the SAME Parameter identity
-> ordinary Store observations resume

rapid follow-up:

Parameter.Mode(Dark)
-> replaces the unsettled Light Parameter
-> older Light Store observation
   cannot overwrite newer Dark semantic State
~~~

This is ThemeMode/provider currentness, not a generation counter or Execution status registry.
The reusable VexMethod Result topology reports Method realization through Success / Failure / Error positions; it
does not decide whether an observation is current or author semantic State.

## Act / Effect / execution relationship

~~~text
CURRENT State + Control
-> Act?

Act
-> optional admitted State
-> optional Begin

Begin
-> Effect
-> Approach
~~~

Approach is authored here because overlap is part of the consequence relationship, not because
Family owns coroutine execution.

Approach is not inferred from whether the Act stays in the same State or admits a new State:

~~~text
State admission question
    stay / admit
    = what semantic State becomes current now?

Execution overlap question
    Independent / Earliest / Latest / Queued
    = how do unfinished consequences of this same relationship coexist?

These are independent axes.
~~~

ThemeMode happens to use admit(...) + Latest when changing Light/Dark and stay(...) + Latest when
re-selecting the already-current mode. That is evidence that Latest belongs to the Select/Save
overlap meaning, not to "State changed" or "State stayed".

For ThemeMode:

~~~text
Parameter.Mode(Dark)
-> Control.Select(the same Parameter.Mode)
-> immediate ThemeMode.Dark semantic admission
-> beginLatest(Effect.Save(the same Parameter.Mode))

realization boundary
-> Act.Accepted.realizationBegin
   -> exact Effect.Save
   -> authored Approach.Latest
-> Act.Method.Save(
       ThemeModeStore,
       that exact Effect.Save
   )
-> Act.Accepted.launch(
       VexExecutionRoot,
       Method.Save
   )
   fails closed unless Method carries that same in-memory Effect instance
   (object identity here proves provenance; it is not a new semantic identity system)
   consumes the Act's live Family owner + authored Approach + RelationshipId
-> VexExecution Job custody
-> ThemeModeStore.save(Dark)
   -> Boolean provider commit fact
-> Method maps provider fact into VexMethod.Result
-> current Logger facts
-> observed ThemeMode truth
~~~

Selecting the already-current mode may still lawfully return Save because explicit durable selection
is a runtime consequence even when semantic State does not change.

## #207 result-admission distinction

#207 carried a richer Begin grammar where execution Success / Failure could directly author a later
State decision, including a decision against exact CURRENT State.

The current direct Family intentionally keeps a smaller relationship:

~~~text
Act.Begin
    Effect
    Approach
    accepted maintained RelationshipId

bound Family Act.Method
    -> specializes VexMethod
    -> provider / Store realization
    -> VexMethod.Result
       -> Success
       -> Failure
       -> Error when the Method escapes its ordinary returned contract
    -> rightful result interpretation

outside/provider truth
    -> rightful observation / reconciliation
    -> already-semantic State
    -> Family.observe(State)
~~~

Result is reusable relationship vocabulary, not later-State admission. This preserves the #207
subtraction: a result cannot become a second place where feature State meaning is authored. When a
feature needs stale-result gating or reconciliation, that meaning remains with the feature/provider
boundary that knows what "still current" means.

### VexMethod Result branches and operation outputs

The reusable architecture owns the terminal branches and their only concrete carriers:

~~~text
VexMethod
-> Result
   -> Success
      -> Output<T>
   -> Failure
      -> Output<F>
   -> Error
      -> UnhandledException(Throwable)
~~~

Result is sealed and privately rooted. Success / Failure are the only lawful returned carriers.
Error has a behavior-module-internal constructor, so provider/domain code cannot manufacture an
escaped-Method error as an ordinary return. A domain supplies only the operation output already
needed by Success or Failure.

VexMethod.Result is the terminal return contract of `VexMethod.invoke()`. Provider / Store /
repository contracts do not adopt it merely because they can succeed or fail. They return the
smallest truthful provider/domain fact, and the bound Method interprets that fact.

ThemeMode makes the ownership boundary explicit:

~~~kotlin
suspend fun ThemeModeStore.save(
    state: ThemeMode,
): Boolean
~~~

~~~text
true
    physical write committed

false
    physical write did not commit
~~~

The Family-owned executable then forms the Method result:

~~~kotlin
class Save(...) :
    VexFamily.Act.Method<
        ThemeModeFamily,
        ThemeMode,
        Unit
    > {

    override suspend fun invoke():
        VexMethod.Result<
            ThemeMode,
            Unit
        > =
        if (store.save(parameter.mode)) {
            VexMethod.Result.Success(parameter.mode)
        } else {
            VexMethod.Result.Failure(Unit)
        }
}
~~~

An escaped Method exception uses the only Error path:

~~~kotlin
VexMethod.Result.Error(
    cause =
        error,
)
~~~

So the consumer exhaustively handles Success / Failure / Error without an `else`, without a
Method-specific Error factory, and without another domain Result or Failure hierarchy.

The Method result reports the realization attempt. It does not replace subsequent provider
observation or decide semantic State merely because Success / Failure / Error is known.

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

## Execution relationship identity

Family owns the semantic composition of the maintained-work address because the identity is already
present in its authored vocabulary:

~~~text
live Family instance
+ Control kind/name
+ optional typed Control.ResourceId
-> VexExecution.RelationshipId
~~~

The full Control payload is not automatically identity. ThemeMode Light/Dark remains Select payload,
so both requests share the same Select relationship and Latest supersedes the earlier Save.

VexExecution owns only active Job custody and overlap mechanics:

~~~text
RelationshipId -> actual Job(s)
~~~

Do not insert another owner merely to translate a feature verb into launch/begin. If an existing
Session, Family realization boundary, or lifecycle owner already knows the work, keep that meaning
there and use VexExecution as the execution connective tissue.

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

This is a semantic boundary description, not a requirement for one universal Store interface.

Family itself remains semantic and does not receive Store/provider mechanics directly.

~~~text
Family
    Control + CURRENT State
    -> Act
    -> optional Begin
       -> Effect + Approach

realization boundary
    -> realizes Effect through Store under rightful VexExecution custody
    -> admits accepted/observed facts to VexLogger
    -> reconciles already-semantic outside truth back through Family.observe(State)
~~~

The concrete runtime must preserve exact Family/Store kinship. A Store/runtime belonging to another
Family must not become composable merely because its methods happen to have compatible Kotlin
signatures.

ThemeMode uses one concrete ThemeMode realization boundary. Its current policy is authored once at that rightful
owner rather than supplied as generic construction arguments:

~~~text
Control.Select
-> accepted Act carries one Select RelationshipId
-> beginLatest authors the overlap Approach

Light / Dark
-> operation payload
-> not execution identity

Store observation NotSet
-> outside truth
-> not effective ThemeMode admission
~~~

Do not infer Store or execution identity from class names, reflection, package scanning, naming
convention, or arbitrary Control payload. Do not extract a universal runtime bridge from this one
patient zero; a second real Family must first demonstrate the exact repeated law.

## Logger boundary

Family does not receive VexLogger.

~~~text
Family.activate(Control)
-> returns accepted semantic Act

realization boundary
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
-> supplies Store / VexExecutionRoot / Logger to the existing realization callsite
-> consumer receives Family
~~~

Dependency construction does not earn a separate runtime/execution wrapper object. It wires the
objects that already own meaning and lifetime.

## ThemeMode patient zero

Current production relationship on PR #209:

~~~text
VexApplication lifetime
    ThemeModeStore

VexLifeViewModel/session lifetime
    ThemeModeFamily
    VexExecutionRoot
    VexLogger

Debug Menu action
-> ThemeModeFamily.Parameter.Mode
-> ThemeModeFamily.activate(Control.Select(the same Mode))
-> Act.Accepted
   -> authored Act
      -> immediate State?
      -> beginLatest(Effect.Save(the same Mode))
   -> realizationBegin?
-> Act.Method.Save(ThemeModeStore, the exact accepted Effect.Save)
-> Act.Accepted.launch(VexExecutionRoot, Method.Save)
   -> verifies the exact accepted Begin Effect
   -> returns one VexMethod Result form: Success / Failure / Error
   -> consumes already-bound live Family owner
   -> consumes already-authored Approach
   -> consumes already-authored RelationshipId
-> VexExecution Job custody
-> ThemeModeStore
   -> Boolean provider commit fact
-> Method-owned VexMethod.Result
-> accepted/observed Logger chronology
-> Family.observeStoredThemeMode(...)
~~~

The already-authored Act relationship flows directly into VexExecution custody; feature meaning
remains in ThemeModeFamily and its realization functions.

## Same meaning law across different implementations

The repository now shows the same ownership law through several different implementation shapes:

~~~text
ThemeMode direct Family
    semantic relationship itself needs maintained overlap
    -> Act authors Latest
    -> VexExecution owns Job custody

VexStream Playback / Queue / Repeat
    semantic Behavior resolves Effect
    provider operation is immediate
    -> existing Connection realizes Effect directly
    -> no async execution machinery is added merely for symmetry

VexMaps Distribution Session
    existing Session already owns the domain operation and async lifetime
    -> Session uses VexExecution custody directly
    -> domain verbs remain on Session
~~~

The reusable idea is therefore not a Kotlin shape. It is:

~~~text
meaning stays with the owner that can answer the semantic/domain question
execution contributes only the lifetime / overlap mechanics actually required by that meaning
provider observation returns through the owner that can interpret what became true
~~~

This is the level at which sibling architectures should be compared and abstracted.
## Compound / Relay remains separate

Direct Family concrete runtime is not a replacement for Compound Relay.

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
- inert Begin/Effect return without execution during act resolution;
- availability without side effect;
- already-semantic observe(...);
- exact Projection/DisplayState pairing;
- serialized concurrent activation.

ThemeMode tests separately pressure same-State Save effect and explicit NotSet resolution.

Repository-owned Gradle/JVM proof is required on exact PR #209 source before the refined Family
candidate may be called proven.

<!-- [VXG RealForever][VEX-FAMILY][PURE-STATE-CONTROL-ACT-EFFECT] -->
