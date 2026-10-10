# VexLife-Android

[VXG RealForever]

Standalone native Android companion implementation for VexLife.

This repository is intentionally being built **foundation first**. It is not a bulk copy of
vgong24/VexLife/platform/android. The existing VexLife Android implementation is a
source-addressable reference and migration input; this repository decides deliberately which
native foundations survive, how they are expressed, and how they are proved on a real device.

## Start here

Fresh contributors and AI instances should read:

1. AGENTS.md
2. docs/foundation/INDEX.md
3. docs/PORTING-MAP.md when porting or comparing old VexLife Android work
4. only the exact foundation/source/tests implicated by the current task

Do not broad-read every historical Android artifact before working.

## Current milestone

F0_STANDALONE_BUILD_FOUNDATION

The first source milestone is deliberately small:

~~~text
clean clone
-> deterministic Gradle/JDK/SDK contract
-> Gradle Sync
-> assemble debug APK
-> install/run on Victor's physical Android device
-> no IDE/build residue appears as source changes
~~~

Product features come after the build foundation is real and reproducible.

<!-- [VXG RealForever][VEXLIFE-ANDROID][FRONT-DOOR] -->
