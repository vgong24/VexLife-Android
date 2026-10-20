# Conversation design record — Live truth with controlled Lab perturbations

[VXG RealForever]

Feature ref: `feature.vexlife.conversation`  
Recorded at: **2026-10-16**  
Record ref: `design.feature.vexlife.conversation.2026-10-16.002`

## Discussion / pressure

The first visible Conversation Lab deliberately simulated missing external/provider conditions through
the same repository/state path future production would consume.

That established an important boundary:

~~~text
Lab
-> simulated provider truth
-> normal repository
-> normal Conversation state
-> normal room
~~~

A later debugging question exposed a second need that replacement-only simulation does not answer:

> What if the real feature is exhibiting a bug and we want to keep the real Conversation, real
> currentness, real device/session context and real provider observations, but deliberately change one
> surrounding input to learn whether that input is causal?

Replacing the whole repository with mock data would destroy much of the real bug environment.

## Prior design

The Lab was primarily understood as the missing outside world:

~~~text
SIMULATED PROVIDER
-> Conversation repository
-> Conversation state

plus Lab-owned availability inputs
-> presentation projection
~~~

That worked for an in-memory first practicum, but the availability inputs were named as if the Lab
owned their current truth:

~~~text
homeAvailability
companionAvailability
simulationEnabled
~~~

Those names become misleading once real observations and Lab perturbations can coexist.

## New understanding

There are three different questions and they should keep different owners:

~~~text
FEATURE FLAG
    should a capability participate at all?

SOURCE BINDING
    which provider/repository is canonical?
    real or simulated

LAB PERTURBATION
    which independently owned observed input should be deliberately overridden?
~~~

"Live", "Simulated" and "Mixed" are useful human-facing descriptions, but they are not one giant
runtime mode enum.

~~~text
Live
    real source + no overrides

Simulated
    simulated source + no overrides

Mixed
    real source + one or more explicit overrides
~~~

The architecture stays composable because source binding and perturbation are orthogonal.

## Decisions crystallized

### 1. Convergence owns effective meaning

When Conversation truth, authenticated-participant truth, Home availability, Companion availability
and Lab overrides change independently, one feature-owned Compound Relay reconciles them:

~~~text
Conversation repository -----authenticated participant ----Home availability ------------+-> VexConversationConvergenceRelay.processStates(...)
Companion availability -------/
Conversation Lab ------------/
                                      |
                                      v
                           effective current inputs
                                      |
                                      v
                         VexConversationSurfaceProjection
~~~

The Relay is the only owner of override precedence.

~~~text
CONVERGENCE != PROJECTION
~~~

The Relay decides **which facts are effective**. The existing surface projection maps those already
effective facts into UI-facing shape and does not interpret Lab controls.

### 2. Repository truth is not rewritten for counterfactuals

A Lab availability override does not mutate `VexConversationState` or a provider repository.

~~~text
observed Home = Available
Lab override = Unavailable

repository truth
    unchanged

Convergence output
    Home = Unavailable
    provenance = LAB_OVERRIDE
~~~

Clearing the override reveals the latest observed value, including observations that changed while
the override was active.

~~~text
old observation R1
-> apply override P
-> provider advances to R2
-> clear P
-> effective truth = R2
~~~

It must not restore a stale copy of R1.

### 3. Synthetic provider truth still uses provider/repository ingress

Counterfactual context and provider simulation are different operations.

~~~text
"pretend Vex availability is unavailable"
    -> Lab override
    -> Convergence

"pretend Maya sent this message"
    -> synthetic provider publication
    -> Conversation repository
    -> Convergence observes repository truth
~~~

The Relay must not manufacture messages, membership generations or provider admission.

### 4. Source names should describe the controlled role

The Lab vocabulary becomes:

~~~text
syntheticProviderEnabled
homeAvailabilityOverride
companionAvailabilityOverrides
~~~

instead of a broad `simulationEnabled` plus fields that look like canonical availability truth.

These names let a fresh reader understand that provider simulation and input override are independent
capabilities.

### 5. Provenance remains outside product domain objects

Debug/Design-Lab surfaces may need to explain:

~~~text
Home availability     OBSERVED
Vex availability      LAB_OVERRIDE
Conversation messages OBSERVED
~~~

That provenance belongs to Convergence/debug output. It does not add `isMock`, `isSynthetic` or
similar developer flags to `VexConversationMessage`, participants or Family bindings.

### 6. No new generic Reality/Mock framework is introduced

The repository already has the reusable mechanism needed for several changing truths:

~~~text
VexCompoundRelay
dependencies()
processStates()
outputStates
~~~

Conversation reuses that foundation.

Events remains the first worked Convergence consumer; Conversation now provides independent pressure
that developer Lab truth may participate as another dependency without becoming the feature state
owner.

No `VexRealityEngine`, generic Mock repository hierarchy, global FeatureLab framework or generic
Live/Mock/Mixed registry is earned by this refinement.

## Test consequences

The feature-level proof should make the intended relationship readable:

~~~text
observed truth + no override
    -> observed effective truth

observed truth + override
    -> only the overridden input changes

provider changes while override is active
    -> override remains effective

clear override
    -> latest provider observation becomes effective

synthetic provider message
    -> repository must change before Convergence sees the message
~~~

These are reducer/domain tests. Physical-device/Journey proof remains tied to whatever controls are
eventually admitted into the registered production Screen/Debug Menu.

## Current design consequence

The next feature-local implementation is:

~~~text
VexConversationLab
    explicit provider-simulation controls
    explicit availability overrides

VexConversationConvergenceRelay
    one retained coherent input family
    one serialized processStates()
    explicit observed-vs-Lab provenance

VexConversationSurfaceProjection
    pure projection of already-effective facts
~~~

The shared app/session/Debug paths remain separately coordinated. Do not create a second repository,
Compose-owned truth, or Debug singleton just to demonstrate this design before those paths reconcile.

See current synthesis: `../README.md`

<!-- [VXG RealForever][feature.vexlife.conversation][DESIGN-RECORD-002] -->
