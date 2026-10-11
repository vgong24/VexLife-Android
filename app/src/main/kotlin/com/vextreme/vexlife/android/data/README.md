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
  userprofile/
    UserProfile.kt
    UserProfileRepository.kt
    RoomUserProfileRepository.kt
    InMemoryUserProfileRepository.kt
    UserProfileDao.kt
    UserProfileEntity.kt
    UserProfileFixture.kt
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

Log History demonstrates a multi-statement @Transaction boundary (insert + retention prune).
User Profile demonstrates single-row upsert/update and schema migration without pretending profile
data owns future VexHome/VexStream connection state.

See `docs/foundation/DATA-REPOSITORY.md`.

<!-- [VXG RealForever][VEXLIFE-ANDROID][DATA] -->