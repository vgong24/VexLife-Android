# Android Host Proof via VexRelay

[VXG RealForever]

## Purpose

This repository owns Android-native implementation and Android-native proof. A source worker may not
always have the Android SDK, Gradle execution environment, ADB access, or Victor's physical device.
That capability gap must not force the lane to stop or make Victor manually interpret build output.

When direct repository/build/device capability is unavailable, VexRelay/VexLocalBridge may carry one
bounded Android proof task through the qualified host:

```text
source worker
  -> one current-contract-bound proof package
  -> one exact host command
Victor
  -> runs that one command
host
  -> Gradle / ADB execution
  -> one canonical RETURN ZIP
source worker
  -> consumes RETURN
  -> continues from exact PASS / failure / held predicate
```

This document preserves the **VexLife-Android realization** of that flow. It does not own or duplicate
the VexRelay protocol.

## Ownership boundary

VexLife-Android owns:

- which Android candidate is being proved;
- which Gradle claims are required for that candidate;
- which APK/device facts matter;
- repository-specific application/package/activity identities;
- how Android proof is interpreted for the current feature/foundation.

Vextreme-SDK owns:

- VexRelay task/result schemas;
- current Relay contract/current runtime;
- package formation and task-window mechanics;
- VexLocalBridge host carriage;
- repository/host confinement;
- replay protection;
- canonical RETURN formation and consumption.

Before forming a fresh Android host proof, read the live SDK:

```text
docs/private-continuity/vexrelay/current-contract.json
```

and require:

```text
contractRef=source.vexrelay.current-contract/v1
```

Do not copy a prior ZIP digest, launcher command, task window, attempt ID, SDK head, or adapter binding
from this document. Those are attempt-local/current-contract facts and must be formed from current
source.

## Selection rule

Use the smallest truthful execution surface.

```text
worker already has Android SDK / Gradle / required device access
  -> execute directly

worker lacks required Android host capability
  -> use current VexRelay/VexLocalBridge host proof

human judgment / visual acceptance is actually required
  -> keep that one human-only predicate explicit
```

Relay is not ceremony. Do not reproduce the host path when the active worker can execute the proof
directly.

## Human contract

For a selected Android host proof, Victor should receive only:

```text
one digest-bound ZIP
+ one exact platform command
+ one canonical RETURN ZIP
```

The package/adapter owns technical choices such as repository, candidate, commands, evidence capture,
failure classification and RETURN location. Victor must not have to choose which branch, Gradle task,
ADB command, log, recovery route, or failure explanation matters.

## Android proof request shape

A reusable Android host-proof request binds at least:

```text
repository = vgong24/VexLife-Android
candidateRef = exact branch / PR / accepted source coordinate
candidateHead = exact immutable commit SHA

proof class(es):
  SOURCE_BINDING
  GRADLE
  APK
  ADB_DEVICE      # only when device proof is required

required Gradle operations
expected APK path when applicable
current applicationId / launcher activity when device launch is applicable
evidence to retain
stop conditions
```

The host must verify the requested candidate before execution. A successful command run against a
different checkout/head is not candidate proof.

## Repository-owned Gradle proof

The Android host layer should ask the repository for proof instead of reproducing its task list.

```text
focused feature proof when needed
-> vexVerifyLocal
-> optional evidence collectors
```

Canonical repository entry points are owned by `gradle/test-foundation.gradle.kts`:

```bash
./gradlew vexVerifyJvm
./gradlew vexVerifyLocal
./gradlew vexVerifyDevice
```

`vexVerifyLocal` owns the ordinary source/JVM/debug-APK proof and does not require a connected
Android target. `vexVerifyDevice` owns the connected Android instrumentation/Journey substrate.
A focused feature proof may run before the aggregate task, but focused PASS never replaces the
repository-owned aggregate claim.

The host proof should retain enough output to identify the **first substantive failure**, plus the
relevant test result/report artifacts. Do not make the consumer reconstruct a failing test from a
truncated terminal transcript when structured Gradle/JUnit evidence exists.

## Post-proof source effects

Android proof must not reduce source state to only CLEAN/DIRTY.

```text
BUILD_OUTPUT
  disposable or already excluded by repository source policy

GENERATED_SOURCE
  preserve exact bytes
  compare against repository policy
  explicit accept / hold / cleanup disposition

REVIEWED_IMMUTABLE_INPUT
  bind exact accepted bytes
  validation must not silently regenerate them

UNEXPECTED_SOURCE_CHANGE
  fail closed
```

