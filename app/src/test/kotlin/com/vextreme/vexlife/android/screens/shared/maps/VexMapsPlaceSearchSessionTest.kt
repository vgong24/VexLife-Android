package com.vextreme.vexlife.android.screens.shared.maps

import android.net.Uri
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VexMapsPlaceSearchSessionTest {
    @Test
    fun newerSearchOwnsResultEvenIfCancelledOlderProviderReturnsLate() =
        runBlocking {
            val source =
                FakePlaceSearchSource()
            val session =
                session(
                    source,
                )

            session.search(
                "old",
            )
            withTimeout(
                1_000L,
            ) {
                source.oldStarted.await()
            }

            session.searchInputChanged()
            session.search(
                "new",
            )

            withTimeout(
                1_000L,
            ) {
                session.states
                    .first { state ->
                        state.search
                            .resultRefs() ==
                            listOf(
                                "new",
                            )
                    }
            }

            source.oldRelease.complete(
                Unit,
            )
            yield()

            assertEquals(
                listOf(
                    "new",
                ),
                session.current
                    .search
                    .resultRefs(),
            )
            assertEquals(
                listOf(
                    "old",
                    "new",
                ),
                source.queries,
            )

            session.close()
        }

    @Test
    fun searchInputChangeClearsResultsWithoutChangingCurrentPack() =
        runBlocking {
            val source =
                FakePlaceSearchSource()
            val session =
                session(
                    source,
                )

            session.search(
                "new",
            )

            withTimeout(
                1_000L,
            ) {
                session.states
                    .first { state ->
                        state.search
                            .resultRefs() ==
                            listOf(
                                "new",
                            )
                    }
            }

            val pack =
                session.current.pack
            session.searchInputChanged()

            assertSame(
                pack,
                session.current.pack,
            )
            assertSame(
                VexMapsPlaceSearchState.Idle,
                session.current.search,
            )

            session.close()
        }

    @Test
    fun importChooserOpeningPreservesCompletedResults() =
        runBlocking {
            val source =
                FakePlaceSearchSource()
            val session =
                session(
                    source,
                )

            session.search(
                "new",
            )

            withTimeout(
                1_000L,
            ) {
                session.states
                    .first { state ->
                        state.search
                            .resultRefs() ==
                            listOf(
                                "new",
                            )
                    }
            }

            session.importSelectionStarted()

            assertEquals(
                listOf(
                    "new",
                ),
                session.current
                    .search
                    .resultRefs(),
            )

            session.close()
        }

    @Test
    fun searchFailureIsOwnedByPlaceSearchSession() =
        runBlocking {
            val source =
                FakePlaceSearchSource()
            val session =
                session(
                    source,
                )

            session.search(
                "fail",
            )

            val error =
                withTimeout(
                    1_000L,
                ) {
                    session.states
                        .first { state ->
                            state.search is
                                VexMapsPlaceSearchState.Error
                        }
                        .search as
                        VexMapsPlaceSearchState.Error
                }

            assertSame(
                VexMapsPlaceSearchError.SEARCH,
                error.error,
            )

            session.close()
        }

    private fun session(
        source:
            VexMapsPlaceSearchSource,
    ): VexMapsPlaceSearchSession =
        VexMapsPlaceSearchSession(
            source =
                source,
            executionRoot =
                VexExecutionRoot(
                    dispatcher =
                        Dispatchers.Unconfined,
                ),
        )

    private class FakePlaceSearchSource :
        VexMapsPlaceSearchSource {
        val pack =
            VexMapsPlaceSearchPack(
                database =
                    File(
                        "fake-places.sqlite",
                    ),
                byteLength =
                    1L,
                lastModifiedMillis =
                    1L,
            )

        val oldStarted =
            CompletableDeferred<Unit>()
        val oldRelease =
            CompletableDeferred<Unit>()
        val queries =
            mutableListOf<String>()

        override fun currentPack():
            VexMapsPlaceSearchPack =
            pack

        override suspend fun import(
            source: Uri,
        ): VexMapsPlaceSearchPack =
            error(
                "import is not used by these tests",
            )

        override suspend fun search(
            pack: VexMapsPlaceSearchPack,
            query: String,
            limit: Int,
        ): List<VexMapsPlaceSearchResult> {
            assertSame(
                this.pack,
                pack,
            )
            queries +=
                query

            return when (
                query
            ) {
                "old" -> {
                    oldStarted.complete(
                        Unit,
                    )
                    withContext(
                        NonCancellable,
                    ) {
                        oldRelease.await()
                    }
                    listOf(
                        place(
                            "old",
                        ),
                    )
                }

                "fail" ->
                    error(
                        "search failed",
                    )

                else ->
                    listOf(
                        place(
                            query,
                        ),
                    )
            }
        }

        private fun place(
            ref: String,
        ): VexMapsPlaceSearchResult =
            VexMapsPlaceSearchResult(
                placeRef =
                    ref,
                name =
                    ref,
                subtitle =
                    null,
                coordinate =
                    VexMapCoordinate(
                        latitude =
                            34.0,
                        longitude =
                            -118.0,
                    ),
            )
    }
}

private fun VexMapsPlaceSearchState.resultRefs():
    List<String> =
    (
        this as?
            VexMapsPlaceSearchState.Results
    )
        ?.places
        ?.map {
            it.placeRef
        }
        .orEmpty()

// [VXG RealForever]
