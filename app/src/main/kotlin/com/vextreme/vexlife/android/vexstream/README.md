# VexStream — Android feature home

[VXG RealForever]

## Human outcome

A listener comes to VexStream in VexLife to browse music known by VexStream, connect that music to a
playable source, listen through the phone, understand what is playing/coming next, and keep verified
media locally where the admitted product flow supports it.

This file is the **whole-feature orientation home**. It is not a runtime owner and there is no broad
`VexStreamExperience` object merely to mirror this document.

Repository feature-authoring context:

- `docs/features/README.md`
- `docs/foundation/FEATURE-EXPERIENCE-LANGUAGE.md`

## Human question map

~~~text
WHAT MUSIC IS KNOWN?
    data/vexstream
    -> VexStreamMusicRepository + track/collection state

WHERE CAN MEDIA COME FROM?
    verified local copy
    paired VexStream desktop
    admitted remote routes
    -> repository/source-resolution/network owners

WHAT IS PLAYBACK DOING?
    vexstream/playback
    -> Playback State / Control / Act / Effect
    -> Media3VexStreamPlaybackConnection

WHAT CAN I DO WITH THE CURRENT QUEUE?
    vexstream/queue
    -> Previous / Next / Shuffle / Clear through Queue State / Control / Act / Effect
    -> Media3VexStreamQueueConnection

WHAT DOES THE LISTENER SEE?
    screens/shared/vexstream
    -> Android Compose/VexCompose presentation

WHO OWNS PLAYER LIFETIME?
    media/vexstream/VexStreamPlaybackService
    -> MediaSession / ExoPlayer lifetime

WHAT PROVES THE HUMAN WALK?
    journeyTest/.../VexStreamJourneys.kt
~~~

## Current source map

~~~text
data/vexstream/
    catalog/library/source/offline/pairing truth and repositories
    README.md preserves producer/consumer and security boundaries

vexstream/playback/
    human question: Play / Pause from current playback truth
    Behavior family + concrete Media3 Connection + UI Experience membrane

vexstream/queue/
    human question: Previous / Next / Shuffle / Clear from current queue truth
    Behavior family + concrete Media3 Connection + UI Experience membrane

media/vexstream/
    Android MediaSession/player/network realization

screens/shared/vexstream/
    Android presentation
    connection / track / collection / player / queue surfaces

journeyTest/.../vexstream/
    integrated human interaction proof
~~~

The folders are not required to be symmetrical. A source area exists when its human/product question
and owner are real.

## Playback and Queue are siblings, not one giant player State

Playback asks:

> What is the current media being asked to do?

Queue currently asks:

> What can the listener truthfully do with the current ordered media context?

Current admitted answers are Previous, Next, and Clear. Clear requests the provider mutation but does
not claim Empty until Media3 observation reports that truth.

They are near each other in the UI but remain independent Behavior families. Do not cross-multiply
their States simply because Mini Player displays both.

## Current Queue boundary

Previous / Next are shared semantic Controls consumed by Mini Player and Context Dock.
Shuffle is the same Queue family question and is consumed as a one-shot action by Mini Player and
Queue presentation; it has no persistent selected state.
Clear is consumed by Queue presentation through the same current-truth Connection.

Still deliberately outside the Queue Behavior family:

~~~text
queue-row selection
    carries an index request payload

reorder
    carries dynamic queue-item identity

repeat
    separate semantic question not absorbed merely because it is Queue-adjacent
~~~

The accepted Behavior foundation still holds a generic runtime-request-payload abstraction. Do not
invent one just to migrate queue-row selection.

## Dynamic Android presentation identity

`screens/shared/vexstream/VexStreamPresentationRefs.kt` is private Android
presentation/testing machinery for dynamic rendered items.

It is **not** the VexStream feature entry, Queue truth, Track domain identity, provider state, or a
generic Vex architecture.

A developer beginning from Queue should first understand Queue and only descend into that helper when
working on the exact rendered-item / Journey / interaction-subject addressing problem.

If the helper grows because unrelated product areas merely share String-building mechanics, keep the
technical-junk-drawer smell visible and prefer established terms/feature-local placement when a real
change creates a natural opportunity.

## Naming / source-formation law

Before adding another durable VexStream noun:

~~~text
human situation
-> current truth / owner
-> missing question
-> relationships + boundary
-> established development vocabulary
-> existing rightful home
-> name
-> minimum source
-> proof
~~~

Do not promote an implementation limitation into product vocabulary.

## Coverage and architecture debt

Coverage answers **what executable source the selected tests exercised**. It does not decide whether a
large live class should receive more tests or be decomposed first.

VexStream therefore allows a source-owned coverage-debt marker:

~~~text
// VEX-COVERAGE-DEBT: LIVE_REFACTOR_DEBT | <why this live source remains intentionally broad>
~~~

The marker never removes live code from the coverage denominator. It adds context to the coverage
projection so a low percentage can say "known extraction debt" instead of implying "add tests around
the current shape forever."

Current marked VexStream seams are intentionally specific:

~~~text
VexStreamLibrarySurface
    library/search list rendering has been extracted
    remaining composition still routes raw controller/source-resolver concerns for held actions

VexStreamPlayerPresentation
    Seek still carries a runtime position payload through raw MediaController

VexStreamQueuePresentation
    Queue play/reorder carry runtime occurrence/index identity
    Repeat ownership remains unresolved

VexStreamTrackPresentation
    track play/queue/download actions still combine runtime identity with provider/repository effects
~~~

A marker is removed when its described debt is actually resolved. A source file is not called
deprecated/replaced merely because a successor design exists; production reachability remains the
deciding fact.

## Current proof shape

Use focused JVM tests for Behavior meaning, repository-owned aggregate verification for source
integration, connected-device/Journey proof for Media3/Android interaction, and lived review for
product feel.

Current canonical Journey:

`app/src/journeyTest/kotlin/com/vextreme/vexlife/android/testing/journey/catalog/shared/vexstream/VexStreamJourneys.kt`

## Historical architecture quarry

Draft PR #80 and PR #81 explored earlier VexArchitecture / Case / Placement / generic Media Player
contracts.

Their useful human-first lessons are consumed here and in the repository feature-authoring docs.
Their separate Case/Placement/Transition kernel is not current authority because accepted
`architecture:behavior` now owns State / Control / Act / Effect semantics with fewer overlapping
framework nouns.

Do not resurrect the old V2 monolithic `VexStreamLibrarySurface.kt` while borrowing those lessons.
Current main intentionally decomposes connection, track, collection, player, queue, and search
presentation into their current owners.

<!-- [VXG RealForever][VEXSTREAM][FEATURE-HOME] -->
