# Data / Repository Foundation

[VXG RealForever]

## The question

> How can a page observe/persist data without knowing whether it came from Room, memory, a backend,
> or a future synchronization policy?

The first real consumer is Debug -> Log History. The architecture is intentionally learned through
that concrete need rather than by introducing a generic BaseRepository hierarchy.

## Shape

~~~text
page / runtime consumer
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

The Debug Log History dialog observes LogHistoryRepository rather than VexLogger's local Compose history.

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

## Log History persistence

Room is the Android-local substrate for this first repository.

Storage keeps:
- an auto-generated database row ID for global storage order;
- a generated sessionRef;
- VexLogRecord.sequence as session-local sequence;
- timestamp / duration / action / screen context / section.

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

The queue deliberately does NOT create an application scope. VexLifeApp currently supplies its
composition scope. When VexAppScope is admitted, lifecycle ownership should move there without
changing the repository/logger contract.

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
    appOpened(HomePage)
    afterSeconds(120)
    click(
        element = AppShell.Elements.DebugMenu,
        from = HomePage,
        to = DebugMenuPage,
    )
}

val repeatedReset = typical.append {
    afterSeconds(30)
    click(
        element = DebugMenuPage.ResetLog,
        from = DebugMenuPage,
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
  fixtures
  InMemory repository
  mapper laws
  persistence queue ordering / failure isolation

integration:
  Room DAO + RoomLogHistoryRepository using an in-memory Android database

lived:
  app action -> logger -> queue -> Room -> Log History dialog
  Reset -> database cleared -> new action appears as fresh history
~~~

The current source candidate includes unit-testable contracts/fixtures. Room integration/device proof
is still required before claiming this as a foundation checkpoint.

<!-- [VXG RealForever][VEXLIFE-ANDROID][DATA-REPOSITORY] -->