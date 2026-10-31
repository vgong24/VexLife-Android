# VexLife-Android — Development Culture

`[VXG RealForever]`

> This repository is young on purpose. Preserve the useful intentions of the Vextreme/VexLife
> lineage, but do not inherit every historical process, abstraction, or repository mechanism.

## Foundation status

**TEMPORARY BOOTSTRAP NOTICE — THE REPOSITORY FOUNDATION IS INCOMPLETE.**

While this notice exists, treat the current architecture as an intentionally growing foundation.
A present pattern is not automatically permanent merely because it is the first implementation.

Remove this notice only when the foundation index's establishment criteria are actually met and the
repository can be entered, built, understood, and extended without depending on private chat context
or the old VexLife Android tree.

Removing the notice does **not** mean every future feature is complete. It means the repository's
foundation is mature enough to be treated as the normal starting point rather than a bootstrap
experiment.

## Lineage without process inheritance

This repository draws cultural lineage from public source, especially:

- `vgong24/Vextreme` → `docs/culture.md`
- `vgong24/VexLife` → `docs/CULTURE.md`
- `vgong24/VexLife` → `docs/FOUNDATION-CULTURE-IMPLEMENTATION-STANDARD.md`
- `vgong24/VexLife` → `docs/FOUNDATION-CULTURE-COVERAGE-MAP.md`

At bootstrap formation, the observed upstream main revisions were:

```text
Vextreme.main = 3b719962b99e830e9739d9d53d779b752ac8d087
VexLife.main  = 76085cda27a5b1999cd75283ceec99ecef535145
```

These are provenance anchors, not frozen forever-current requirements. Re-read current upstream
source when a decision materially depends on its current meaning.

Culture lineage does **not** import another repository's operational machinery. Vextreme's document
routing, claim system, build scripts, or cold-start sequence do not govern here merely because they
are useful there. VexLife's monorepo source-manifest, generated topology, or lifecycle machinery do
not govern here merely because this repository came from VexLife Android work.

Import a mechanism only when this repository has a concrete need for it.

## Intention before syntax

Before changing source, be able to answer:

```text
What outcome is this for?
What owns the meaning?
What is specifically Android's responsibility?
What state does it observe or own?
What can it do, and what permission/effect boundary applies?
What proves the behavior we are claiming?
What else becomes wrong if this changes?
```

Do not let a convenient framework pattern become architecture merely because it compiled.

## Subtractive completion

Architecture work defaults to subtraction before invention.

Before adding or preserving a reusable type, helper, seam, state holder, identity, adapter, callback
grammar, or lifecycle concept, answer:

```text
What observed product/contract failure requires this?
Why can the existing rightful owner not express it directly?
What required correctness is lost if this addition is removed?
Who consumes it?
What recurring reading/execution/maintenance burden does it create?
```

If removing the proposed addition loses no required correctness, continuity, authority, or proof,
do not add it. A locally useful capability is not automatically architecture.

When a problem is claimed to be solved, replay the original failure against the candidate. A new
mechanism's adjacent benefit does not establish that the original failure closed.

Prefer:

```text
one semantic doorway
+ feature-owned meaning
+ private mechanics
```

over multiple peer semantic APIs that can author the same outcome.

Tests prove behavior and implementation consistency; they do not earn architecture by themselves.
Historical machinery remains evidence, not an obligation to preserve a generic abstraction after its
real consumers disappear.

When human-vetted architecture changes, reopen only the exact changed semantic declarations and
connections. Do not backdate, broaden, or preserve approval across changed meaning. Unchanged
semantic laws keep their provenance; changed laws require fresh human review of the exact source.

## Mastered architecture

Human vetting is provenance, not a claim that architecture has reached its best possible form.

~~~text
VETTED
    Victor explicitly reviewed this exact semantic law.

MASTERED
    the current architecture has also been deliberately challenged against the full set of known
    demonstrated pressures, stripped of accidental implementation coupling, and shaped as the
    smallest complete long-term semantic grammar we currently know how to build.
~~~

Do not preserve a weaker design merely because it was previously vetted. If a better end-state is
recovered, make the semantic change visible, preserve the old approval as history, and seek fresh
human vetting for the changed law.

Mastery is also not permission to speculate without pressure. The target is neither:

~~~text
first consumer only
-> underfit architecture
~~~

nor:

