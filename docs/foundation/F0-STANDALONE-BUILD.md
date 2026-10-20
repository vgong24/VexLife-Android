# F0 — Standalone Build Foundation

`[VXG RealForever]`

## Status

```text
sourceState=FORMED
hostSyncBuildInstallLaunch=PASS_BY_LIVED_DEVICE_RUN
deviceProof=PASS_VISIBLE_F0_SURFACE
postRunGitSourceState=USER_REPORTED_CLEAN
unitTestCommand=NOT_YET_OBSERVED
checkpoint=[Foundation: F0 Gradle/Manifest/Base]
foundationStatus=INCOMPLETE
```

This file owns the build-foundation decisions for the standalone repository. It does not claim
proof that has not run on Victor's Mac/device yet.

## Why this exists

The VexLife reference Android tree proved that the product can Sync, build, install, launch, and
show the Design Lab on Victor's physical device. It also exposed that Java compilation could inherit
the IDE's JDK 25 while Kotlin targeted JVM 17.

The first standalone F0 host attempt exposed a second, newer Android Studio behavior: the IDE uses
Gradle Daemon JVM criteria and generated gradle/gradle-daemon-jvm.properties with
toolchainVersion=25. That criteria outranks the older Gradle JDK selection, so choosing 17 in the UI
was immediately overwritten on Sync.

F0 therefore owns both layers explicitly:

- Gradle daemon JVM criteria = 17
- Java/Kotlin compile toolchain/target = 17

F0 turns those lessons into repository-owned build intent.

## Toolchain

```text
Android Gradle Plugin = 9.4.0
Gradle                = 9.6.0
Kotlin                = 2.4.20
JVM toolchain         = 17
Java source/target    = 17
Compose BOM           = 2026.09.00
compileSdk            = 37
targetSdk             = 37
minSdk                = 23 (PROVISIONAL product-support floor)
```

AGP 9.4's published compatibility uses Gradle 9.6.0 and JDK 17 and supports API 37. Kotlin
compilation is explicitly toolchain-bound so the Android Studio runtime is not allowed to silently
retarget Java/Kotlin compilation.

## SDK choices

`compileSdk=37` keeps the native implementation on the current Android API surface.

`targetSdk=37` is deliberate rather than automatic. It opts the new standalone app into Android 17
runtime behavior early, while the app has almost no behavior to migrate. This is especially useful
because future VexHome/local-device communication will have to handle Android 17's local-network
permission model honestly instead of postponing that discovery.

`minSdk=23` is provisional. It preserves the reference app's compatibility floor while the
standalone foundation is formed, but it is **not yet an accepted long-term support promise**.
Changing the minimum SDK requires a product/support decision grounded in actual consumers and the
cost of compatibility.

## Application identity

```text
namespace     = com.vextreme.vexlife.android
applicationId = com.vextreme.vexlife.android
```

This is intentionally distinct from the historical reference app's
`com.vextreme.vexlife.r2`, allowing the standalone foundation to coexist during migration.

Release identity, signing, Play distribution, and production versioning remain held.

## Wrapper provenance

The wrapper scripts and wrapper JAR are carried byte-for-byte from the physically proven VexLife
PR847 reference. The wrapper properties select Gradle 9.6.0 and bind the distribution SHA-256.

This reuses a known carrier while removing the monorepo-relative build topology.

## Daemon JVM criteria

`gradle/gradle-daemon-jvm.properties` is intentional shared build configuration, not Gradle cache
residue. It pins the Gradle daemon requirement to JVM 17 so Android Studio cannot silently rewrite
the build runtime to its bundled JBR version on project Sync.

This is separate from the compile toolchain. The daemon executes Gradle; the module toolchain
executes Java/Kotlin compilation. Both are explicit because they solve different reproducibility
problems.

## Repository-local topology

The standalone repository uses normal Gradle project-relative outputs. It does **not** port:

```text
../../generated/android-gradle/**
VexLife source-manifest buckets
VexLife JavaScript generation as a prerequisite to compile
.idea/**
.gradle/**
local.properties
```

If a later generator is justified by a real recurring Android need, add it under its own source and
proof contract.

## First source correction from host proof

The first standalone build also exposed an invalid framework theme reference:
`Theme.DeviceDefault.DayNight.NoActionBar`. F0 now owns a tiny app theme whose parent is the
platform-provided `Theme.Material.Light.NoActionBar`. This keeps the bootstrap dependency surface
small; product theming remains a later foundation.

## First proof sequence

A fresh checkout must eventually establish:

```text
1. Git source state clean
2. Android Studio Gradle Sync succeeds
3. ./gradlew :app:testDebugUnitTest succeeds
4. ./gradlew :app:assembleDebug succeeds
5. debug APK installs on Victor's physical device
6. app launches and shows the F0 standalone-foundation surface
7. a second Git status remains clean except for intentionally untracked local state already ignored
```

A screenshot proves the visible surface. Build output proves the build. Git status proves source
cleanliness. Do not collapse those claims.

## Lived checkpoint — 2026-10-10

Victor ran the standalone repository on the physical Samsung SM-S908U after the daemon-JVM and theme corrections.

Observed visible result:

```text
VexLife Android
Standalone foundation
Source foundation formed; host and device proof in progress.
```

This establishes that the current standalone source could Sync/build/install/launch far enough to render the intended F0 Compose surface on the physical device. Victor also reported no local changes in GitHub Desktop after this round, supporting the intended machine-residue boundary.

Evidence boundary:

- the visible device screenshot proves the rendered F0 surface;
- successful launch implies the build/install path executed sufficiently for that run;
- the clean Git state is Victor's explicit host report;
- `./gradlew :app:testDebugUnitTest` was not separately observed and remains pending;
- this checkpoint is not evidence for later architecture modules.

Stable checkpoint name:

`[Foundation: F0 Gradle/Manifest/Base]`

## What F0 still does not prove

```text
State Relay architecture
intention/operation/result grammar
Design Lab
Home/network/pairing
local-network permission flow
model integration
release signing/distribution
long-term minSdk support
final product visual design
```

Those are later foundations or protected integration stages.

<!-- [VXG RealForever][VEXLIFE-ANDROID][F0-STANDALONE-BUILD] -->
