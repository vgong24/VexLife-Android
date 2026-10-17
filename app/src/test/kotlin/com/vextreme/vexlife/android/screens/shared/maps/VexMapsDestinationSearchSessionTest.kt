package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.execution.VexExecutionRoot
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

class VexMapsDestinationSearchSessionTest {
    @Test
    fun newerSearchOwnsResultsEvenIfOlderProviderReturnsLate() =
        runBlocking {
            val source =
                FakeDestinationSearchSource()
            val session =
                session(source)

            session.search("old")
            withTimeout(1_000L) {
                source.oldStarted.await()
            }

            session.queryChanged()
            session.search("new")

            withTimeout(1_000L) {
                session.states
                    .first { state ->
                        state.resultRefs() ==
                            listOf("new")
                    }
            }

            source.oldRelease.complete(Unit)
            yield()

            assertEquals(
                listOf("new"),
                session.current.resultRefs(),
            )

            session.close()
        }

    @Test
    fun unavailableProviderFailsClosedWithoutStartingWork() {
        val source =
            FakeDestinationSearchSource(
                available = false,
            )
        val session =
            session(source)

        assertSame(
            VexMapsDestinationSearchState
                .Unavailable,
            session.current,
        )

        session.search("Palmdale")

        assertSame(
            VexMapsDestinationSearchState
                .Unavailable,
            session.current,
        )
        assertEquals(
            emptyList<String>(),
            source.queries,
        )

        session.close()
    }

    @Test
    fun emptyProviderResultIsVisibleNoResultsTruth() =
        runBlocking {
            val source =
                FakeDestinationSearchSource()
            val session =
                session(source)

            session.search("none")

            withTimeout(1_000L) {
                session.states
                    .first { state ->
                        state ===
                            VexMapsDestinationSearchState
                                .NoResults
                    }
            }

            session.close()
        }

    private fun session(
        source: VexMapsDestinationSearchSource,
    ) =
        VexMapsDestinationSearchSession(
            source = source,
            executionRoot =
                VexExecutionRoot(
                    dispatcher =
                        Dispatchers.Unconfined,
                ),
        )

    private class FakeDestinationSearchSource(
        private val available: Boolean = true,
    ) :
        VexMapsDestinationSearchSource {
        val oldStarted =
            CompletableDeferred<Unit>()
        val oldRelease =
            CompletableDeferred<Unit>()
        val queries =
            mutableListOf<String>()

        override fun isAvailable(): Boolean =
            available

        override suspend fun search(
            query: String,
            limit: Int,
        ): List<VexMapsPlaceSearchResult> {
            queries += query

            return when (query) {
                "old" -> {
                    oldStarted.complete(Unit)
                    withContext(NonCancellable) {
                        oldRelease.await()
                    }
                    listOf(place("old"))
                }

                "none" ->
                    emptyList()

                else ->
                    listOf(place(query))
            }
        }

        private fun place(
            ref: String,
        ) =
            VexMapsPlaceSearchResult(
                placeRef = ref,
                name = ref,
                subtitle = null,
                coordinate =
                    VexMapCoordinate(
                        latitude = 34.5,
                        longitude = -118.1,
                    ),
            )
    }
}

private fun VexMapsDestinationSearchState.resultRefs():
    List<String> =
    (
        this as?
            VexMapsDestinationSearchState.Results
    )
        ?.places
        ?.map {
            it.placeRef
        }
        .orEmpty()

// [VXG RealForever]
