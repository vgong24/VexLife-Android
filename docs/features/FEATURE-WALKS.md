# Feature Walk completion traceability

[VXG RealForever]

## The question

> When focused behavior, renderer, repository, provider and device proofs are individually healthy,
> can a developer still see whether a person can complete the whole feature outcome through the
> installed app—and exactly which boundary is missing when they cannot?

Feature Walks answer that completion/traceability question.

They were earned by two independent pressures:

~~~text
Conversation
    deep repository / Lab / Hybrid / room proof existed
    installed app had no human doorway into the room

VexMaps
    destination shell Journey existed
    deeper search/navigation proofs existed
    ordinary find -> route -> navigate experience remained incomplete
~~~

The missing owner was not another executor. It was a small feature-owned map connecting the whole
human outcome to the Journey and focused proof owners that already exist.

## Vocabulary

~~~text
Feature
    existing stable semantic feature identity

Feature Walk
    one whole human/product outcome whose completion matters

Case
    one materially distinct path/branch of that outcome

Checkpoint
    one observable boundary that must be true for the Case

Journey
    executable Android human walk for one currently unheld required Case

Proof backlink
    machine-checked locator to an existing focused test for one Checkpoint

Environment
    Journey execution condition such as baseline, locale or font scale
~~~

Environment stays orthogonal. `VexJourneyCase` remains the execution identity
`shard/journey@environment`; `VexFeatureWalkCase` is the product-completion identity.

## Permanent distinctions

~~~text
FEATURE_WALK != JOURNEY
FEATURE_WALK_CASE != VEX_JOURNEY_CASE
CHECKPOINT_PROOF != WHOLE_WALK_COMPLETE
PROOF_BACKLINK != TEST_PASS
JOURNEY_BINDING != JOURNEY_EXECUTION_PASS
ROOM_INTERACTION_PROVEN != USER_CAN_REACH_ROOM
~~~

A focused proof can satisfy one Checkpoint while the Case remains held by another. A structural report
must never promote linked source into runtime PASS, semantic clearance or product acceptance.

## Stable identity

Feature Walk addresses extend the feature's existing stable ref:

~~~text
feature.vexlife.conversation/W01/C01/P01
feature.vexmaps/W01/C02/P01
~~~

~~~text
Wnn = Feature Walk
Cnn = Case
Pnn = Checkpoint
~~~

The dense indices are permanent addresses, not list positions. Do not renumber existing identities to
insert a newly discovered Walk/Case/Checkpoint. Append the next identity and use typed Kotlin names for
human readability:

~~~kotlin
FeatureWalks.conversation.simulatedFamily.roundTrip
FeatureWalks.vexMaps.destinationNavigation.destinationShell
~~~

Authored truth:

- feature-owned Walk/Case/Checkpoint names and commitments;
- stable refs;
- exact holds and release predicates.

Derived truth:

- the flattened catalog;
- Journey binding projection;
- forward/reverse proof-link report;
- environment-specific Journey case refs.

## Source shape

~~~text
app/src/journeyTest/kotlin/.../testing/featurewalk/
    VexFeatureWalk.kt
    FeatureWalks.kt
    ConversationFeatureWalks.kt
    VexMapsFeatureWalks.kt
    VexFeatureWalkJourney.kt

app/src/test/kotlin/.../<feature>/
    *FeatureWalkProofBacklinks.kt

app/src/test/kotlin/.../testing/featurewalk/
    VexFeatureWalkContractTest.kt
~~~

The source lives beside Journey/test proof, not in production runtime. It does not route the app,
register Screens, form providers, or decide product acceptance.

## Journey relationship

A currently required Case expects exactly one typed Journey binding only when every required
Checkpoint is unheld:

~~~kotlin
vexFeatureWalkJourney(
    featureWalkCase =
        FeatureWalks.vexMaps.destinationNavigation.destinationShell,
) {
    ...
}
~~~

The Case owns the Journey's local ID/title. This prevents a second handwritten path identity while
preserving Journey as the execution owner.

A held Case must not form a placeholder Journey merely to make the catalog look complete. The hold is
the useful result.

## Focused proof relationship

The first experiment uses explicit typed backlinks next to the owning feature tests. Each backlink
names:

~~~text
Checkpoint ref
-> exact test class
-> exact @Test method
~~~

The contract test verifies that the Checkpoint exists and the method still exists as a real JUnit
test. The backlink itself does not claim that the test ran or passed.

This bounded source-locator form is intentional. A classpath scanner, annotation processor or KSP
index is not justified until more real consumers show that explicit backlinks are the recurring
burden. Do not add that machinery pre-emptively.

## Current first consumers

### Conversation

~~~text
feature.vexlife.conversation/W01/C01
    Victor sends and Maya responds in the simulated Family room

P01 HELD
    installed human doorway is absent

P02 linked focused proof
    Victor Send remains on the repository contract

P03 linked focused proof
    Maya simulated provider ingress reaches canonical Conversation truth

P04 HELD
    same installed room cannot execute/render the round trip until P01 exists
~~~

The Case has no Journey yet. That is the intended visibility, not a testing failure.

### VexMaps

~~~text
feature.vexmaps/W01/C01
    existing destination-shell Journey

feature.vexmaps/W01/C02
    focused search-currentness proof exists
    whole installed find -> route -> navigate checkpoint remains HELD
~~~

This proves the vocabulary is not Conversation-specific and that a Journey, focused proof and whole
feature completion can truthfully disagree.

## Structural report

`VexFeatureWalkContractTest` writes:

~~~text
build/reports/vex/feature-walks.txt
~~~

The report projects only:

- stable hierarchy and uniqueness;
- currently expected Journey bindings;
- focused proof source backlinks;
- holds and release predicates.

It explicitly records:

~~~text
proofExecution=NOT_ASSERTED
journeyExecution=NOT_ASSERTED
~~~

Behavioral proof still comes from the named focused tests and Journey/device execution.

## Adding a Feature Walk

1. Reuse the feature's existing stable `FEATURE_REF`.
2. Add a Walk only for a whole human outcome that materially needs completion traceability.
3. Split Cases only for materially different human paths, not device environments.
4. Name observable Checkpoints at their rightful boundaries.
5. Mark required holds with a concrete reason and release predicate.
6. Bind one typed Journey when the required Case becomes unheld.
7. Backlink existing focused tests to the Checkpoints they actually prove.
8. Run the structural contract plus the real focused/Journey/native proof required by the claim.

Do not create a Feature Walk for every unit test, Screen, action or implementation layer. The
contract exists to reveal whole-outcome gaps that owner-local proof cannot reveal by itself.

<!-- [VXG RealForever][VEXLIFE-ANDROID][FEATURE-WALKS][END-TO-END] -->
