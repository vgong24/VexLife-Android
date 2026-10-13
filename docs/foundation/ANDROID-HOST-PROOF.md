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

## Gradle proof

For the current repository foundation, the ordinary aggregate verification shape is:

```bash
./gradlew \
  :app:verifySourceAlignment \
  :architecture:elements:test \
  :architecture:logging:test \
  :app:testDebugUnitTest \
  :app:assembleDebug \
  --no-configuration-cache \
  --stacktrace
```

A narrower candidate may request a focused test first, but focused PASS does not silently replace an
aggregate claim.

The host proof should retain enough output to identify the **first substantive failure**, plus the
relevant test result/report artifacts. Do not make the consumer reconstruct a failing test from a
truncated terminal transcript when structured Gradle/JUnit evidence exists.

## APK and ADB proof

Current source declares:

```text
applicationId = com.vextreme.vexlife.android
launcher activity = .MainActivity
debug APK = app/build/outputs/apk/debug/app-debug.apk
```

These values are current repository facts, not eternal Relay constants. A newly formed task must
re-read current source when the candidate could have changed them.

When device proof is requested and a qualified ADB target is available, the task may prove:

```text
adb target observed / authorized
exact candidate APK exists
APK install succeeds
launcher activity starts
package/activity is observed running
requested device-side test/evidence completes
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

## First worked use — Events architecture comparison

The first Android Relay consumer candidate is the Events architecture laboratory on draft PR #74.

Formation-time binding must use the **current PR head**, not the historical SHA below. At the time this
entry was first written, the candidate coordinate was:

```text
PR = #74
branch = VXG-101326-events-architecture-comparison-lab
head = fcd24b1c109995e1855cc11b52cb4725ddbb87c7
```

The intended proof asks the host to establish, in order:

```text
1. exact VexLife-Android candidate binding
2. :app:verifySourceAlignment
3. :architecture:elements:test
4. :architecture:logging:test
5. :app:testDebugUnitTest
   - includes EventsArchitectureParityTest
6. :app:assembleDebug
7. verify exact debug APK exists
8. if a qualified ADB device is available:
   - observe target
   - install candidate APK
   - launch VexLife
   - retain device evidence
9. form one canonical RETURN
```

The Events lane must not add a live `PARITY / DIVERGENCE` confidence surface merely because the
comparison source compiles. Repository parity execution is the next material predicate; ADB/lived
proof follows the claim that actually requires it.

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
