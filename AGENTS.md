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

## Repository descent map

A fresh worker should be able to descend without predecessor-chat reconstruction:

```text
README.md
-> AGENTS.md
-> docs/CULTURE.md
-> docs/foundation/INDEX.md
   -> exact owning foundation doc/source/tests

feature work
-> docs/features/README.md
   -> feature home
   -> docs/features/FEATURE-WALKS.md when whole-feature completion is the question

proof question
-> docs/foundation/test-proof/README.md
   -> vexVerifyJvm / vexVerifyLocal
   -> Journey / ScreenCrawler / vexVerifyDevice only when that claim needs them

host/device proof unavailable locally
-> docs/foundation/ANDROID-HOST-PROOF.md
```

`./gradlew vexFoundationHealth` checks that this repository-owned descent and the canonical proof
entry points still exist. It is included by `vexVerifyJvm` and therefore by `vexVerifyLocal`.
If that health check fails, repair the rightful source/map/task; do not weaken the check merely to
make verification green.

## Common contribution routes

- **New product feature or major feature evolution:** start with `docs/features/README.md`.
  Describe the human situation/current truth/missing question before new taxonomy, check established
  development vocabulary before minting Vex-specific nouns, and keep one durable feature home current.
  Existing features are worked examples, not mandatory source skeletons.
A contributor should not need to learn testing-framework internals before changing product code.

- **Whole-feature / end-to-end completion:** start with `docs/features/README.md`, then
  `docs/features/FEATURE-WALKS.md`. Its canonical descent is feature identity/home ->
  `FeatureWalks.<feature>...` -> owning `*FeatureWalks.kt` -> bound `*Journeys.kt` and focused
  `*FeatureWalkProofBacklinks.kt` -> named test method. The generated
  `build/reports/vex/feature-walks.txt` is a projection of those links, not another truth owner.
- **New Screen or interaction:** start with the owning `*Screen.kt`, then follow
  `docs/foundation/F1-UI-BLUEPRINT-HOME.md`. Register the Screen once in `AppScreens`, keep
  feature-owned effects beside the Screen/domain, and add focused behavior proof.
- **New or changed user journey:** use the owning feature's `*Journeys.kt` plus the typed
  `Journeys.<domain>.<path>` map. Read `docs/foundation/JOURNEY-INSTRUMENTATION.md` for the
  exact authoring/proof route. You do not need to read every existing journey first.
- **Screenshot / locale / font evidence:** configure
  `VexScreenCrawlerInstrumentedTest.kt`; do not edit crawler planner/evidence internals merely
  because a new Screen or journey exists. See `docs/foundation/SCREEN-CRAWLER-EVIDENCE.md`.
- **Android host proof when the active worker lacks the required local substrate:** start with
  `docs/foundation/ANDROID-HOST-PROOF.md`. Prefer repository-owned `vexVerifyLocal` /
  `vexVerifyDevice` entry points and the live SDK Relay contract; do not reconstruct package,
  launcher, ADB, or RETURN mechanics from predecessor chat.

The journey coverage report names registered production semantics that no journey currently covers,
including the owning Screen where known. Use that as a discovery aid rather than maintaining a
second checklist of every feature path.


## PR current-state cleanliness

For developmental PRs, keep one compact **current-state projection** in the PR body and update it in
place when head/proof state materially changes. Routine commits and proof attempts do not each need a
new conversation comment.

Use comments only when a material failure, protected decision, or causal evidence would be lost by
showing latest state alone. Machine/detail evidence belongs in the owning report/proof bundle.

Coverage runs generate one replaceable PR-body block at:

`build/vex/test-foundation/coverage-status.md`

When an exact-head VexRelay proof runs `vexCoverage`, successful F09 canonical RETURN consumption
must treat that generated block as a bounded provider projection:

~~~text
PASS canonical RETURN
+ exact candidate head still equals PR head
+ exactly one VEX-COVERAGE-STATUS marker block in the coverage proof receipt
  -> use the current direct-provider effect route
  -> append the block when absent, otherwise replace only the existing marked block
  -> continue to Assurance

head mismatch / ambiguous or absent block / provider capability unavailable
  -> do not invent or publish coverage
  -> preserve the proof result and hold only the projection effect
~~~

The host proof itself never edits GitHub. VexRelay carries the evidence; the provider continuation
projects it after F09. No GitHub Actions runner is required.

Update that block in place; do not append one coverage comment per run.

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
