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
4. [docs/PORTING-MAP.md](docs/PORTING-MAP.md) only when old VexLife Android provenance matters
5. only the exact foundation/source/tests implicated by the current task

Do not broad-read every historical Android artifact before working.

ChatGPT Project instructions may improve convenience, but this repository is designed so they are
not required to recover the Android architecture or development posture.

## Current milestone

The standalone build foundation is established enough to support architecture work.

Current review slice:

~~~text
F1 architecture/relay
   VexRelay + VexCompoundRelay

F2 architecture/execution
   VexAppScope

F3 architecture/observability
   VexLogger
~~~

These three foundations are being reviewed/proved as one reusable Android architecture threshold.

Read:

1. docs/foundation/INDEX.md
2. architecture/relay/README.md
3. architecture/execution/README.md
4. architecture/observability/README.md
5. docs/foundation/POST-F1-F3-GAPS.md

The next tooling phase after an accepted F1-F3 checkpoint is code coverage. Component-first Compose
construction (container/list/Tooltip/Snackbar/Design Lab) follows after that.

The repository foundation remains explicitly incomplete; current architecture must still pass its
focused/full Gradle proof and review before checkpoint/merge.

<!-- [VXG RealForever][VEXLIFE-ANDROID][FRONT-DOOR] -->
