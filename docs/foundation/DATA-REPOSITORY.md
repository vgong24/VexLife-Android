# Data / Repository Foundation

[VXG RealForever]

## The question

> How can a screen observe/persist data without knowing whether it came from Room, memory, a backend,
> or a future synchronization policy?

The first real consumer is Debug -> Log History. The architecture is intentionally learned through
that concrete need rather than by introducing a generic BaseRepository hierarchy.

## Shape

~~~text
screen / runtime consumer
        |
        v
domain-specific repository contract
        |
        +-------------------+
        |                   |
        v                   v
 local Room source     future remote/backend source
        |
        v
SQLite
~~~

For Log History:

~~~text
VexLogger
  forms accepted diagnostic record
        |
        v
LogHistoryPersistenceQueue
  non-blocking projection boundary
        |
        v
LogHistoryRepository
        |
        v
RoomLogHistoryRepository
        |
        v
LogHistoryDao / LogHistoryEntity
~~~

The Debug Log History bottom sheet observes LogHistoryRepository rather than VexLogger's local
Compose history.

## Laws

~~~text
REPOSITORY != DAO
ENTITY != DOMAIN MODEL
PREVIEW_FIXTURE != PRODUCTION_STATE_OWNER
DATABASE_WRITE != USER_ACTION_SUCCESS
DIAGNOSTIC_PERSISTENCE != CANONICAL_PRODUCT_TRUTH
OBSERVED_REPOSITORY_STATE != OPERATION_ATTEMPT
~~~

There is intentionally no BaseRepository<T>. Each repository should answer a real domain question,
because remote/local merge, retry, freshness, permissions, and mutation semantics differ by domain.

## Transaction boundary

Room already wraps one @Insert/@Update/@Delete/@Upsert write as a transaction. Use @Transaction when
multiple database statements together form one invariant.

Current concrete example:

~~~text
insert LogHistory row
+
prune retained LogHistory window
=
one atomic database effect
~~~

LogHistoryDao.insertAndPrune() owns that transaction. Repository code must not split it back into
"insert(); prune()" calls, because app/process interruption between them would reopen a partial-state
window.

Transactions protect database consistency only. They do not make network/device side effects atomic;
future cross-system operations will need their own operation/reconciliation contracts.

## Schema names, serialization names, and obfuscation

These are separate compatibility surfaces:

~~~text
Room:
  @Entity(tableName = ...)
  @ColumnInfo(name = ...)
  migrations

network/protocol:
  DTO field names
  @SerialName / @SerializedName only if that serializer is chosen

R8/obfuscation:
  Kotlin/JVM symbol renaming at build time
~~~

Database entities now declare explicit SQL table/column names. Renaming a Kotlin property is therefore
not silently treated as a database column rename.

Do not add @SerializedName to Room entities to "protect the database"; it is a JSON/Gson contract,
not a Room schema contract. When backend DTOs arrive, keep DTOs separate from Room entities and map
between DTO/domain/entity shapes.

The current app build does not yet enable release minification/obfuscation. Add an explicit R8/minify
proof when that release concern is admitted rather than assuming this database work proved it.

## Log History persistence

Room is the Android-local substrate for this first repository.

Storage keeps:
- an auto-generated database row ID for global storage order;
- a generated sessionRef;
- VexLogRecord.sequence as session-local sequence;
- timestamp / duration / action / screen context / section;
- optional details_json captured only at explicit semantic boundaries such as form Submit.

The database ID is NOT the logger sequence. Logger sequence may restart after Reset or process
recreation while the persisted database has its own row identity.

Two bounds are separate:

~~~text
recentLimit = 256
  what the UI observes at once

retentionLimit = 4096
  how much persisted diagnostic history is retained locally
~~~

The repository prunes old rows after append so SQLite history is not unbounded.

## Async ownership

VexLogger is synchronous because user interactions should not become suspend functions merely for diagnostics.

The persistence queue bridges that into the suspend repository:

~~~text
user action
-> logger commits local diagnostic
-> logger.onRecord
-> bounded queue
-> externally owned CoroutineScope
-> repository.append
~~~

Repository failures are swallowed/counted by the queue and cannot make the product action fail.

The queue deliberately does NOT create an application scope. Production now supplies
VexLifeViewModel.viewModelScope through VexLifeSession, so database/log work survives Activity
configuration recreation. A future VexAppScope may take over longer-lived semantic operation
ownership without changing the repository/logger contract.

## Reset

Debug -> Log History -> Reset now means clear, not "start another visual section."

~~~text
Reset Clicked
-> VexLogger forms the click diagnostic
-> app action ClearLogHistory
-> VexLogger.clearHistory()
-> LogHistoryPersistenceQueue.clear()
-> Room repository clears persisted history
~~~

Clear is an ordered queue command. When the bounded queue is full, older diagnostic append work may
be dropped to preserve app semantics and make room for newer commands; later records remain ordered after Clear.

## Preview and test data

Use domain fixtures rather than Room entities:

~~~kotlin
val typical = LogHistoryFixture.build {
    appOpened(HomeScreen)
    afterSeconds(120)
    click(
        element = AppShell.Elements.DebugMenu,
        from = HomeScreen,
        to = DebugMenuScreen,
    )
}

val repeatedReset = typical.append {
    afterSeconds(30)
    click(
        element = DebugMenuScreen.ResetLog,
        from = DebugMenuScreen,
    )
}

val repository = InMemoryLogHistoryRepository(
    initialRecords = repeatedReset.records,
)
~~~

This is intentionally chain-buildable. A preview or unit test can start from a common scenario,
append one edge case, and immediately observe the UI/result without copying the production
repository or database schema.

Future domains should copy the principle, not necessarily this exact builder API.

## Form submission boundary

User Profile is also the first form proof.

~~~text
TextFieldElement
  stable blueprint/registry identity

draft text
  local presentation state
  no runtime action per keystroke
  no VexLogger record per keystroke

Save
  one semantic interaction
  one immutable FormSubmission snapshot
  one AppActionRequest

repository
  persists the submitted snapshot
~~~

Draft field changes are intentionally absent from VexLogger. At Save/Submit, however, the immutable
FormSubmission snapshot is serialized once as JSON detail using stable element refs as keys. This
preserves what was actually submitted without recording typing order.

A form screen may know how to assemble its draft values into FormSubmission. It must not perform the
repository mutation secretly after calling runtime.activate.

The declared Save action is delivered through AppActionRegistrations to one feature-local
AppActionRegistration kept beside UserProfileScreen. AppScreens discovers each registered
AppScreenBlueprint's local registrations; VexLifeSession supplies broader-lifetime construction
dependencies without owning a feature-action list. AppActionRegistrations rejects duplicate ActionKey
ownership and missing declared Invoke coverage so screens cannot silently collide or remain unwired.

## User Profile proof domain

The second repository intentionally stays small:

~~~text
UserProfile(
  profileRef,
  displayName,
  updatedAt
)

UserProfileRepository:
  observe(profileRef)
  upsert(profile)
  updateDisplayName(...)
~~~

A single upsert/update is already transactional at the Room write boundary. This domain proves
insert/update behavior and reusable fixtures without inventing VexHome pairing storage prematurely.

The current test UI is reachable from the shared FAB menu and shows:
- the persisted display name observed from UserProfileRepository;
- an editable draft field;
- Save, which submits once through VexRuntime and then upserts through the repository.

That visible round trip is the first lived proof target for User Profile.

Schema evolution now has:
- v1 -> v2: preserve Log History while moving physical SQL columns to explicit snake_case names and
  add user_profile;
- v2 -> v3: add nullable details_json for structured submit diagnostics;
- v3 -> v4: add bounded vex_maps_state.

Existing rows remain valid with null details. Schema changes are migrations, not formatting.