Events A008 established the concrete GENERATED_SOURCE case: all parity/test/build predicates passed
before Room emitted `app/schemas/com.vextreme.vexlife.android.data.VexLifeDatabase/4.json`.
The generated schema is now repository source so ordinary proof can return cleanly.

Test Foundation established the complementary reviewed-reference case: candidate screenshot/golden
bytes are not accepted merely because an update task generated them.

## APK and Android target proof

Current source declares:

```text
applicationId = com.vextreme.vexlife.android
launcher activity = .MainActivity
debug APK = app/build/outputs/apk/debug/app-debug.apk
```

These values are current repository facts, not eternal Relay constants. A newly formed task must
re-read current source when the candidate could have changed them.

When device proof is requested, target acquisition is bounded:

```text
existing authorized Android target
OR
already-configured qualified AVD
  -> boot only the task-started emulator
  -> no wipe
  -> no snapshot replacement/save
  -> stop only the emulator the task started

missing required substrate
  -> exact held host predicate / separately authorized repair
```

ADB/device/AVD effects are Android-runtime effects. They must be explicitly admitted and evidenced;
they are not made read-only merely because a CLI performs them.

A qualified target may then prove:

```text
target observed / authorized
exact candidate APK exists
APK install succeeds
launcher activity starts
requested instrumentation/Journey/ScreenCrawler evidence completes
```

For the current source shape, equivalent host commands may include:

```bash
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.vextreme.vexlife.android/.MainActivity
```

Those lines are a repository-specific **worked command shape**, not the VexLocalBridge launcher and
not permission to make Victor select among multiple devices. Device selection/currentness belongs to
the formed task/host adapter. If the required device predicate cannot be established, preserve that
as a held/not-reached device effect rather than erasing completed Gradle proof.

## Evidence separation

Do not collapse distinct claims:

```text
SOURCE_BINDING PASS
!=
GRADLE PASS
!=
UNIT TEST PASS
!=
APK ASSEMBLED
!=
ADB INSTALL PASS
!=
APP LAUNCH PASS
!=
LIVED HUMAN ACCEPTANCE
```

A later-stage missing predicate does not invalidate a completed earlier proof unless that predicate
was required for the earlier claim.

Examples:

```text
Gradle/test PASS + no authorized device
  -> repository proof retained
  -> device proof held / not reached

focused parity test FAIL
  -> preserve exact failing test/evidence
  -> do not continue to claim aggregate PASS

APK installs + app launches
  -> native launch proved
  -> visual/product acceptance still separate
```

## Recover before replay

If semantic Android proof already reached PASS but a later carrier/RETURN step fails, preserve and
consume retained proof before considering replay. Expensive device, visual, or crawler work is not
automatically rerun merely because result formation failed later.

## RETURN consumption

The canonical RETURN is the continuation surface. A future source worker should consume it before
asking Victor for terminal output or replaying the task.

The consumer should be able to recover:

- exact candidate binding;
- which proof stages ran;
- first substantive failure or terminal PASS;
- Gradle/test evidence;
- APK/device evidence when requested;
- held/not-reached predicates;
- whether a same-task retry is lawful under the producing contract.

Do not replace the canonical RETURN with screenshots, copied console text, or a prose summary when the
RETURN exists.

## Independent convergence evidence

Two independent Android consumers earned this reusable boundary before extraction:

```text
Events PR #74
  A008
  focused parity PASS
  repository tests/build/APK PASS
  exposed generated-source cleanliness seam

Test Foundation PR #76
  A019
  host proof PASS
  baseline Android 11/11 PASS
  explicit ScreenCrawler 9/9 PASS
  crawler screenshots 19/19 identity + SHA matches
  source cleanliness PASS
  managed cleanup PASS
```

The reusable result is the proof/carrier architecture. Events, Maps, VexStream, Journey,
ScreenCrawler, and visual semantics remain with their rightful feature/foundation owners.

This extraction branch must receive its own exact host proof before merge; prior lane proofs establish
the design evidence, not the extracted branch's final source validity.

## Reuse by future instances

A fresh worker should enter through:

```text
AGENTS.md
-> docs/foundation/INDEX.md
-> ANDROID-HOST-PROOF.md
-> live Vextreme-SDK current-contract.json
-> current candidate source/tests
```

Then form a fresh proof from current facts.

Do not rediscover the host workflow from predecessor chat, and do not turn this worked example into a
frozen Android-specific fork of VexRelay.

<!-- [VXG RealForever][VEXLIFE-ANDROID][ANDROID-HOST-PROOF][VEXRELAY] -->
