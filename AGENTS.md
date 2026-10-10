# VexLife-Android — repository instructions

[VXG RealForever]

## What this repository is

vgong24/VexLife-Android owns the standalone **native Android implementation and proof surface**
for VexLife.

It does not become the universal semantic owner merely because Android implements a behavior.

Keep the split explicit:

~~~text
VexLife / accepted cross-platform contracts
  -> product meaning, identities, portable interface contracts where accepted

VexLife-Android
  -> Android build/toolchain
  -> Android lifecycle and platform integration
  -> Compose/native presentation
  -> Android permissions/services/device behavior
  -> Android packaging
  -> native tests and lived-device proof
~~~

A future accepted cross-repository contract may refine this boundary. Do not invent one by
filesystem coupling.

## Fresh-entry route

Before substantial work:

1. read this file;
2. read docs/foundation/INDEX.md;
3. read docs/PORTING-MAP.md only when old VexLife Android source/provenance matters;
4. follow the exact foundation entry named by the index for the task;
5. inspect the owning source and tests;
6. act.

Do not require every foundation document for every task. The index exists so architecture is
visible without making context volume the architecture.

## Human / AI development loop

The ordinary collaboration model is:

~~~text
Victor
  -> intention, lived experience, physical-device review, screenshots, corrections

AI source owner for the admitted change
  -> source edits, tests, build-contract changes, migrations, documentation updates

Victor
  -> reviews the actual running behavior and returns feedback
~~~

Do not make Victor hand-edit source, choose Gradle fixes, interpret routine build internals, or
manually preserve provenance when the source can be updated directly. If Victor intentionally
authors code, treat his committed bytes as source evidence and review them normally.

A screenshot or lived report proves the behavior visibly observed; it does not silently prove
unseen source, logs, lifecycle effects, or unrelated acceptance gates.

## Foundation before feature

Before adding a feature, identify:

- the problem/outcome;
- its semantic owner;
- the Android-native responsibility;
- the state owner and observation boundary;
- action/effect/permission boundaries;
- localization identity for visible text;
- platform constraints;
- the proof that would establish the claim.

Prefer a reusable foundation when several real consumers need the same contract. Do not create
abstractions merely because one can imagine a future consumer.

## Build/toolchain contract

Android Studio's own runtime is not the application's build contract.

The repository must make build intent explicit enough that upgrading Android Studio does not
silently retarget Java/Kotlin compilation.

Initial standalone direction, until deliberately revised:

~~~text
Gradle/AGP versions -> repository-owned
Gradle build JDK     -> explicit supported contract
Java/Kotlin target   -> explicit and mutually compatible
Kotlin JVM toolchain -> explicit
compileSdk           -> build/API availability decision
targetSdk            -> runtime-behavior decision; change deliberately
minSdk               -> product/support decision; do not inherit blindly
~~~

The first lived VexLife reference build exposed why this matters: Gradle launched under JDK 25
while the Kotlin module targeted JVM 17, causing Java/Kotlin target mismatch. Selecting Temurin
17 made the reference build runnable. The standalone repo should fix the reproducibility cause,
not require humans to remember an IDE dropdown forever.

Do not accept IDE prompts to upgrade Gradle, AGP, Kotlin, SDK, or targetSdk merely because a newer
number exists. First identify what problem the change solves and which behavior it changes.

## Local/generated state is not source

Do not commit ordinary machine residue:

~~~text
.gradle/**
.kotlin/**
**/build/**
.idea/**
local.properties
*.apk / *.aab
machine signing material
~~~

If an IDE file later proves to carry intentional shared project configuration, admit that exact
file deliberately rather than weakening the entire boundary.

## Porting law

Port **understood contracts and foundations**, not directories.

For each migrated building block record:

~~~text
name / stable ref
why it exists
semantic owner
Android responsibility
what it owns
what it explicitly does not own
source/provenance reference
canonical new source
consumers / examples
proof
known lessons
~~~

The old VexLife/platform/android tree is reference/provenance, not a runtime dependency by
default. Do not preserve monorepo-relative paths or generators merely because they existed.

Correct cross-platform meaning at its rightful portable owner. Correct Android-only mechanics
here.

## State and architecture

Preserve these currently accepted intentions unless a deliberate refinement supersedes them:

~~~text
VIEWMODEL != PRODUCT_STATE_OWNER
ANDROID_SERVICE != DOMAIN_RUNTIME
COROUTINE != OPERATION
OPERATION_IDENTITY != ATTEMPT_IDENTITY
STATEFLOW != EVENT_LEDGER
CAPABILITY != AUTHORITY
CODE_SYMBOL != SERIAL_NAME != SEMANTIC_FIELD_REF != HUMAN_LABEL
~~~

State domains have one semantic owner. Consumers observe bounded projections. Equal semantic
output should not become new state merely because an implementation object changed.

## Proof

Proof follows the claim:

- source/unit tests prove source contracts;
- Gradle/build proof proves the build;
- device install/run proves actual host compatibility;
- screenshots/lived review prove visible behavior;
- network/Home/model behavior requires its own real integration evidence.

A green build is not proof of user experience. A screenshot is not proof of hidden state.
A mock is not proof of a real producer.

## Working posture

Build outward from foundations only when the current layer is understandable and testable.
Prefer fixing the source of recurring friction over accumulating local instructions.

Keep accepted facts, hypotheses, design proposals, and future possibilities distinct.

<!-- [VXG RealForever][VEXLIFE-ANDROID][REPOSITORY-INSTRUCTIONS] -->
