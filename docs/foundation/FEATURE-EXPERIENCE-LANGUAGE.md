# Feature Experience Language — human meaning before source taxonomy

[VXG RealForever]

## Why this exists

A fresh feature or major feature evolution should begin with the human/product situation before package
names, state machines, providers, repositories, or framework abstractions harden around it.

This is a **question lens**, not a class hierarchy. A question with no distinct truthful answer does
not earn a class, interface, file, package, registry, or new Vex noun.

## Experience is an orientation lens

Here, **feature experience** means the broad human/product reason several rightful owners cooperate.

It does **not** require a runtime object named `<Name>Experience`.

Current accepted Behavior also uses a feature-local `<Name>Experience` name for a much narrower
Compose/UI membrane around one Behavior family. That source role remains valid and precise.

Keep the two levels distinct:

~~~text
feature experience
    human/product orientation
    normally preserved in the feature home + truthful product nouns

<Name>Experience in Behavior source
    optional UI membrane
    Projection / DisplayState + activate(Control)
~~~

Do not create a broad root object merely so the documentation diagram has something to point at.

## Experience sentence

Start with ordinary language:

> A **[WHO]** comes to **[HOME]** because **[WHY]**.  
> **[WHAT]** tells them what domain things are known.  
> **[WHERE]** tells them where those things can come from, go, or be realized.  
> **[HOW]** identifies the important behavioral questions/capabilities.  
> **[NOW / WHEN]** says what is actually true and under what conditions an action is lawful.  
> **[WHAT CAN I DO?]** names stable human-facing Controls or requests.  
> **[WHAT DOES THAT MEAN NOW?]** names the Act selected from current truth.  
> **[WHAT SHOULD RUNTIME ATTEMPT?]** names the Effect or concrete requested transformation.  
> **[WHO / WHAT REALIZES IT?]** names the rightful Connection/provider/service/repository/SDK owner.  
> **[WHAT DO THEY SEE?]** names the Projection/result returned to the human experience.

Write the sentence before deciding which answers deserve durable source.

## Question map

| Question | Meaning | Typical source role when actually earned |
|---|---|---|
| **HOME** | Where does this vocabulary belong? | feature/system home |
| **WHO** | Who is living or initiating the experience? | human/system actor |
| **WHY** | Why do these pieces belong together? | feature intention/outcome |
| **WHAT** | What domain things are known? | model/library/catalog/repository |
| **WHERE** | Where can the thing come from/go/be realized? | source/destination/resolver |
| **HOW** | Which important behavioral question am I entering? | capability/domain/Behavior family; Architecture only when structural meaning is truly reusable |
| **NOW / WHEN** | What is actually true now? | State + conditions/source/authority facts |
| **WHAT CAN I DO?** | Which stable request/affordance exists? | Control or ordinary request |
| **WHAT DOES THAT MEAN NOW?** | What semantic verb does the request mean from current truth? | Act |
| **WHAT SHOULD RUNTIME ATTEMPT?** | What runtime transformation is selected? | Effect |
| **WHO / WHAT REALIZES IT?** | What owner can perform/observe the effect? | Connection/provider/service/repository/SDK adapter |
| **WHAT DO THEY SEE?** | What read-only result reaches the view/human? | Projection / optional DisplayState / UI |

The table does not imply one source type per row.

## Formation rule for human and AI workers

Before creating a durable class/file/interface/concept:

~~~text
1. HUMAN SITUATION
   What is actually happening in ordinary language?

2. CURRENT TRUTH
   What already exists and who owns it?

3. PRESSURE
   What real situation cannot currently be represented, distinguished, requested, or observed?

4. MISSING QUESTION
   What question does the system currently lack an answer for?

5. RELATIONSHIPS
   Which existing concepts participate?

6. BOUNDARY
   What must the new thing NOT own or imply?

7. ESTABLISHED LANGUAGE
   What do competent Android/Kotlin/UI/domain developers already call this concept?

8. EXISTING HOME
   Can a rightful existing owner answer it with a smaller addition?

9. ORDINARY DESCRIPTION
   Describe the needed thing without using the proposed new noun.

10. NAME
    Choose a name/path that lets a fresh developer recover approximately that description.

11. SOURCE SHAPE
    Only now decide whether this deserves a class, value type, helper, property, interface, or no
    durable source at all.

12. PROOF
    Prove the original missing question is now answered.
~~~

Compact form:

~~~text
QUESTION
-> MEANING
-> RELATIONSHIP
-> ESTABLISHED PATTERN
-> NAME
-> CODE
~~~

not:

~~~text
CODE SHAPE
-> NEW NOUN
-> COMMENT EXPLAINING THE NOUN
~~~

## Source-grounded explanation

The formation rule above also governs how semantic source is explained.

Do not read a reducer, State machine, or focused test backward and turn a plausible interpretation
into feature purpose. Ground source-local explanation through current authority:

~~~text
accepted feature-purpose source
-> exact human/product question
-> whole-human Walk / Journey / lived contract when one exists
   OR explicit current absence / hold
-> local semantic owner
-> focused proof of the narrower claim
-> source-local KDoc / comments
~~~

Preserve these distinctions:

