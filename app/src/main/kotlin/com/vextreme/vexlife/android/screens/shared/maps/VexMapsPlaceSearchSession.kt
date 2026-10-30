package com.vextreme.vexlife.android.screens.shared.maps

import android.net.Uri
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider/custody boundary consumed by [VexMapsPlaceSearchSession].
 *
 * The SQLite implementation owns pack validation/replacement and query execution. The Session owns
 * only which pack/search attempt/result is current for the lived Maps planning surface.
 */
internal interface VexMapsPlaceSearchSource {
    fun currentPack(): VexMapsPlaceSearchPack?

    suspend fun import(
        source: Uri,
    ): VexMapsPlaceSearchPack

    suspend fun search(
        pack: VexMapsPlaceSearchPack,
        query: String,
        limit: Int = 5,
    ): List<VexMapsPlaceSearchResult>
}

internal data class VexMapsPlaceSearchSessionState(
    val pack:
        VexMapsPlaceSearchPack?,
    val search:
        VexMapsPlaceSearchState =
        VexMapsPlaceSearchState.Idle,
)

internal data class VexMapsPlaceSearchImportWork(
    val generation: Long,
    val source: Uri,
)

internal data class VexMapsPlaceSearchWork(
    val generation: Long,
    val pack: VexMapsPlaceSearchPack,
    val query: String,
)

/**
 * Lived currentness owner for VexMaps local place search.
 *
 * The human query text remains presentation/input state. [VexMapsPlaceSearchStore] remains the
 * provider/custody owner. This Session answers only:
 *
 * - which installed pack is current for this Maps lifetime;
 * - which import/search attempt is current;
 * - which result/error may still be admitted after asynchronous work returns.
 *
 * Search/import execution is delegated to [VexMapsPlaceSearchExecution] so Compose does not own
 * provider Jobs or stale-result admission.
 *
 * Provider attempts are separately admitted through Session-owned relationships. Cancellation is
 * best-effort cleanup; the generation gates remain the semantic currentness owner when an older
 * provider returns after cancellation.
 */
