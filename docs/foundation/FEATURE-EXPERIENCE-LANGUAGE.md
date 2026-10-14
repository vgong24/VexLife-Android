# Feature Experience Language — fresh feature template

[VXG RealForever]

## Why this exists

A fresh feature should begin with ordinary human meaning before package names, state machines,
providers, repositories, or framework abstractions harden around it.

The purpose of this template is to make the feature explainable as a sentence, then use the sentence
to decide which concepts deserve durable source names.

This is a **question lens**, not a required class hierarchy. If a question has no distinct answer,
do not invent a class merely to fill the template.

## The experience sentence

Start with:

> A **[WHO]** comes to **[HOME]** because **[WHY]**.  
> **[WHAT]** tells them what domain things are known.  
> **[WHERE]** tells the experience where those things can come from, go to, or be realized.  
> **[HOW]** defines how the important behavior is structured.  
> **[NOW / WHEN]** explains what is true and possible now.  
> **[WHAT CAN I DO?]** names what may be requested.  
> **[WHO / WHAT REALIZES IT?]** names the provider or rightful runtime owner.  
> **[WHAT DO THEY SEE?]** names the projection/result returned to the experience.

Write this in product language first. Do not substitute implementation names simply because they
already exist.

## Question map

| Question | Meaning | Typical source role |
|---|---|---|
| **HOME** | Where does this vocabulary belong? | feature/system/experience root |
| **WHO** | Who is living or initiating the experience? | human actor, system actor, consumer |
| **WHY** | Why do these pieces belong together? | experience intention/outcome |
| **WHAT** | What domain things are known? | domain model, library/catalog, repository projection |
| **WHERE** | Where can the thing come from/go/be realized? | sources, destinations, resolvers |
| **HOW** | How is an important behavior structurally understood? | architecture |
| **NOW / WHEN** | What is true now and under what conditions is something lawful? | Case + Placement |
| **WHAT CAN I DO?** | What semantic request can be made? | Control |
| **WHO / WHAT REALIZES IT?** | What runtime owner can actually perform the effect? | Provider / service / Relay / repository / SDK adapter |
| **WHAT DO THEY SEE?** | What read-only result is presented back? | Projection / UI / notification / returned result |

The table does **not** imply one class per row.

### Package placement follows human feature language

Use package/folder names to answer the human feature question first:

~~~text
feature/
├── library/
├── sources/
├── playback/
└── queue/
~~~

Then use filenames inside the human area to express code roles:

~~~text
playback/
├── <PlaybackContract>
├── <PlaybackProvider>
├── <PlaybackCase>
├── <PlaybackControl>
└── <PlaybackStatus>
~~~

This lets a newcomer ask **"what part of the feature is this?"** before they need to know
**"what implementation role am I searching for?"**

Prefer human feature-area packages over top-level implementation buckets such as architecture/,
provider/, model/, or adapter/ when those buckets would hide the product language.

## Category-drift test

Before adding a new top-level noun, ask:

1. **Which human question does this noun answer?**
2. **Does another existing noun already answer that question?**
3. **Is this a durable concept, or only a derived fact?**
4. **Is this product meaning, or technical machinery?**
5. **Would removing the noun lose correctness, continuity, authority, or useful navigation?**

Warning signs:

~~~text
two peer nouns answer the same question
derived facts become top-level architectures
providers appear beside human-facing product concepts
an SDK name becomes semantic vocabulary
a "Manager", "Coordinator", "Path", or "Workflow" appears before repeated integration pressure exists
every question is forced to have a class
~~~

If the sentence becomes harder to explain after adding the noun, investigate before preserving it.

## Experience versus Architecture

An **Experience** is the broad human/product container explaining why several different owners belong
together.

An **Architecture** gives reusable structural meaning to one important behavior inside that
experience.

~~~text
Experience
    integrates/orients

Architecture
    explains structure

Provider
    realizes behavior
~~~

Do not make Experience a God object. It may expose the pieces that cooperate, but it should not
reimplement repositories, transports, SDKs, persistence, platform services, or provider behavior.

Do not use Architecture as another word for "folder".

## Source versus Provider

A **Source** answers where domain/media/input can come from.

A **Provider** answers who/what can realize a runtime capability.

One SDK may technically do both. That does not make the meanings identical.

