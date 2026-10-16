# Debug Menu — Current Design

[VXG RealForever]

Last meaningfully updated: **2026-10-15T23:41-07:00**  
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

Planned first feature-lab consumer of the refined pattern.

Debug Menu should expose high-level controls such as seed/reset/add participant/currentness/provider
availability, but Conversation owns what those controls mean and how state is formed.

Conversation's current scenario design lives at:

`../../../../conversation/design/README.md`

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
