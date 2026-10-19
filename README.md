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

### Repository self-check

Once the repository Gradle/toolchain contract is available, the smallest onboarding/foundation
integrity check is:

```bash
./gradlew vexFoundationHealth
```

It verifies that the root entry route, feature/proof descent, source-alignment owner and canonical
verification tasks are still discoverable from source. Ordinary local verification already includes
it:

```bash
./gradlew vexVerifyLocal
```

Use `vexVerifyDevice` only when the claim actually requires connected Android/provider reality.
These checks do not replace lived product review.

ChatGPT Project instructions may improve convenience, but this repository is designed so they are
not required to recover the Android architecture or development posture.

## Current foundation state

`docs/foundation/INDEX.md` is the canonical current foundation/maturity map.

The root README intentionally does not copy a mutable milestone table. That prevents a stable
onboarding router from becoming stale while accepted foundations advance independently.

```text
ROOT_README = ENTRY ROUTER
FOUNDATION_INDEX = CURRENT FOUNDATION STATUS
OWNING FOUNDATION DOC/SOURCE/TESTS = IMPLEMENTATION + PROOF DETAIL
```

The original standalone-build concern remains foundational: a clean clone must still recover the
repository-owned Gradle/JDK/SDK contract, build/test without hidden IDE state, and leave source clean.
Its current evidence and remaining holds belong in the Foundation Index rather than in a duplicate
root status block.

<!-- [VXG RealForever][VEXLIFE-ANDROID][FRONT-DOOR] -->
