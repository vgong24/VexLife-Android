# F1 — Reusable State Projection SDK Module

`[VXG RealForever]`

## Status

```text
module=:sdk:state
package=com.vextreme.vex.android.state
sourceState=FORMED
moduleTests=NOT_YET_RUN
sampleAppConsumption=SOURCE_WIRED
deviceProof=NOT_YET_RUN
foundationStatus=INCOMPLETE
```

## Why this becomes an SDK building block

State projection is needed by more than one future Android surface: application presentation,
Home/device status, runtime state, Design Lab specimens, and other bounded consumers all need a
consistent way to observe one current semantic state without taking ownership of that state.

That recurring need is concrete enough to justify a reusable module.

The module remains pure Kotlin/JVM. It has no Activity, Compose, Android lifecycle, permission, or
packaging dependency.

## Provenance

Historical reference:

```text
repository=vgong24/VexLife
referencePR=847
referenceHead=7c946bc90b3f8326988e74a32a02551118e9f48e
source=platform/android/state-relay/src/main/kotlin/vexlife/android/state/VexStateProjection.kt
test=platform/android/state-relay/src/test/kotlin/vexlife/android/state/VexStateProjectionContractTest.kt
```

The standalone port preserves the useful behavior while refining placement:

```text
old module = platform/android/state-relay
new module = :sdk:state

old package = vexlife.android.state
new package = com.vextreme.vex.android.state
```

The new namespace reflects the intended reusable Android-SDK role rather than one VexLife app
implementation.

## Owns

- snapshot shape for a bounded consumer projection;
- observation vocabulary carried to Android/Kotlin consumers;
- revision/current-instance admission checks;
- semantic no-op suppression when no distinct transition exists;
- explicit-transition observability;
- stale and contradictory revision rejection;
- defensive copying at retention and every exposed StateFlow read.

## Explicitly does not own

```text
product/domain state ownership
semantic hash generation
transition meaning
event or operation ledger
repository/provider currentness
network/Home truth
permissions or authority
Android lifecycle
Compose rendering
persistence
```

The rightful producer supplies semantic identity. This module only protects and projects the current
snapshot.

## Why defensive copying remains part of the foundation

A StateFlow can be read by many consumers. If a mutable platform object survives admission by
reference, a later caller or consumer mutation can silently rewrite previously accepted state.

Therefore callers must provide `copyValue`, and the projection copies:

- on initial retention;
- on later admission;
- on `state.value`;
- in replay-cache exposure;
- for collected values.

Immutable values can use an identity copy function.

## Sample application role

The sample `:app` consumes `:sdk:state` through `FoundationContract.statusProjection()`.

This is intentionally small. It proves the module is a real dependency of the sample rather than an
unconsumed abstraction. The device surface will show:

```text
SDK state projection: FOUNDATION_INCOMPLETE
```

That visible string is only a demonstration consumer. It does not make the SDK state module the
owner of repository foundation status.

## Proof

Module contract tests cover:

- observation vocabulary;
- defensive initial retention;
- defensive `state.value`;
- defensive replay cache;
- defensive collector values;
- semantic no-op suppression;
- explicit same-value transition emission;
- instance change;
- stale revision rejection;
- contradictory same-revision rejection;
- wrong stateRef rejection;
- defensive copy after admission.

The sample-app test checks that the app actually consumes the SDK module.

Still required:

```text
./gradlew :sdk:state:test
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
physical-device launch
visible SDK state projection line
post-run clean Git source state
```

## Future SDK shape

This module establishes the first candidate pattern:

```text
sdk/
  state/       current semantic-state projection

app/           sample + lived proving consumer
```

Do not infer the names or boundaries of future SDK modules merely from this example. F2/F3 should
earn their own reusable shape from real responsibilities rather than being mechanically split into
folders.

Candidate checkpoint after proof:

`[Foundation: F1 State Projection SDK]`

<!-- [VXG RealForever][VEXLIFE-ANDROID][F1-STATE-PROJECTION] -->