~~~text
imagined every possible future
-> framework maximalism
~~~

Use all **demonstrated** pressures available across current consumers, lived product requirements,
Origin Walk history, provider/runtime boundaries, and adjacent accepted architecture. Then choose
the smallest grammar that can express those pressures without changing its semantic nouns for each
new realization.

A mastered semantic root should not depend on one realization merely because that realization was
the first patient zero. For example:

~~~text
semantic Effect
!= SharedPreferences
!= network request
!= coroutine
!= provider callback

Connection/reality boundary
owns those realization mechanics.
~~~

Before calling an architecture slice mastered, ask:

~~~text
Would another already-demonstrated consumer force us to rename the semantic nouns?
Does a semantic root depend on Android/provider/storage/execution mechanics it does not own?
Are two different causes being hidden behind one convenient API?
Are two APIs authoring the same semantic truth?
Can a feature-specific mechanism be replaced by an already-earned semantic role?
Have we replayed known failure modes against the end-state rather than only the current example?
~~~

Mastered architecture may still have explicitly open boundaries. The requirement is that the
settled grammar remains truthful about those unknowns instead of filling them with guessed machinery.

### Preserve known owner coordinates in reusable types

When reusable infrastructure already knows the rightful semantic owner coordinates, carry those
coordinates in the type rather than relying on naming/proximity/documentation.

~~~text
WEAKER

DisplayState<State>
Projection<State>

STRONGER WHEN FAMILY OWNERSHIP IS KNOWN

DisplayState<Family, State>
Projection<Family, State, Display>
~~~

The goal is not generic maximalism. Preserve the **strongest already-known ownership coordinates**
that prevent invalid composition.

If a child is derived by its parent, make the derivation seam compiler-visible through an override or
parent-owned factory rather than inviting adjacent code to reconstruct the relationship.

~~~text
State
-> Family.displayStateFor(State)
-> Family.projectionFor(State)
~~~

Prefer compiler-visible ownership to comments such as "these two values must belong together."

This is especially important for AI-authored work: if the type system can reject a wrong owner,
wrong State, wrong DisplayState, or wrong projection composition, do not leave that correctness as a
prompt-memory requirement.

## Foundation before features

Build outward only when the layer underneath is stable enough to explain, test, and recover.

"Foundation first" does not mean "design everything in advance." It means each new layer should
teach us something real about the layer below, and recurring friction should be fixed at its source
instead of accumulated as local ritual.

## Map first; depth second

A fresh worker should not need the whole repository in context.

```text
human intention
-> foundation index
-> exact building block
-> owning source + tests
-> affected neighbors
-> real proof
```

When a new building block becomes load-bearing, make it findable from the index. Do not make the
front door absorb its implementation details.

## One semantic owner; bounded projections

Preserve the useful VexLife state discipline:

```text
one canonical owner per state domain
-> small observable projection for each consumer
-> semantic equality before emission
-> no new "truth" merely because an implementation object changed
```

StateFlow is one Android mechanism for this intention, not the intention itself.

Keep these distinctions visible unless deliberately refined:

```text
VIEWMODEL != PRODUCT_STATE_OWNER
ANDROID_SERVICE != DOMAIN_RUNTIME
COROUTINE != OPERATION
OPERATION_IDENTITY != ATTEMPT_IDENTITY
STATEFLOW != EVENT_LEDGER
CAPABILITY != AUTHORITY
CODE_SYMBOL != SERIAL_NAME != SEMANTIC_FIELD_REF != HUMAN_LABEL
```

## Rooted semantic trees; no orphan contracts

Architecture should be recoverable from source as rooted semantic trees, not as a flat package of
peer types that merely import, reference, or manually coordinate with one another.

Every reusable Vex concept must have one explicit root path:

```text
Vex<Root>
-> Child
-> Child
-> ...

or

<ConcreteRoot>
-> Child
-> Child
-> ...
```

A child that exists only because its parent owns that responsibility belongs to the parent's
contract. If that child owns further semantic children, those relationships must remain explicit
under the child as well. Do not stop the tree halfway and leave lower-level types as package peers
whose relationship has to be reconstructed from constructors, imports, naming similarity, or
predecessor knowledge.

```text
GOOD

VexFamily
-> DisplayState
   -> StateOnly
-> Act
   -> <only relationships Act actually owns>

ThemeModeFamily
-> State
-> Control
-> DisplayState

NOT SUFFICIENT

VexFamily
VexDisplayState
VexAct
VexBegin
VexResult

# package proximity and cross-references do not establish semantic ownership
```

