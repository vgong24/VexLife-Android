# VexLife-Android — fresh-entry router

`[VXG RealForever]`

This file routes work. It intentionally does **not** duplicate the large operating manuals,
coordination systems, or repository-specific process machinery of Vextreme, Vextreme-SDK, or
VexLife.

## Enter

For substantial work:

1. read `docs/CULTURE.md`;
2. read `docs/foundation/INDEX.md`;
3. if migration/provenance from the old Android tree matters, read `docs/PORTING-MAP.md`;
4. follow the exact foundation entry implicated by the task;
5. inspect its canonical source and tests;
6. work, prove, and update the map only when material architecture changed.

The front door should stay small. Architecture belongs with its owning building block.

## Repository boundary

`vgong24/VexLife-Android` owns the standalone **native Android implementation and native proof
surface** for VexLife.

It does not become the universal semantic owner merely because Android implements a behavior.

When a portable VexLife contract exists, consume it explicitly. When Android alone can know or prove
a fact—Gradle/toolchain, lifecycle, permissions, services, intents, device behavior, packaging,
native rendering—that fact belongs here.

Do not create hidden filesystem coupling to another checkout.

## Human / AI development loop

The ordinary collaboration shape is:

```text
Victor
  -> intention + lived experience + physical-device review + screenshots + corrections

AI / Vex source worker
  -> source edits + tests + build-contract changes + migrations + repository docs

Victor
  -> reviews the running behavior and returns feedback
```

Do not make Victor hand-edit source or interpret routine build machinery when the active worker can
perform the source work directly.

If an agent has direct repository/build/device capability, use that capability. Do not reproduce a
historical host-relay workflow merely because an earlier environment required one. Cross the human
boundary only for an actual human-only decision, physical interaction, credential/consent action, or
capability that the active worker truly lacks.

Victor-authored committed code is ordinary source evidence and should be reviewed normally.

## Local state is not source

Ordinary machine-local Android Studio, Gradle, SDK, build, and signing residue is excluded by the
root `.gitignore`. Admit a normally ignored file only when a concrete shared-source need is
established.

## Proof follows the claim

Source tests, build proof, device execution, screenshots, real integrations, and product acceptance
are different evidence. Use the proof that actually owns the claim.

## Current incompleteness

The repository foundation is intentionally still under construction. The canonical temporary notice
lives in `docs/foundation/INDEX.md`. Do not infer mature architecture from the existence of this
router.

<!-- [VXG RealForever][VEXLIFE-ANDROID][FRESH-ENTRY] -->