~~~text
FEATURE PURPOSE != IMPLEMENTATION INFERENCE
WHOLE HUMAN WALK != FOCUSED TEST EXAMPLE
FOCUSED TEST != PRODUCT ACCEPTANCE
CURRENT SOURCE != HISTORICAL DESIGN RECORD
~~~

If the whole human path is absent, keep that absence visible. Do not synthesize one from unit-test
fixtures merely because the local implementation is understandable.

A useful semantic comment should deepen the existing doorway by answering only what the code cannot
make obvious on its own:

~~~text
why this owner exists
which accepted feature question it serves
which independently-owned truths participate
what relationship / decision it owns
what neighboring authority or completion claim it does not own
~~~

Comments that merely translate syntax add little value:

~~~text
"calculate availability from visibility"
"derive output from current state"
"this field affects candidate selection"
~~~

The feature home / practicum remains the purpose owner; Journey / Feature Walk owns the human path
when present; focused tests own narrower proof; comments summarize that accepted context locally.

## Established language before Vex language

Do not invent architecture vocabulary for a problem software engineering already names clearly.

Before minting a Vex-specific noun, consider whether an ordinary term already communicates the role:

~~~text
ID
key
item
state
metadata
route
adapter
provider
connection
test tag
semantic element
repository
service
~~~

Vex-specific language is earned when it preserves a genuinely useful semantic distinction, not when
the implementation happens to need another String, wrapper, registry, helper, or file.

~~~text
SAME REPRESENTATION TYPE
!= SAME SEMANTIC ROLE
!= SAME OWNER
~~~

A domain TrackRef, Compose item key, queue-item key, semantic element ID, and interaction subject may
all be String-like while answering different questions.

## Name-before-context test

Evaluate an architectural/source-navigation noun **before** reading the implementation that explains
it.

Ask:

> If a fresh developer is told only "go inspect <Name/path>", can they form a useful hypothesis about
> the conceptual neighborhood, owner, and likely consequence radius?

Then read the implementation.

A large prediction/reality delta is architectural feedback. Do not erase it merely because KDoc made
the helper understandable afterward.

Context may refine a name. Context should not be required to discover what the name basically means.
Documentation deepens a good doorway; it should not routinely rescue an opaque one.

## Package placement follows human meaning

Prefer a path that lets the developer locate the human/product area first:

~~~text
feature/
├── library/
├── playback/
└── queue/
~~~

Then express technical roles inside the area:

~~~text
playback/
├── ...State
├── ...Control
├── ...Act
├── ...Effect
├── ...Behavior
└── Media3...Connection
~~~

Do not force that exact tree onto every feature. The rule is:

> **human neighborhood first; implementation role second**

A technical helper can remain technical. It simply should not become a parallel feature map.

## Watch for technical junk drawers

A helper that grows because many values share one code shape deserves scrutiny.

Warning shape:

~~~text
GenericTechnicalBucket
    track row IDs
    track action IDs
    search list IDs
    queue list IDs
    queue item keys
    queue action subjects
~~~

The grouping rule may have become "things implemented as dynamic ID strings" rather than one human
meaning.

Do not split files merely for symmetry, and do not start a rename campaign. When real work crosses the
boundary, ask whether the machinery would be easier to find beside Track, Search, Queue, or another
rightful owner.

## Source, provider, Connection, and platform

Keep these distinct:

~~~text
domain/source owner
    knows/stores domain truth

Behavior
    describes lawful semantic relationships

Connection/provider/service/repository adapter
    observes runtime truth and realizes Effects

Android/SDK/native owner
    owns platform mechanics and lifetime
~~~

One object may implement several roles. That does not make the roles semantically identical.

## VexStream worked example

VexStream currently teaches the questions without requiring a broad `VexStreamExperience` runtime
object.

~~~text
HOME
    VexStream

WHO / WHY
    listener wants to browse and listen to known music

WHAT
    Library/catalog truth
    -> data/vexstream repository owners

WHERE
    verified local media / paired VexStream desktop / later admitted sources
    -> source resolution + repository/network owners

HOW
    Playback Behavior
    Queue Behavior
    -> independent human questions, not one giant player State

NOW / WHEN
    VexStreamPlaybackState
    VexStreamQueueState

WHAT CAN I DO?
    PlaybackControl.PlayPause
    QueueControl.Previous / Next
    plus other feature actions that have not yet earned Behavior migration

WHAT DOES THAT MEAN / WHAT SHOULD RUNTIME ATTEMPT?
    Act -> Effect inside the owning Behavior family

WHO REALIZES IT?
    concrete Media3 Connections / MediaSessionService for player effects
    repositories/network/storage for catalog/source effects

WHAT DO THEY SEE?
    screens/shared/vexstream Android presentation
~~~

The source of truth for this worked example is the feature home:

`app/src/main/kotlin/com/vextreme/vexlife/android/vexstream/README.md`

## Review test

A fresh reviewer should be able to explain the feature in ordinary language without reading its
implementation history.

If they must ask:

~~~text
what product area is this technical noun actually about?
why did this derived fact get its own owner?
is this another truth or only a projection?
why is this a Vex-specific term instead of an established software term?
which class/file do I start from?
~~~

treat those questions as architecture feedback, not reviewer failure.

<!-- [VXG RealForever][FEATURE-EXPERIENCE-LANGUAGE][SEMANTIC-PREDICTABILITY] -->
