# Vex Feature Template

[VXG RealForever]

> Copy only the sections that help the feature think and recover. Delete sections with no truthful
> distinct answer. This is not a documentation quota.

## Feature identity

~~~text
STABLE FEATURE REF:
CURRENT HUMAN NAME:
HISTORICAL / SEARCH NAMES:
FEATURE / HOME:
CODE IDENTITY ANCHOR:
HUMAN OUTCOME:
CURRENT STATUS:
SEMANTIC OWNER(S):
CURRENT NEXT EDGE:
~~~

The stable feature ref is semantic continuity, not the current spelling of the feature. When a
feature is renamed, preserve the ref and retain prior names as searchable aliases at the code-local
identity anchor.

## Experience sentence

> A **[WHO]** comes to **[HOME]** because **[WHY]**.  
> **[WHAT]** tells them what domain things are known.  
> **[WHERE]** tells them where those things can come from/go/be realized.  
> **[HOW]** names the important behavioral question(s).  
> **[NOW / WHEN]** says what is actually true now.  
> **[WHAT CAN I DO?]** names the stable Control/request.  
> **[WHAT DOES IT MEAN NOW?]** names the semantic Act when Behavior applies.  
> **[WHAT SHOULD RUNTIME ATTEMPT?]** names the Effect/concrete request.  
> **[WHO / WHAT REALIZES IT?]** names the rightful runtime owner.  
> **[WHAT DO THEY SEE?]** names the result/projection.

The feature experience is an orientation lens. Do not create a `<Name>Experience` source object
unless that object has a real runtime/source responsibility.

## Human question map

~~~text
HOME:
WHO:
WHY:
WHAT:
WHERE:
HOW:
NOW / WHEN:
WHAT CAN I DO?:
WHAT DOES IT MEAN NOW?:
WHAT SHOULD RUNTIME ATTEMPT?:
WHO / WHAT REALIZES IT?:
WHAT DO THEY SEE?:
~~~

## Source-concept formation

For each proposed durable noun:

~~~text
HUMAN SITUATION:
CURRENT TRUTH / OWNER:
REAL PRESSURE:
MISSING QUESTION:
RELATIONSHIPS:
BOUNDARY — MUST NOT OWN:
ESTABLISHED SOFTWARE TERM(S):
EXISTING RIGHTFUL HOME:
ORDINARY DESCRIPTION WITHOUT PROPOSED NOUN:
PROPOSED NAME:
WHY THIS SOURCE SHAPE IS NEEDED:
PROOF OF ORIGINAL MISSING QUESTION:
~~~

### Name-before-context check

Before reading the implementation that explains the proposed name:

~~~text
NAME / PATH:
WHAT WOULD A FRESH DEVELOPER PREDICT IT MEANS?:
WHO WOULD THEY PREDICT OWNS IT?:
WHAT CONSEQUENCES WOULD THEY EXPECT FROM CHANGING IT?:
~~~

After reading implementation:

~~~text
WHAT IT ACTUALLY MEANS:
PREDICTION DELTA:
DOES NAME / PLACEMENT NEED INVESTIGATION?:
~~~

A comment may deepen a name. It should not be required to discover the conceptual neighborhood.

## Representation-versus-role check

~~~text
VALUES SHARE THE SAME REPRESENTATION?:
DO THEY ANSWER THE SAME QUESTION?:
DO THEY HAVE THE SAME OWNER?:
~~~

Do not unify IDs/keys/refs/states merely because they are all Strings or data classes.

## Package placement

~~~text
HUMAN FEATURE AREAS:
TECHNICAL ROLES INSIDE EACH AREA:
CAN A NEWCOMER FIND THE AREA WITHOUT KNOWING INTERNAL FRAMEWORK TERMS?:
~~~

## Continuity growth

Use only when the feature's evolving design has become too important to reconstruct from one README.

~~~text
<feature>/
├── README.md
├── Vex<Feature>Docs.kt
└── design/
    ├── README.md
    └── records/
        └── YYYY-MM-DD-NNN-<design-refinement>.md
~~~

design/README.md is the current intended design, not a diary.

Each dated record captures one meaningful refinement:

~~~text
FEATURE REF:
RECORDED AT:
DISCUSSION / PRESSURE:
PRIOR DESIGN:
NEW UNDERSTANDING:
DECISIONS CRYSTALLIZED:
WHAT REMAINS OPEN:
CURRENT DESIGN CONSEQUENCE:
~~~

A record links to the stable feature ref so it survives feature/package renames. Preserve old records
rather than rewriting the past; currentize the design synthesis instead.

If repeated independent decisions later need stable addresses of their own, a feature-local
design/decisions/ subpackage may be earned. Do not create it pre-emptively.

## Current source / ownership map

~~~text
<smallest useful map from human question -> semantic owner -> runtime owner -> presentation/proof>
~~~

## Reused foundations

~~~text
FOUNDATION / ESTABLISHED PATTERN:
WHY IT FITS:
WHAT THIS FEATURE DOES NOT OWN:
~~~

## Runtime / provider map

~~~text
SEMANTIC QUESTION:
CONNECTION / PROVIDER / REPOSITORY / SERVICE:
VENDOR / NATIVE SDK:
LIFETIME OWNER:
FAILURE / UNAVAILABLE OWNER:
~~~

## Human surfaces

~~~text
SCREEN / SURFACE:
WHAT IT PROJECTS:
WHICH CONTROLS IT EXPOSES:
WHAT IT DOES NOT OWN:
~~~

## Acceptance walk

### <human request>

~~~text
GIVEN
    <current State / source / authorization facts>

WHEN
    <Control or ordinary human request>

MEANS
    <Act, when Behavior owns this relationship>

ATTEMPTS
    <Effect / concrete transformation>

REALIZED BY
    <Connection / provider / repository / service / SDK owner>

OBSERVED AS
    <new truth / result>

EXPERIENCED AS
    <projection / visible outcome>

PROVED BY
    <focused + aggregate proof appropriate to the claim>
~~~

Repeat only for materially distinct walks.

## Current holds

~~~text
HELD:
WHY:
WHAT REAL CONSUMER / EVIDENCE WOULD EARN IT:
~~~

## Completion

~~~text
[ ] Feature home matches current source
[ ] Names/paths predict their conceptual neighborhood
[ ] Established vocabulary was checked before new Vex vocabulary
[ ] New durable concepts answer real missing questions
[ ] Focused semantic tests pass
[ ] Changed Journey exists when warranted
[ ] Native/provider proof used for native/provider claims
[ ] Appropriate vexVerifyJvm / vexVerifyLocal / vexVerifyDevice run
[ ] Generated/unexpected source effects reconciled
[ ] Stale experimental/duplicate routes removed or deliberately held
[ ] Next unfinished edge explicit, or admitted outcome terminal
~~~

<!-- [VXG RealForever][VEX-FEATURE-TEMPLATE] -->
