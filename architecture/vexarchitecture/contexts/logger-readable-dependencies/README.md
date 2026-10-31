# VexLogger + readable dependency relationships — architecture context

[VXG RealForever]

> **CONTEXT / LAYDOWN PR — NOT AN IMPLEMENTATION CONTRACT**
>
> This directory preserves Victor's current relational architecture sketches in repository-readable
> form so a fresh instance can continue the VexLogger / dependency-foundation work without rebuilding
> the intention from chat history.
>
> Sibling foundation snapshot:
> https://github.com/vgong24/VexLife-Android/pull/207
>
> This PR deliberately **continues from #207's current snapshot head**. #207's history remains
> untouched; this branch contains only the additional Logger/readable-dependency context on top.
> It still does not imply either PR should merge.

## Entry order

Read in this order:

~~~text
1. PR #207: architecture/vexarchitecture/VEXLOGGER.md
2. this README.md
3. SOURCE-SKETCHES.md
4. RELATIONAL-BRIDGE.md
~~~

The Logger contract comes first because traceability already has an owner. The dependency work must
not reintroduce hidden tracing identity, execution history, or parallel logger roots while trying to
make dependency construction readable.

## Intention

The current work is broader than conventional dependency injection.

The goal is to make relationships **readable as architecture** rather than borrowing apparent
simplicity from a container whose important relationships are only visible after framework/compiler
resolution.

The current direction being explored is:

~~~text
declared Modules
-> readable dependency relationships
-> deterministic graph construction
-> memoized live instances
-> explicit lifetime placement
-> Families/components receive what they depend on
-> VexLogger observes accepted/observed facts across those rightful owners
~~~

The relationship map should help a human or fresh AI answer:

~~~text
What depends on what?
What must exist first?
Which live instance is reused?
What lifetime owns it?
Where does this Family come from?
What UI consumes it?
What semantic action occurred?
What result actually happened?
What was actually shown to the user?
~~~

without introducing a second hidden ontology to answer those questions.

## Source status vocabulary

This context uses four statuses:

~~~text
SKETCH
    directly preserves Victor's current visual architecture thinking

SETTLED FROM #207
    already-refined architecture law that this work must respect

BRIDGE INTERPRETATION
    relational reading that connects the sketch to settled architecture;
    useful, but not automatically a final API

TO DISCUSS
    intentionally unresolved; do not silently decide it in implementation
~~~

## Important separations

Do not collapse these merely because they can all be called "identity":

~~~text
UI element identity
    which visible/interactive element acted

semantic Control identity
    which semantic request was made

dependency relationship identity
    which dependency/provider instance is being constructed/reused

execution RelationshipId
    which active semantic work relationship owns current Job(s)

Logger chronology
    what accepted/observed facts happened and in what order
~~~

A readable architecture may connect these coordinates, but one coordinate is not permission to replace
the others.

## Current work sequence

The intended human work sequence is:

~~~text
solidify VexLogger traceability foundation
-> lay down readable dependency relationship foundation
-> establish lifetime/readability rules
-> return to Family
-> connect Family to dependency construction without hiding relationships
-> connect accepted/observed Family/runtime facts back through VexLogger
~~~

Do not rush directly into a DI implementation from these sketches.

<!-- [VXG RealForever][VEXARCHITECTURE][LOGGER][READABLE-DEPENDENCIES][CONTEXT] -->
