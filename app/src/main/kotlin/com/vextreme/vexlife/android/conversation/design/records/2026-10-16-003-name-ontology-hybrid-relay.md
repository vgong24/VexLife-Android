# [Name Ontology] Conversation record — Hybrid names the mixed-provenance relationship

[VXG RealForever]

Feature ref: `feature.vexlife.conversation`  
Recorded at: **2026-10-16**  
Record ref: `design.feature.vexlife.conversation.2026-10-16.003`  
Tags: **[Name Ontology] [Conversation] [Hybrid]**

## Reader protocol

Cold-read the old name before reading this conclusion:

~~~text
VexConversationConvergenceRelay
~~~

Without opening its implementation or KDoc, write down what you think it owns.

Useful prompts:

~~~text
Does it own all Conversation convergence?
Does it reconcile provider ingress?
Does it merge Family history/currentness?
Does it decide Send admission/results?
Does it own UI/presentation only?
What consequence radius would you expect from changing it?
~~~

Then inspect the actual relationship.

## Product/source evidence

The Relay starts from already-admitted Conversation repository truth and independently owned
surrounding observations:

~~~text
Conversation repository truth
authenticated participant
Home availability
Companion availability
Conversation Lab overrides
        |
        v
one retained current relationship
        |
        v
Conversation projection / downstream consumers
~~~

It does not:

~~~text
admit Conversation messages
own Family membership/history
execute Send
replace provider authority
own the whole Conversation domain
~~~

Synthetic provider messages still enter through the repository before this relationship can observe
them.

## Cold-read mismatch

The historical name:

~~~text
VexConversationConvergenceRelay
~~~

correctly identifies the Relay mechanism, but the semantic stem `Conversation` plus the broad role
`Convergence` reasonably suggests a general Conversation-wide convergence owner.

That consequence radius is larger than the class actually owns.

A proposed correction added `Surface`:

~~~text
VexConversationSurfaceHybridRelay
~~~

but the same cold-read method exposed another mismatch. The relationship affects effective identity,
availability, provenance and action eligibility before UI rendering. Surface is a consumer/result
shape, not the complete hybrid relationship.

No second independent Conversation Hybrid relationship currently exists.

Therefore the current name is:

~~~text
VexConversationHybridRelay
~~~

## What Hybrid means here

`Hybrid` is not shorthand for "mock" or "debug".

It names a specialized Compound relationship where differently sourced facts may coexist while their
ownership and provenance remain intact.

~~~text
canonical / observed truth
+
explicit noncanonical contribution
    synthetic condition
    Lab override
    future other qualified provenance
        |
        v
one effective current relationship
~~~

Current Conversation laws:

~~~text
HYBRID != MOCK
HYBRID != DEBUG
HYBRID IS-A SPECIALIZED CONVERGENCE RELATIONSHIP

SOURCE_BINDING != LAB_PERTURBATION
LAB_OVERRIDE != REPOSITORY_TRUTH
HYBRID_OUTPUT != CANONICAL_SOURCE
~~~

Removing a Lab override reveals the latest observed truth, not a stale snapshot captured when the
override began.

## Why there is no <Thing> stem yet

Do not invent categories merely to make the naming grammar look extensible.

Today Conversation has one earned Hybrid relationship.

Provider simulation remains repository/provider ingress, not a ProviderHybridRelay.
Send remains the Family repository/effect contract, not a SendHybridRelay.
Family/history composition remains adapter/provider responsibility, not a HistoryHybridRelay.

Therefore:

~~~text
VexConversationHybridRelay
~~~

is more truthful than:

~~~text
VexConversationSurfaceHybridRelay
VexConversationProviderHybridRelay
VexConversationSendHybridRelay
VexConversationHistoryHybridRelay
~~~

If Conversation later earns a second independent Hybrid relationship, apply Name Ontology again and
introduce the question stem then:

~~~text
VexConversation<ActualQuestion>HybridRelay
~~~

The suffix is earned by real distinction, not future-proofing symmetry.

## Foundation pressure

The reusable semantic role appears broader than Conversation:

~~~text
VexCompoundRelay
    several independently changing truths
    -> one coherent current relationship

Hybrid
    a Compound relationship where mixed provenance is intentional,
    provenance remains visible,
    canonical ownership remains intact,
    and noncanonical contributions are removable
~~~

The rightful reusable source home is the Relay foundation.

That source is currently under active ownership in PR #77, so this Conversation refinement does not
create a competing `VexHybridRelay` foundation type locally.

Instead, Conversation routes second-consumer pressure to the Relay owner.

A future reusable `VexHybridRelay` should carry real semantic obligations rather than be an empty
marker. At minimum:

~~~text
canonical truth remains independently retained
provenance remains inspectable
noncanonical contributions are removable
canonical currentness may continue while a contribution is active
removal reveals latest canonical truth
one relationship owner decides precedence
~~~

A generic Debug connective contract is also not invented here. It should be earned from multiple
feature-owned Hybrid/Lab consumers rather than teaching Debug Menu one feature's control schema.

## Semantic identity continuity

The Kotlin type name and the Relay semantic identity string are different naming layers.

The current Relay keeps:

~~~text
relayRef = "relay.conversation.convergence"
~~~

for this naming-only refinement.

Changing a durable semantic identity is a separate compatibility/currentness decision; it is not
implicitly authorized by a Kotlin symbol rename.

~~~text
CODE_SYMBOL != SEMANTIC_IDENTITY
~~~

## Historical search continuity

Preserve these historical coordinates:

~~~text
VexConversationConvergenceRelay
VexConversationConvergenceInputState
VexConversationConvergenceOutputState
VexConversationInputProvenance
Conversation Convergence
~~~

They remain searchable history, not current naming authority.

## Current consequence

Current source vocabulary:

~~~text
VexConversationHybridRelay
VexConversationHybridInputState
VexConversationHybridOutputState
VexConversationHybridInputProvenance
~~~

The architecture itself is unchanged:

~~~text
repository/provider truth
    remains canonical

Lab
    owns synthetic-provider controls and explicit overrides

Hybrid Relay
    owns the mixed-provenance current relationship

Surface Projection
    maps already-effective facts

Room
    renders
~~~

See current synthesis: `../README.md`

<!-- [VXG RealForever][feature.vexlife.conversation][DESIGN-RECORD-003][Name Ontology][Hybrid] -->
