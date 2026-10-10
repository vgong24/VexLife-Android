# VexLife -> VexLife-Android Porting Map

[VXG RealForever]

## Purpose

Preserve the useful Android architecture and the lessons that produced it without making the
standalone repository a copy of VexLife's monorepo layout.

This map records provenance and migration intent. It does not make old source current here.

## Lived reference checkpoint

Reference repository:

~~~text
repository=vgong24/VexLife
referencePR=847
referenceBranch=VXG-100926-vexlife-android-link-design-lab-slice-01
referenceHead=7c946bc90b3f8326988e74a32a02551118e9f48e
referenceTree=e48865fd2705669458e5c838e9aaf829d5fa0d9f
referenceChangedFiles=24
~~~

Victor's physical-device review on 2026-10-09 established, for this exact reference candidate:

- Android Studio could Sync/build after the Gradle JDK was changed to installed Temurin 17;
- the application installed and launched on the physical Android device;
- the primary screen displayed the Android runtime architecture reference surface;
- the debug/developer Design Lab doorway was visible and opened successfully;
- the VexButton specimen executed;
- the Design Lab Output Console visibly reported a local specimen action with
  preRevision=0, result=ADMITTED_LOCAL_FIXTURE, and postRevision=1.

This is a bounded lived reference, not blanket acceptance of every old architectural choice.

## Toolchain lesson already earned

The reference project declared Kotlin JVM 17 in state-relay, but Java compilation inherited the
JDK used to launch Gradle. Under Android Studio Rabbit 1's embedded JBR/JDK 25 this produced:

~~~text
compileJava target = 25
compileKotlin target = 17
-> inconsistent JVM target compatibility
~~~

Selecting the installed Temurin 17 Gradle JDK allowed the reference build to proceed.

Migration consequence:

**VexLife-Android should declare a repository-owned JVM/toolchain contract so IDE runtime upgrades
do not silently become source/build semantics.**

## Do not port as source

The following were observed as Android Studio / Gradle / machine-local state and should not be
migrated:

~~~text
platform/android/.gradle/**
platform/android/.idea/**
platform/android/local.properties
module/root build output
APK/AAB artifacts as source
machine signing configuration
~~~

The standalone root .gitignore owns this boundary.

## Monorepo assumptions to reconsider rather than copy

The reference Android project used repository-relative paths such as:

~~~text
../../generated/android-gradle/kotlin-persistent
../../generated/android-gradle/<module>
~~~

Those are VexLife monorepo placement decisions. The standalone repository must choose its own
build/cache/generated topology from its actual needs.

Likewise, JavaScript generators and VexLife source-manifest machinery are not automatically Android
runtime dependencies. Port only a generator/contract when the Android repository has a real
consumer for that mechanism.

## Port order

~~~text
F0 standalone build/toolchain
-> F1 state projection
-> F2 intention / operation / attempt / result
-> F3 composition + lifecycle boundary
-> F4 localization + presentation primitives
-> F5 Design Lab
-> separately admitted real Home / transport / device capabilities
-> separately admitted model/companion integrations
~~~

A later block may cause an earlier foundation to be refined. Preserve the reason and affected proof;
do not treat the sequence as a prohibition on learning.

## Cross-repository seam

Desired direction:

~~~text
portable VexLife meaning / accepted contract
        |
        | explicit versioned interface
        v
VexLife-Android native implementation
        |
        +-> Android-specific lifecycle / permissions / services
        +-> Compose/native rendering
        +-> Android packaging and device proof
~~~

Avoid:

~~~text
VexLife-Android
  -> arbitrary relative filesystem reach into a VexLife checkout
  -> hidden dependency on VexLife's generated directory topology
  -> copying portable semantics into Android-only code without an owner
~~~

## Immediate next migration edge

F0_STANDALONE_BUILD_FOUNDATION.

Do not bulk-copy platform/android. Construct the smallest standalone Gradle/Android project that
can prove the toolchain and device path, then port the next building block with its provenance and
proof.

<!-- [VXG RealForever][VEXLIFE-ANDROID][PORTING-MAP] -->
