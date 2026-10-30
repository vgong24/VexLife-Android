package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface VexMapsDestinationSearchSource {
    fun isAvailable(): Boolean

    suspend fun search(
        query: String,
        limit: Int = 5,
    ): List<VexMapsPlaceSearchResult>
}

internal class VexMapsDestinationSearchUnavailableException :
    IllegalStateException(
        "VexMaps destination-search provider is unavailable",
    )

internal sealed interface VexMapsDestinationSearchState {
    data object Unavailable :
        VexMapsDestinationSearchState

    data object Idle :
        VexMapsDestinationSearchState

    data object Searching :
        VexMapsDestinationSearchState

    data object NoResults :
        VexMapsDestinationSearchState

    data class Results(
        val places:
            List<VexMapsPlaceSearchResult>,
    ) :
        VexMapsDestinationSearchState

    data object Error :
        VexMapsDestinationSearchState
}

internal data class VexMapsDestinationSearchWork(
    val generation: Long,
    val query: String,
)

/**
 * Lived currentness owner for the ordinary visible "Where to?" destination search.
 *
 * This does not own local place-pack import, route planning, selected-destination truth, or provider
 * implementation. It answers only which submitted destination search attempt/result is current.
 *
 * Provider attempts are separately admitted by the Session-owned relationship. Cancellation is best-effort
 * resource cleanup; [VexMapsLatestRequestGate] remains the semantic currentness owner when an older
 * provider ignores cancellation long enough to return late.
 */
internal class VexMapsDestinationSearchSession(
    private val source:
        VexMapsDestinationSearchSource,
    executionRoot:
        VexExecutionRoot,
) {
    private val searchGate =
        VexMapsLatestRequestGate()

    private val mutableState =
        MutableStateFlow<
            VexMapsDestinationSearchState
        >(
            initialState(),
        )

    val states:
        StateFlow<
            VexMapsDestinationSearchState
        > =
        mutableState.asStateFlow()

    val current:
        VexMapsDestinationSearchState
        get() = mutableState.value

    private val execution =
        VexMapsDestinationSearchExecution(
            root = executionRoot,
            session = this,
        )

    fun queryChanged() {
        searchGate.invalidate()
        execution.cancelSearch()
        mutableState.value =
            initialState()
    }

    fun search(
        query: String,
    ) {
        val normalized =
            query.trim()

        if (normalized.isEmpty()) {
            queryChanged()
            return
        }

        if (!source.isAvailable()) {
            searchGate.invalidate()
            execution.cancelSearch()
            mutableState.value =
                VexMapsDestinationSearchState
                    .Unavailable
            return
        }

        val generation =
            searchGate.begin()
        mutableState.value =
            VexMapsDestinationSearchState
                .Searching

        execution.search(
            VexMapsDestinationSearchWork(
                generation = generation,
                query = normalized,
            ),
        )
    }

    fun resultConsumed() {
        queryChanged()
    }

    fun close() {
        searchGate.invalidate()
        execution.shutdown()
    }

    internal suspend fun searchForExecution(
        work: VexMapsDestinationSearchWork,
    ) {
        try {
            val places =
                source.search(
                    query = work.query,
                )

            if (
                !searchGate.accepts(
                    work.generation,
                )
            ) {
                return
            }

            mutableState.value =
                if (places.isEmpty()) {
                    VexMapsDestinationSearchState
                        .NoResults
                } else {
                    VexMapsDestinationSearchState
                        .Results(
                            places,
                        )
                }
        } catch (
            cancellation:
                CancellationException,
        ) {
            throw cancellation
        } catch (
            _: VexMapsDestinationSearchUnavailableException,
        ) {
            if (
                searchGate.accepts(
                    work.generation,
                )
            ) {
                mutableState.value =
                    VexMapsDestinationSearchState
                        .Unavailable
            }
        } catch (
            _: Throwable,
        ) {
            if (
                searchGate.accepts(
                    work.generation,
                )
            ) {
                mutableState.value =
                    VexMapsDestinationSearchState
                        .Error
            }
        }
    }

    private fun initialState():
        VexMapsDestinationSearchState =
        if (source.isAvailable()) {
            VexMapsDestinationSearchState
                .Idle
        } else {
            VexMapsDestinationSearchState
                .Unavailable
        }
}

@OptIn(VexExecution.RelationshipFoundationApi::class)
private class VexMapsDestinationSearchExecution(
    private val root:
        VexExecutionRoot,
    private val session:
        VexMapsDestinationSearchSession,
) :
    VexExecution<
        VexMapsDestinationSearchSession
    >(
        root,
    ) {
    private class SearchRelationship(
        private val session:
            VexMapsDestinationSearchSession,
    ) :
        VexExecution.RelationshipId<
            VexMapsDestinationSearchSession
        > {
        override fun equals(
            other:
                Any?,
        ):
            Boolean =
            other is
                SearchRelationship &&
                session ===
                other.session

        override fun hashCode():
            Int =
            System.identityHashCode(
                session,
            )
    }

    private val searchRelationship =
        SearchRelationship(
            session,
        )

    fun search(
        work: VexMapsDestinationSearchWork,
    ) {
        launch(
            relationshipId =
                searchRelationship,
            approach =
                VexExecution.Approach.Latest,
            action =
                session::searchForExecution,
            argument =
                work,
        )
    }

    fun cancelSearch() {
        forceStop(
            relationshipId =
                searchRelationship,
            reason =
                "VexMaps destination search invalidated",
        )
    }

    fun shutdown() {
        root.forceStopAll(
            "VexMaps destination-search session shut down",
        )
    }
}

// [VXG RealForever]
