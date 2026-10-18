# Debug Menu — Current Design

[VXG RealForever]

Last meaningfully updated: **2026-10-16**  
Current design record: `records/2026-10-15-001-feature-lab-pattern.md`

## Current intention

Keep Debug Menu small and scalable while allowing features to create deterministic lived conditions
that would otherwise require unavailable providers, devices or external humans.

## Fractal lab relationship

~~~text
Debug Menu
    registers/displays developer controls
        ↓
feature-owned Lab/controller
    knows feature-specific synthetic conditions
        ↓
normal feature repository/state
        ↓
normal product screen
~~~

The Debug Menu layer should be able to answer:

~~~text
Which feature lab/control is this?
What developer action is exposed?
Which rightful owner receives it?
~~~

It should not need to know the feature's internal semantic model.

## Why this scales

A future feature can add a lab without editing one giant Debug state machine.

Conceptually:

~~~text
debug/
    reusable developer surface

events/
    Events synthetic controller + scenarios

conversation/
    Conversation Lab + scenarios

maps/
    future Maps lab only if real pressure earns it
~~~

The visible controls may still be composed on one Debug Menu screen. Semantic ownership stays
distributed.

## Current consumer examples

### Events

Already implemented.

Synthetic event controls publish Events conditions and exercise normal Events owners.

### Conversation

Conversation is now an accepted feature-local consumer of the pattern.

Accepted source owns:
~~~text
VexConversationLab
    synthetic-provider controls
    explicit Home / Companion availability overrides

VexConversationHybridRelay
    one mixed-provenance current relationship
    observed-vs-Lab provenance
~~~

The production Debug Menu doorway is still intentionally **not wired** because the shared Debug /
AppAction / app-session paths are under active ownership elsewhere.

When that doorway is admitted, Debug should expose high-level controls into the existing
Conversation-owned Lab/Hybrid relationship. It must not recreate Conversation state, override
precedence, or provider simulation inside Debug.

Conversation current design lives at:

`../../../../conversation/design/README.md`

## Hybrid-aware developer controls

Conversation added a useful distinction to the original feature-lab pattern:

~~~text
synthetic provider condition
    changes provider/repository truth through normal ingress

Lab override
    changes one effective surrounding observation
    without rewriting repository truth

Hybrid Relay
    reconciles observed + overridden facts once
~~~

Debug Menu should not need to implement that precedence itself.

A future reusable Debug connective contract may be earned if several features expose compatible
feature-owned Hybrid/Lab capabilities. That contract is **not** admitted yet.

~~~text
DEBUG HOSTS CONTROLS
!=
DEBUG OWNS HYBRID SEMANTICS
~~~

Do not add a global Lab/Hybrid registry merely because Conversation now has one Hybrid Relay.

## Rule for adding another lab

Before adding controls, answer:

~~~text
What unavailable external condition are we simulating?
Which feature owns that condition?
Which normal repository/state path will receive it?
What visible product behavior should react?
Could the same proof be obtained without another lab abstraction?
~~~

If the last answer is yes, do not add ceremony.

<!-- [VXG RealForever][DEBUG-MENU][CURRENT-DESIGN] -->
