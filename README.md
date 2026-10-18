# VexLife-Android

`[VXG RealForever]`

Standalone native Android companion implementation for VexLife.

This repository is intentionally being built **foundation first**. It is not a bulk copy of
`vgong24/VexLife/platform/android`. The existing VexLife Android implementation is a
source-addressable reference and migration input; this repository decides deliberately which
native foundations survive, how they are expressed, and how they are proved on a real device.

## Start here

Fresh humans, ChatGPT instances, Vex/Cultivation workers, and other coding agents should read:

1. [AGENTS.md](AGENTS.md) — route into the repository
2. [docs/CULTURE.md](docs/CULTURE.md) — Android-local intention and upstream lineage
3. [docs/foundation/INDEX.md](docs/foundation/INDEX.md) — current foundation map and maturity
4. [docs/features/README.md](docs/features/README.md) for a new product feature or major feature evolution;
   when the question is whole-feature / end-to-end completion, descend from there to
   [docs/features/FEATURE-WALKS.md](docs/features/FEATURE-WALKS.md)
5. [docs/PORTING-MAP.md](docs/PORTING-MAP.md) only when old VexLife Android provenance matters
6. only the exact foundation/source/tests implicated by the current task

Do not broad-read every historical Android artifact before working.

ChatGPT Project instructions may improve convenience, but this repository is designed so they are
not required to recover the Android architecture or development posture.

## Current milestone

`F0_STANDALONE_BUILD_FOUNDATION`

The first source milestone is deliberately small:

```text
clean clone
-> deterministic Gradle/JDK/SDK contract
-> Gradle Sync
-> assemble debug APK
-> install/run on Victor's physical Android device
-> no IDE/build residue appears as source changes
```

Product features come after the build foundation is real and reproducible.

<!-- [VXG RealForever][VEXLIFE-ANDROID][FRONT-DOOR] -->