Example:

~~~text
Spotify source
    can tell the experience which media is available

Spotify player provider
    can realize playback through Spotify's SDK
~~~

Use separate contracts when the semantic responsibilities differ, even if one implementation object
happens to satisfy both.

## Current truth versus availability / authority

Keep these questions separate:

~~~text
KNOWN
    the domain thing exists in the experience

SOURCE_AVAILABLE
    some source can currently supply it

RUNTIME_AVAILABLE
    the provider/runtime can currently realize it

AUTHORIZED
    the actor is allowed to request it

CASE
    what semantic truth currently applies

PLACEMENT
    what relationships are lawful from that Case
~~~

Do not collapse them into one boolean merely because one screen needs an enabled state.

## Naming interfaces

Prefer role-bearing names:

~~~text
VexMediaPlaybackProvider
VexMediaPlaybackContract
~~~

over interface-marker names:

~~~text
IVexMediaPlaybackProvider
IPlaybackContract
~~~

The language construct is visible in Kotlin/IDE tooling. The name should explain **why the interface
exists**.

## Fresh-feature authoring template

### 1. Epic / experience intention

~~~text
HOME:
WHO:
WHY:

EXPERIENCE SENTENCE:
A [WHO] comes to [HOME] because [WHY].
~~~

### 2. Human question map

~~~text
WHAT is known?
WHERE can it come from / go?
HOW is the important behavior structured?
NOW / WHEN: what is true and possible now?
WHAT CAN I DO?
WHO / WHAT actually realizes the effect?
WHAT result/projection comes back?
~~~

Leave an answer blank when the feature genuinely does not need a distinct concept.

### 3. Proposed source map

Only after the questions are understood:

~~~text
<Experience>
├── <human/product peer>
├── <source/destination peer>
└── <Architecture>
    ├── <Capability>
    └── <Capability>

<Provider contracts>
└── <SDK/native adapter>
~~~

This is an example shape, not a required skeleton.

### 4. Acceptance-language walk

For each important human request:

~~~text
GIVEN
    <current Case / source facts / authorization facts>

WHEN
    <human request / Control>

THEN
    <selected semantic relationship>

REALIZED BY
    <rightful Provider / Relay / repository / service / SDK owner>

OBSERVED AS
    <new Case / source truth / result>

EXPERIENCED AS
    <projection / visible outcome>

PROVED BY
    <unit / integration / build / device / lived proof appropriate to the claim>
~~~

This makes the acceptance criterion traceable both from the human experience downward and from the
runtime result upward.

## VexStream example

### Epic sentence

> A **listener** comes to **VexStream** because they want to experience the music they know wherever
> it can be realized. The **Library** tells them what music is known. **Sources** tell VexStream where
> playable media can come from. The **Player Architecture** defines how listening is structured.
> **Playback and Queue Cases/Placements** explain what is true and possible now. **Controls** name
> what may be requested. **Player Providers** realize those meanings through Media3 or future SDK
> adapters. The observed **projection** returns the result to the listener.

### Question map

~~~text
HOME
    VexStream

WHO
    listener / consumer

WHY
    experience the listener's music

WHAT
    Library

WHERE
    Sources

HOW
    VexStreamPlayerArchitecture

NOW / WHEN
    Playback / Queue Case + Placement

WHAT CAN I DO?
    Controls such as PlayPause / Next / Previous / PlaySelected

WHO / WHAT REALIZES IT?
    VexStreamPlayerProvider
    -> Media3VexStreamPlayerProvider today

WHAT DO THEY SEE?
    Now-playing / queue / screen / context-dock projections
~~~

This sentence is the reason the current V2 model does **not** promote Connection, Availability,
Provider, and a speculative Path abstraction as peer experience nodes.

## Review test

A fresh reviewer should be able to explain the feature in ordinary language without reading its
implementation history.

If they must ask:

~~~text
Why are these two peer nodes different?
Why is this an Architecture rather than a capability?
Is this Source the same thing as Provider?
Why does this derived fact have its own owner?
Which class do I start from?
~~~

treat those questions as architectural feedback, not reviewer failure.

<!-- [VXG RealForever][FEATURE-EXPERIENCE-LANGUAGE][VEX-ARCHITECTURE] -->
