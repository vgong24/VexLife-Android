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

private class VexMapsPlaceSearchExecution(
    private val root:
        VexExecutionRoot,
    private val session:
        VexMapsPlaceSearchSession,
) :
    VexExecution(
        root,
    ) {
    fun importPack(
        work:
            VexMapsPlaceSearchImportWork,
    ) {
        forceStop(
            session::importForExecution,
            "VexMaps place-search import superseded",
        )
        launchOnce(
            session::importForExecution,
            work,
        )
    }

    fun search(
        work:
            VexMapsPlaceSearchWork,
    ) {
        forceStop(
            session::searchForExecution,
            "VexMaps place search superseded",
        )
        launchOnce(
            session::searchForExecution,
            work,
        )
    }

    fun cancelSearch() {
        forceStop(
            session::searchForExecution,
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
