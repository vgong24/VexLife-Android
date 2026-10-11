# data

[VXG RealForever]

Start here when asking:

> Where does this page's data come from, and who owns persistence?

## Folder shape

~~~text
data/
  AppRepositories.kt
  VexLifeDatabase.kt
  loghistory/
    LogHistoryRepository.kt
    RoomLogHistoryRepository.kt
    InMemoryLogHistoryRepository.kt
    LogHistoryDao.kt
    LogHistoryEntity.kt
    LogHistoryPersistenceQueue.kt
    LogHistoryFixture.kt
~~~

The important command-click direction is:

~~~text
page
-> repository interface
-> production implementation
-> DAO/entity only when debugging storage
~~~

Do not make pages consume DAOs or entities directly.

`InMemoryLogHistoryRepository` and `LogHistoryFixture` exist so previews/tests can reproduce exact
states through the same repository contract instead of teaching production code about mocks.

There is deliberately no generic BaseRepository<T>.

See `docs/foundation/DATA-REPOSITORY.md`.

<!-- [VXG RealForever][VEXLIFE-ANDROID][DATA] -->