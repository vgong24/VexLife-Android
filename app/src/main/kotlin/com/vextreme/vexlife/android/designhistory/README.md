# VexLife DesignHistory — Feature Home

[VXG RealForever]

## Feature identity

~~~text
featureRef = feature.vexlife.design-history
currentName = VexLife DesignHistory
code navigation = VexLifeDesignHistoryDocs
~~~

## Human question

> How can VexLife preserve the evolution of feature intent so a future human or AI can recover what
> we are building, why it took this shape, how it changed, and what is current — without making one
> centralized document own every feature's design?

## Why this exists

Git/PR history proves implementation changes. Feature READMEs explain current source truth. Neither
alone preserves the full design trajectory that often happens in conversations before or between
implementation milestones.

Without a durable design-continuity pattern, a successor may have to reconstruct intent from:

~~~text
old chats
PR comments
commit diffs
stale issue prose
memory of prior workers
~~~

DesignHistory exists to make that reconstruction unnecessary when the design pressure is material.

## Ownership boundary

DesignHistory does **not** own feature semantics.

~~~text
docs/features/*
    repository-wide feature-authoring contract

VexLife DesignHistory
    continuity grammar / worked meta-feature

feature package
    owns its own current design + records
~~~

Therefore Conversation still owns Conversation design, VexStream owns VexStream design, VexMaps owns
VexMaps design, and Debug Menu owns its developer-surface design.

DesignHistory must not become:

~~~text
a global design database
a central source of every feature's current truth
a runtime feature registry
a replacement for Git history
a replacement for feature README/source authority
~~~

## Current source map

~~~text
designhistory/
├── README.md
├── VexLifeDesignHistoryDocs.kt
└── design/
    ├── README.md
    └── records/
        ├── 2026-10-15-001-fractal-feature-design-continuity.md
        └── 2026-10-16-002-name-ontology-as-design-evidence.md
~~~

Repository-wide authoring rules remain in:

~~~text
docs/features/README.md
docs/features/VEX-FEATURE-TEMPLATE.md
~~~

## Current pattern

When continuity pressure earns it:

~~~text
<feature>/
├── README.md
│   current implemented/source truth
├── Vex<Feature>Docs.kt
│   stable feature identity + navigation
└── design/
    ├── README.md
    │   current intended design
    └── records/
        ├── YYYY-MM-DD-NNN-meaningful-refinement.md
        └── ...
~~~

One record captures one meaningful design refinement, not one chat turn.

## Stable identity

Names and package paths may change. The stable feature ref should not need to.

~~~text
featureRef
    durable semantic identity

currentName
    current human/source name

historicalSearchNames
    former names/search aliases
~~~

DesignHistory does not currently define one shared runtime identity type. The feature-local Docs
anchor is sufficient for the first real consumers. A shared abstraction must be earned by repeated
machine-level pressure.

## Current consumers

### Conversation

~~~text
featureRef = feature.vexlife.conversation
currentName = Conversation
historical/search name = VexConversation
~~~

Conversation owns:

~~~text
conversation/README.md
conversation/design/README.md
conversation/design/records/*
~~~

### Debug Menu

Debug Menu is not currently promoted as a peer product feature identity. It demonstrates that a
shared developer surface can preserve its own design while linking to feature-owned designs rather
than duplicating them.

### Name Ontology transfer

DesignHistory now has a second reusable continuity lesson:

~~~text
cold-read durable name
-> predict question / owner / consequence radius
-> compare against source
-> preserve material prediction gap
-> rename only when the gap matters
-> keep historical term searchable
~~~

The originating VexMaps Distribution record currently lives on active draft PR #86.

Conversation independently transferred the method into accepted main through:
~~~text
conversation/design/records/2026-10-16-003-name-ontology-hybrid-relay.md
VexConversationHybridRelay
~~~

The tag `[Name Ontology]` names the continuity method. It is not a central approved-word registry.

## Proof map

Current DesignHistory claims are primarily source-navigation and continuity claims.

Cheapest truthful proof:

~~~text
source inspection
    package roles are distinct

compile/JVM
    code identity anchors remain valid Kotlin

fresh-worker navigation
    old name -> stable ref -> current design -> dated record

rename pressure
    current name can change without changing featureRef or losing old search names
~~~

DesignHistory does not claim a user-visible runtime feature yet.

## Current next edge

The first POC is complete, the visible Conversation Lab/Hybrid work has landed, and Name Ontology has
now transferred across independent feature pressure.

Do not add another DesignHistory mechanism by symmetry. Continue only when another real continuity
failure or repeated consumer pressure exposes a missing grammar, recovery path, or source-navigation
need.

<!-- [VXG RealForever][feature.vexlife.design-history][FEATURE-HOME] -->