`Vex` names an independently meaningful **root**, not every descendant in that root's vocabulary.
Once the root supplies scope, descendants use the ordinary name of the role unless another
independent root has actually been earned:

```text
VexFamily.DisplayState
VexFamily.DisplayState.StateOnly

not

VexFamily + VexDisplayState + VexDisplayStateEmpty
```

A dependency earns its own `Vex<Root>` only when it owns a distinct reusable responsibility that
can be understood and contracted independently of the consumer. For example, execution custody or
logging may be independent roots when their contracts stand on their own. Consumption does not
collapse those roots into one tree:

```text
VexFamily
-> explicit dependency on VexExecution
-> explicit dependency on VexLogger

VexExecution != VexFamily child
VexLogger    != VexFamily child
```

When two roots interact, make the root-to-root boundary explicit. Do not extract a free-floating
"shared" type merely because both use it. If the concept has independent meaning, prove that and
give it a rightful root; otherwise keep each responsibility under its actual owner.

Before adding, moving, renaming, or judging an architectural type, a fresh worker should be able to
answer:

```text
What is this type's root?
What parent owns this exact relationship?
If it has children, where are those child relationships contracted?
If it crosses to another root, where is that boundary explicit?
If it has no parent, what independent responsibility earned it a Vex root?
```

If those answers are not recoverable from current source, investigate the current owner before
creating another abstraction. Do not "complete" a tree by combining adjacent historical/legacy
surfaces, and do not treat an unparented type as permission to invent a new root.

This law constrains **representation of already-earned relationships**; it does not earn new
relationships. First prove the responsibility is real. Then place it under the narrowest rightful
root/parent so future humans and AI workers can build on the tree instead of reconstructing a graph
from assumptions.

## Naming follows the boundary

Naming style is part of the contract. Do not rename between styles merely because a worker/model
prefers a different aesthetic.

```text
Kotlin type / object / enum names
  PascalCase
  UserProfileRepository, DebugMenuScreen

Kotlin functions / properties / locals / parameters
  camelCase
  displayName, updateDisplayName, profileRef

Kotlin compile-time constants
  UPPER_SNAKE_CASE
  DATABASE_NAME

Android resources
  snake_case
  user_profile_name_label

Room / SQLite physical schema
  snake_case, declared explicitly
  user_profile, profile_ref, updated_at_epoch_millis

semantic identity strings
  lowercase dot-separated paths
  element.user-profile.body.profile.display-name

network/file serialized names
  explicit external-protocol names at DTO boundary
  never inferred from Room names merely for convenience
```

A Kotlin property such as `displayName` mapping to SQL `display_name` is intentional, not an
inconsistency. Keep the mapping visible with annotations/mappers.

When introducing a new boundary, follow its existing convention. Do not globally "normalize" names
across Kotlin, SQL, Android resources, semantic refs, and wire protocols.

If a rename changes persisted SQL or an external serialized name, it is a compatibility/migration
change, not a cosmetic refactor.

## Screen is the UI destination vocabulary

Use **Screen** for app destinations and their source files:

```text
ScreenElement
ScreenBlueprint            portable structural contract
AppScreenBlueprint         VexLife app Screen contract
ScreenRegistration
HomeScreen.kt
AppScreens
screens/primary/...
screens/shared/...
```

App-authored Screens extend `AppScreenBlueprint`. Adding the Screen once to `AppScreens` makes its
optional `appActionRegistrations(...)` override discoverable automatically. A Screen that declares
an Invoke action but supplies no owner fails action-coverage construction; Screens with only
runtime-owned navigation/presentation actions inherit the empty registration set.

Kotlin inheritance cannot require a separate top-level Android Studio `@Preview` composable, so the
source-alignment gate intentionally owns that part of the authoring contract and emits the concrete
missing-preview/source-shape error.

`AppScreenBlueprint` is also the app-level render bridge. VexLifeApp owns pager/transition
composition only; it must not maintain another `screenId -> ScreenContent` feature switchboard.

~~~text
AppScreens
  one registered Screen list

AppScreenBlueprint.renderContent(...)
  default generic VexElementTree realization
  OR
  Screen-local override that narrows repositories/theme/runtime into its ScreenContent

