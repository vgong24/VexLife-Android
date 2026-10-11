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
ScreenBlueprint
ScreenRegistration
HomeScreen.kt
AppScreens
screens/primary/...
screens/shared/...
```

Do not alternate between Page and Screen for the same app-destination concept. Framework APIs may
still use their own terms (for example HorizontalPager's page index); do not rename framework
vocabulary merely to mimic the domain.

Every authored `*Screen.kt` keeps its ordinary `@VexScreenPreviews` entry in the same source file.
Shared preview fixtures/hosts may be centralized, but the visible preview entry point stays beside
the Screen a worker is editing.

## Static alignment is not behavioral proof

The Gradle source-alignment gate answers only:

> did source drift away from repository-owned naming/placement conventions?

It may check cheap structural facts such as:

- `*Screen.kt` + `ScreenBlueprint` + colocated Preview;
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

Registrations are not accumulating subscriptions. App composition combines fresh immutable
registration groups; recomposition replaces the current snapshot rather than adding another listener.
A duplicate owner fails when groups combine, and an invoked action with no owner fails at dispatch.

The registration set is intentionally queryable by either ActionKey or ActionElement. Element lookup
follows the element's declared Invoke/Sequence intent instead of maintaining a second relationship
map.

REGISTRATION_EXISTS != ACTION_IS_AVAILABLE_OR_AUTHORIZED. Registration proves wiring/ownership only;
future Relay/capability/connection state must own whether an action is currently usable.

The app composition may assemble registrations and supply broader-lifetime dependencies; it should
not re-describe the feature's business effect.

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
