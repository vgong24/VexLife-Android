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
      -> durable Android MediaStore content URI when device publication succeeded
      -> legacy app-private copy only where current migration/compatibility truth still requires it
    paired VexStream desktop
    admitted remote routes
    -> repository/source-resolution/network owners

WHAT IS PLAYBACK DOING?
    vexstream/playback
    -> Play / Pause through Playback State / Control / Act / Effect
    -> Seek through typed mediaId + position request
    -> Media3 playback / seek Connections

HOW SHOULD THIS RENDERED COLLECTION START?
    vexstream/collection
    -> Play All / Shuffle All through typed rendered-collection request
    -> activation-time source resolution + Media3 provider-capability Connection
    -> Shuffle All is a one-shot resolved-order permutation, not persistent shuffle mode

WHAT CAN I DO WITH THE CURRENT QUEUE?
    vexstream/queue
    -> Previous / Next / Shuffle / Clear through Queue State / Control / Act / Effect
    -> Play one current occurrence through typed occurrence request
    -> Reorder one current occurrence by adjacent typed request
    -> Media3 Queue / Queue Play / Queue Reorder Connections

IS REPEAT ACTIVE FOR THIS PLAYBACK CONTEXT?
    vexstream/repeat
    -> Repeat State / Control / Act / Effect
    -> Media3VexStreamRepeatConnection

HOW MUCH OF THIS COLLECTION IS ON THIS PHONE?
    vexstream/offline
    -> current collection offline plan from repository-owned per-track copy truth
    -> exact Download missing request over only requestable current media

WHAT DOES THE LISTENER SEE?
    screens/shared/vexstream
    -> Android Compose/VexCompose presentation
    -> collection-level on-device summary + per-track copy status

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
    verified private staging -> durable Android MediaStore publication
    README.md preserves producer/consumer, storage and security boundaries

vexstream/playback/
    human questions: Play / Pause + where should current media continue from?
    Behavior family for finite Play/Pause meaning
    typed Seek request + current-media/provider-capability Connection

vexstream/queue/
    human questions: finite Queue Controls + play/reorder one current occurrence
    Behavior family for finite Queue Controls
    typed current-occurrence Queue Play + adjacent Reorder requests

vexstream/track/
    human questions: play this rendered track context / place this rendered track next or at queue end
    typed Track requests + source-resolution / Media3 provider-capability Connection
    composition boundary only; not another finite Behavior family

vexstream/collection/
    human question: play this rendered collection naturally or as one shuffled start order
    typed Collection playback request + source-resolution / Media3 provider-capability Connection
    composition boundary only; collection membership remains upstream catalog/playlist truth

vexstream/offline/
    human questions:
      who owns a requested local copy after the initiating row/sheet disappears?
      how much of this rendered collection is already on the phone?
      which exact missing media may be requested now without re-downloading current copies?
    VexStreamOfflineDocs is the code-local semantic doorway
    single-track Download / Remove + one-shot collection Download missing requests
    in-process VexLife session-lifetime Connection
    suspend repository remains byte/state authority; no process-death/background-resume claim

vexstream/repeat/
    human question: Is Repeat active for the current playback context?
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

## MassDownload / collection offline download — current slice

"MassDownload" is the current project/lane search name. Product/source language stays centered on the
human question: **download the missing part of this collection**.

Current admitted shape:

~~~text
rendered album / artist collection / playlist
-> repository-owned per-track local-copy truth
-> VexStreamOfflineCollectionPlan
   on device
   downloading
   requestable missing
   blocked missing
-> Download missing
-> exact targets carry trackRef + media SHA-256 + byte length
-> VexStreamOfflineCopyConnection cheaply skips already-invalid current targets
-> repository downloadForOffline(...) re-checks exact media identity at serialized admission
-> sequential copy attempt
-> exact bytes cross private integrity staging
-> durable Android MediaStore publication + re-verification
-> repository observation changes local-copy truth to the verified content:// ref
-> collection plan/projection derives again
~~~