VexLifeApp
  asks the registered Screen to render
  does not import feature Screens
~~~

Adding a Screen should therefore require one AppScreens registration, not a second edit in
VexLifeApp. Broad composition dependencies may reach the Screen bridge, but the Screen should
immediately narrow them before calling ordinary feature content.

Do not alternate between Page and Screen for the same app-destination concept. Framework APIs may
still use their own terms (for example HorizontalPager's page index); do not rename framework
vocabulary merely to mimic the domain.

Every authored `*Screen.kt` keeps its ordinary `@VexScreenPreviews` entry in the same source file.

The shared preview layer owns environment mechanics only: device/theme forms, in-memory repository
factories, and the real runtime/AppFrame host. It must not own feature-specific preview scenarios.

~~~text
GalleryScreen.kt
  owns "Elements selected" / "AppGraph selected" preview setup

DebugMenuScreen.kt
  owns "Log dialog open" preview setup

UserProfileScreen.kt
  owns seeded/empty profile preview setup

preview/VexPreview.kt
  owns HOW to host those states, not WHAT those states mean
~~~

A preview's `previewSetup` block is therefore a local example of that Screen's initial visual
state, not a hidden central scenario registry or simulated user interaction.

`VexPreviewSetup` owns reusable preview-only state seeding such as `open(...)` and
`selectChild(container, child)`. Do not add Preview-only methods to VexRuntime merely because
Android Studio needs a convenient initial state.

## Local presentation mutations stay local

```text
NavigateTo
  explicit cross-Screen movement

Open / Close / Toggle / SwapTo
  local presentation mutation
  same Screen/shell ownership only

SwapTo(container, child)
  child must be a direct child of that exact swappable container
```

The registry rejects local intents that target another Screen's elements. VexRuntime also rejects
manual activation of a Screen-owned element when that Screen is not current, and PresentationState
rejects direct mutation of inactive Screen-owned presentation state.

This prevents hidden cross-feature effects such as Home mutating Gallery's selected panel before
Gallery is opened.

Platform-native dismissal is also semantic. A draggable/scrim-dismissed modal records one
`Dismissed` interaction against the modal element itself; it does not fabricate a Close-button click.
Android Back remains its own Back diagnostic.

## Static alignment is not behavioral proof

The Gradle source-alignment gate answers only:

> did source drift away from repository-owned naming/placement conventions?

It may check cheap structural facts such as:

- `*Screen.kt` + `AppScreenBlueprint` + colocated Preview;
- Screen registration in `AppScreens`;
- absence of retired `*Page.kt` / `PageBlueprint` destination vocabulary;
- explicit Room table/column names.

It must not pretend source scanning proves navigation, migrations, transactions, persistence,
cancellation, or device behavior. Those remain unit/integration/build/lived proof.

Use:

```text
:app:sourceAlignmentReport
  warning-only orientation

:app:verifySourceAlignment
  failing verification
```

## Semantic journey coverage is not code coverage

Registered UI identity now supports a second proof question:

> does every production interaction surface appear in at least one declared human journey?

The shared `VexJourneyCatalog` is consumed two ways:

~~~text
JVM coverage
  AppScreens.registry
  vs
  journey-named Screens / ActionElements / editable fields / modal surfaces

Android instrumentation
  same journey declaration
  -> real Compose input
  -> VexRuntime/VexLogger
  -> persisted diagnostic records
  -> semantic history comparison
~~~

Do not restate the journey as a second hand-authored expected log. The test-side walk interpreter
forms the expected canonical history from each step.

Keep expected and actual implementations independent:

~~~text
EXPECTED_WALK != VexLogger
EXPECTED_WALK != VexRuntime
~~~

Journey coverage answers semantic map omission. Ordinary code line/branch coverage answers a
different question and may be added separately.

## Action ownership stays scoped

VexRuntime resolves Invoke actions through one current immutable AppActionRegistrations snapshot,
but feature effects should remain beside the Screen/domain that owns them.

```text
Screen declares action
-> VexRuntime logs semantic interaction
-> current AppActionRegistrations
-> exactly one feature-local AppActionRegistration
-> effect owner
```

Do not grow a giant action `when` in VexLifeApp, and do not create broadcast listeners on multiple
Screens. `AppActionRegistrations` rejects duplicate ActionKey ownership so two features cannot silently
respond to one Save/Reset action.

Registrations are not accumulating subscriptions. VexLifeSession assembles one immutable
registration snapshot for the current in-process app session; Activity/Compose recreation reuses that
session rather than adding listeners. A duplicate owner fails when groups combine, and an invoked
action with no owner fails at dispatch.

The registration set is intentionally queryable by either ActionKey or ActionElement. Element lookup
follows the element's declared Invoke/Sequence intent instead of maintaining a second relationship
map.

REGISTRATION_EXISTS != ACTION_IS_AVAILABLE_OR_AUTHORIZED. Registration proves wiring/ownership only;
future Relay/capability/connection state must own whether an action is currently usable.

AppScreens assembles registrations from the same registered Screen list and validates that every
declared Screen Invoke action has an owner. VexLifeSession supplies the construction dependencies but
does not keep a second feature-by-feature registration list. Adding a new Screen should not require a
corresponding VexLifeSession edit.

The app composition may supply broader-lifetime dependencies; it should not re-describe the
feature's business effect.

## Configuration recreation is not a semantic restart

Android may recreate the Activity/Compose tree for configuration changes such as rotation without
the VexLife app session semantically restarting.

Current ownership:

~~~text
VexLifeViewModel
  -> VexLifeSession
       -> VexRuntime navigation/presentation
       -> VexLogger current session
       -> diagnostic persistence queue / viewModelScope
       -> current dark/light selection
       -> app-action registrations
~~~

A configuration recreation should therefore preserve the current Screen and open semantic
presentation state.

Do not promote every Compose-local detail into the ViewModel. Local scroll positions, text drafts,
and feature-local navigation should use the smallest rightful lifetime (remember, rememberSaveable,
or a feature owner) based on a real requirement.

CONFIGURATION_RECREATION != PROCESS_DEATH. The current ViewModel boundary does not claim process
death restoration; that requires explicit serializable/restorable state and separate proof.

## Platform-native responsibility

Portable meaning should remain portable. Android should own what Android can uniquely implement and
prove:

- Gradle/JDK/SDK/toolchain behavior;
- process and application lifecycle;
- permissions;
- intents and deep links;
- services and receivers;
- coroutine/lifecycle cancellation behavior;
- Android-native accessibility and input behavior;
- Compose/native rendering;
- packaging/install/device execution.

A browser proof is not Android proof. A JVM unit test is not physical-device proof. A screenshot is
not proof of hidden state.

## Environment is part of proof

The first lived reference build exposed a real environment-dependent failure: Java compilation
inherited JDK 25 while Kotlin explicitly targeted JVM 17. Selecting installed Temurin 17 allowed the
reference build to proceed.

The lesson is not "always click JDK 17 in the IDE." The lesson is:

**if correctness depends on an environment choice, either encode that choice in source/tooling or
make the dependency explicitly observable and testable.**

This applies to JDKs, SDKs, devices, permissions, OS behavior, architecture, and eventually real
Home/network/model integration.

## Build the reusable thing only after its need is real

A useful example can become a reusable building block. It does not become one automatically.

Before preserving a new abstraction, ask:

- Which real consumers need it?
- What repeated problem does it remove?
- What would be lost if it disappeared?
- Who owns it?
- What proof protects it?
- Can the same truth live at a smaller boundary?

This repository should become easier to understand as it grows, not merely more documented.

## Lived review is part of development

Victor's physical-device experience is a first-class input.

The source worker should normally own code changes. Victor should be able to respond with ordinary
experience:

- what appeared;
- what felt wrong;
- what interaction failed;
- what he expected;
- screenshots/video when useful;
- a protected design/product choice when genuinely needed.

Translate that feedback into source and proof without requiring Victor to become the build system.

## Continuity should be perceivable from Git

A future worker may not have ChatGPT Project instructions, an earlier chat, or the same model.

Therefore the repository must carry enough current architecture, provenance, and proof routes to
continue from Git alone.

PRs and commits should explain material architectural decisions. The foundation index should say
what exists and what remains provisional. Deep historical narration belongs only where a current
decision still needs it.

## Play without architectural amnesia

Design Lab, experiments, debug galleries, and playful UI work are encouraged. They are useful places
to discover reusable primitives.

An experiment becomes foundation only after we understand what it taught us.

<!-- [VXG RealForever][VEXLIFE-ANDROID][CULTURE] -->