internal class VexMapsPlaceSearchSession(
    private val source:
        VexMapsPlaceSearchSource,
    executionRoot:
        VexExecutionRoot,
) {
    private val searchGate =
        VexMapsLatestRequestGate()
    private val importGate =
        VexMapsLatestRequestGate()

    private val mutableState =
        MutableStateFlow(
            VexMapsPlaceSearchSessionState(
                pack =
                    source.currentPack(),
            ),
        )

    val states:
        StateFlow<VexMapsPlaceSearchSessionState> =
        mutableState.asStateFlow()

    val current:
        VexMapsPlaceSearchSessionState
        get() = mutableState.value

    private val execution =
        VexMapsPlaceSearchExecution(
            root =
                executionRoot,
            session =
                this,
        )

    /**
     * Query/selection context changed.
     *
     * A late prior search must not replace meaning for the new input. Pack import remains independent
     * and is not cancelled merely because the human edited search text.
     */
    fun searchInputChanged() {
        searchGate.invalidate()
        execution.cancelSearch()

        val current =
            mutableState.value
        if (
            current.search !is
                VexMapsPlaceSearchState.Importing
        ) {
            mutableState.value =
                current.copy(
                    search =
                        VexMapsPlaceSearchState.Idle,
                )
        }
    }

    /**
     * The human opened the document picker for a replacement place pack.
     *
     * Match the prior composition behavior: invalidate any in-flight search so it cannot land while
     * the picker is open, but keep already-visible search results until an actual replacement URI is
     * admitted through [importPack].
     */
    fun importSelectionStarted() {
        searchGate.invalidate()
        execution.cancelSearch()
    }

    fun importPack(
        source: Uri,
    ) {
        searchGate.invalidate()
        execution.cancelSearch()

        val generation =
            importGate.begin()
        mutableState.value =
            mutableState.value.copy(
                search =
                    VexMapsPlaceSearchState.Importing,
            )

        execution.importPack(
            VexMapsPlaceSearchImportWork(
                generation =
                    generation,
                source =
                    source,
            ),
        )
    }

    fun search(
        query: String,
    ) {
        val current =
            mutableState.value
        val pack =
            current.pack
                ?: return
        val normalized =
            query.trim()

        if (
            normalized.isEmpty() ||
            current.search is
                VexMapsPlaceSearchState.Importing
        ) {
            return
        }

        val generation =
            searchGate.begin()
        mutableState.value =
            current.copy(
                search =
                    VexMapsPlaceSearchState.Searching,
            )

        execution.search(
            VexMapsPlaceSearchWork(
                generation =
                    generation,
                pack =
                    pack,
                query =
                    normalized,
            ),
        )
    }

    fun resultConsumed() {
        searchInputChanged()
    }

    fun close() {
        searchGate.invalidate()
        importGate.invalidate()
        execution.shutdown()
    }

    internal suspend fun importForExecution(
        work:
            VexMapsPlaceSearchImportWork,
    ) {
        try {
            val imported =
                source.import(
                    work.source,
                )

            if (
                !importGate.accepts(
                    work.generation,
                )
            ) {
                return
            }

            mutableState.value =
                VexMapsPlaceSearchSessionState(
                    pack =
                        imported,
                    search =
                        VexMapsPlaceSearchState.Idle,
                )
        } catch (
            cancellation:
                CancellationException
        ) {
            throw cancellation
        } catch (
            _: Throwable
        ) {
            if (
                importGate.accepts(
                    work.generation,
                )
            ) {
                mutableState.value =
                    mutableState.value.copy(
                        search =
                            VexMapsPlaceSearchState.Error(
                                VexMapsPlaceSearchError.IMPORT,
                            ),
                    )
            }
        }
    }

    internal suspend fun searchForExecution(
        work:
            VexMapsPlaceSearchWork,
    ) {
        try {
            val places =
                source.search(
                    pack =
                        work.pack,
                    query =
                        work.query,
                )

            if (
                !searchGate.accepts(
                    work.generation,
                )
            ) {
                return
            }

            mutableState.value =
                mutableState.value.copy(
                    search =
                        VexMapsPlaceSearchState.Results(
                            places,
                        ),
                )
        } catch (
            cancellation:
                CancellationException
        ) {
            throw cancellation
        } catch (
            _: Throwable
        ) {
            if (
                searchGate.accepts(
                    work.generation,
                )
            ) {
                mutableState.value =
                    mutableState.value.copy(
                        search =
                            VexMapsPlaceSearchState.Error(
                                VexMapsPlaceSearchError.SEARCH,
                            ),
                    )
            }
        }
    }
}

@OptIn(VexExecution.RelationshipFoundationApi::class)
private class VexMapsPlaceSearchExecution(
    private val root:
        VexExecutionRoot,
    private val session:
        VexMapsPlaceSearchSession,
) :
    VexExecution<
        VexMapsPlaceSearchSession
    >(
        root,
    ) {
    private enum class RelationshipKind {
        Import,
        Search,
    }

    private class Relationship(
        private val session:
            VexMapsPlaceSearchSession,
        private val kind:
            RelationshipKind,
    ) :
        VexExecution.RelationshipId<
            VexMapsPlaceSearchSession
        > {
        override fun equals(
            other:
                Any?,
        ):
            Boolean =
            other is
                Relationship &&
                session ===
                other.session &&
                kind ==
                other.kind

        override fun hashCode():
            Int =
            31 *
                System.identityHashCode(
                    session,
                ) +
                kind.hashCode()
    }

    private val importRelationship =
        Relationship(
            session =
                session,
            kind =
                RelationshipKind.Import,
        )

    private val searchRelationship =
        Relationship(
            session =
                session,
            kind =
                RelationshipKind.Search,
        )

    fun importPack(
        work:
            VexMapsPlaceSearchImportWork,
    ) {
        launch(
            relationshipId =
                importRelationship,
            approach =
                VexExecution.Approach.Latest,
            action =
                session::importForExecution,
            argument =
                work,
        )
    }

    fun search(
        work:
            VexMapsPlaceSearchWork,
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
                "VexMaps place search invalidated",
        )
    }

    fun shutdown() {
        root.forceStopAll(
            "VexMaps place-search session shut down",
        )
    }
}

// [VXG RealForever]