### Current committed Room schema baseline

Room export is repository source evidence, not disposable build output. The first host-wide Test
Foundation proof generated the exact v4 schema from the accepted current entities, so
`app/schemas/com.vextreme.vexlife.android.data.VexLifeDatabase/4.json` is retained as the current
schema baseline.

Historical v1-v3 schema JSON was not previously retained. Do not reconstruct those files from the
current model and call that migration proof. The existing handwritten migrations remain source, but
true historical migration integration proof stays pending until authentic predecessor schema
evidence or an equivalent accepted fixture is available.

## Simplicity bridges without repository sprawl

Repositories are intentionally isolated, but callers should not need to manually thread every low-level
endpoint through feature code.

Use narrow purpose bridges:

~~~text
AppRepositories
  composition-only repository bundle
  may pass through app routing/composition code as one stable dependency
  leaf Screen/content still receives only the repository/facade it actually needs

AppActionRegistrations
  one immutable queryable registration set
  duplicate-safe feature-local action ownership + ActionKey/Element lookup

VexLogger
  receives one record endpoint (logPersistence::record)

future UserHub
  may intentionally combine UserProfileRepository + LinkedSystemRepository

screen
  receives the smallest repository/facade that answers its question
~~~

VexLifeScaffold may receive AppRepositories because it is the app routing/composition boundary.
That avoids threading `logHistoryRepository`, `userProfileRepository`, and every future repository
through intermediate signatures just so the final routing branch can select one.

The selection still happens at the narrowest useful point:

~~~text
VexLifeScaffold(repositories)
  -> DebugMenuScreenContent(repositories.logHistory)
  -> UserProfileScreenContent(repositories.userProfile)
~~~

Do not pass the entire AppRepositories bundle into VexLogger or ordinary screens merely for
convenience. That would turn composition convenience into a service locator and make unrelated data
reachable from unrelated code.

When several repositories form one real domain relationship, prefer a named facade/owner such as a
future UserHub over making every caller know the underlying repository set.

A bridge is successful when it hides plumbing while preserving dependency meaning.

## VexMaps local state

Maps is another concrete repository consumer, but it does not introduce a generic preferences layer.

~~~text
VexMaps screen
  -> VexMapsRepository
       -> last accepted precise fix
       -> selected online style
  -> RoomVexMapsRepository
  -> one vex_maps_state row
~~~

The row is a convenience checkpoint, not a location ledger. Fresh location fixes replace the prior
coordinate; movement samples and navigation updates are not appended to Room. Reopening Maps may use
the last accepted fix immediately, then refresh the real current fix asynchronously when precise
location permission is already granted.

Map rendering, navigation-session state, pack custody, VexStream playback, and future Personal World
memory remain separate owners. A second consumer should reuse a rightful owner rather than moving
unrelated meaning into VexMapsRepository.

## Backend evolution

A future backend does not automatically mean "replace Room."

A domain repository may eventually decide among:

~~~text
local only
remote only
local cache + remote authority
local authority + export
offline queue + reconciliation
~~~

That policy belongs behind the domain-specific repository contract and must be earned by the domain.
Do not put generic sync/freshness/conflict machinery into this foundation before a real consumer requires it.

## Testing layers

~~~text
unit:
  LogHistory + UserProfile fixtures
  InMemory repositories
  entity/domain mapper laws
  persistence queue ordering / failure isolation

integration:
  Room DAO + RoomLogHistoryRepository using an in-memory Android database

lived:
  app action -> logger -> queue -> Room -> Log History bottom sheet
  rotate Activity while sheet is open -> same Screen/sheet/session remains
  Reset -> database cleared -> new action appears as fresh history
~~~

The current source candidate includes unit-testable contracts/fixtures. Room integration/device proof
is still required before claiming this as a foundation checkpoint.

<!-- [VXG RealForever][VEXLIFE-ANDROID][DATA-REPOSITORY] -->