This is deliberately one-shot. It does not yet mean "keep this collection synchronized forever."

Current durable-device-storage / rediscovery distinction:

```text
DOWNLOAD COMPLETE
= exact bytes verified
+ durable MediaStore object published/re-opened/verified
+ repository local-copy truth updated

CURRENT-CATALOG REDISCOVERY
= current catalog mediaSha256 + byteLength
+ exact deterministic MediaStore row
+ owned path/name/MIME contract
+ non-pending row
+ re-opened exact digest/length verification
-> current trackRef receives the verified content:// local copy

CURRENT-CATALOG REDISCOVERY
!= synthetic trackRef recovery without a catalog
!= migration of pre-existing private copies
!= destructive Clear Data acceptance
```

API 24-28 requests the bounded legacy shared-storage permission only when a Download/Download missing
request actually needs it. Denial starts no repository download. API 29+ uses scoped MediaStore
publication without broad shared-storage permission.

Held until independently earned:

~~~text
persistent Keep available offline intention
durable collection identity for album-like groupings
background / process-death resumption
whole-library download policy
storage/free-space admission beyond the current per-object publication failure boundary
bounded parallel transfer policy
multi-select / mass removal
~~~

Do not call the one-shot relationship "synced" merely because all current members happen to be local.
A later persistent sync feature would own desired membership plus convergence against changing
collection truth.

QuantumTest remains downstream proof optimization. Once this ordinary feature is proven, a partially
downloaded collection becomes a useful reconstructable branch checkpoint; the Quantum experiment
must consume the feature's truth rather than becoming a second MassDownload implementation.

## Playback and Queue are siblings, not one giant player State

Playback asks:

> What is the current media being asked to do?

Queue currently asks:

> What can the listener truthfully do with the current ordered media context?

Current admitted answers are Previous, Next, Shuffle, and Clear. Clear requests the provider mutation
but does not claim Empty until Media3 observation reports that truth.

They are near each other in the UI but remain independent Behavior families. Do not cross-multiply
their States simply because Mini Player displays both.

## Current Queue boundary

Previous / Next are shared semantic Controls consumed by Mini Player and Context Dock.
Shuffle is the same Queue family question and is consumed as a one-shot action by Mini Player and
Queue presentation; it has no persistent selected state.
Clear is consumed by Queue presentation through the same current-truth Connection.

Queue-row Play remains outside the finite-Control Behavior family but now has a feature-local typed
request boundary:

~~~text
current queue snapshot
-> VexStreamQueueOccurrence(mediaId, occurrence, index)
-> VexStreamQueuePlayRequest
-> provider queue + command re-check
-> seek selected occurrence when needed
-> play
~~~

The occurrence coordinate is current provider identity, not a durable queue-item ID.

Reorder reuses that currentness coordinate one adjacent move at a time:

~~~text
latest observed VexStreamQueueOccurrence
+ Earlier / Later
-> VexStreamQueueReorderRequest
-> provider queue + CHANGE_MEDIA_ITEMS re-check
-> reject stale coordinate
-> reject adjacent identical-media crossing
-> move one adjacent item
~~~

After one move is admitted, the drag carries that same occurrence forward at the expected adjacent
index. The Reorder Connection re-checks provider truth before every later step, so a second move is
rejected until the provider actually observes that occurrence at the expected index. Crossing
adjacent identical media IDs still fails closed because the provider projection cannot distinguish
which duplicate moved without a stronger durable item identity.

Repeat remains its independent sibling Behavior family. The accepted Behavior foundation does not
carry arbitrary runtime request payloads through finite Controls, so Queue Play/Reorder remain
feature-local request boundaries rather than a generic payload framework.

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

Current VexStream extraction debt markers are closed by the accepted Track, Offline Copy, and
Collection playback boundaries. Add another marker only when a new live source seam is intentionally
held for a concrete ownership reason.

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
