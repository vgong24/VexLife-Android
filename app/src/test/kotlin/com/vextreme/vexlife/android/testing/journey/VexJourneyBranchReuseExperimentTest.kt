package com.vextreme.vexlife.android.testing.journey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bounded experiment for the design nicknamed "QuantumTest" / "Vroomba".
 *
 * This intentionally stays test-local. It asks whether one authored route graph can generate complete
 * route identities, restore one exact shared checkpoint for multiple descendants, and preserve
 * child-specific failure evidence. It does not establish a reusable framework or parallel Android
 * execution policy.
 */
class VexJourneyBranchReuseExperimentTest {
    private val graph =
        RouteGraph(
            startNode = "start",
            edges =
                listOf(
                    edge("start", "paired", "J01.B01", "pair-approved", "paired"),
                    edge("paired", "catalog", "J02.B01", "catalog-visible", "catalog-visible"),
                    edge("catalog", "downloaded", "J03.B01", "download-verified", "download-verified"),
                    edge("downloaded", "removed", "J04.B01", "remove-download", "download-removed"),
                    edge("downloaded", "preserved", "J04.B02", "preserve-download", "download-preserved"),
                    edge("preserved", "offline", "J05.B02", "producer-offline", "producer-offline"),
                    edge("offline", "local-play", "J06.B01", "local-playback", "local-playback"),
                    edge("local-play", "restarted", "J07.B02", "app-restart", "app-restarted"),
                    edge("restarted", "restored", "J08.B01", "restore-offline-index", "offline-index-restored"),
                    edge("restored", "restored-play", "J09.B01", "restored-local-playback", "restored-local-playback"),
                ),
            terminals =
                mapOf(
                    "removed" to "download-and-remove",
                    "restored-play" to "producer-offline-restart",
                ),
        )

    @Test
    fun oneAuthoredGraphGeneratesTwoCompleteRoutesWithOneSharedPrefix() {
        val routes = graph.routes()

        assertEquals(2, routes.size)
        assertEquals(
            setOf(
                "download-and-remove",
                "producer-offline-restart",
            ),
            routes.map { route -> route.caseKey }.toSet(),
        )

        val sharedPrefix = sharedPrefix(routes)

        assertEquals(
            listOf(
                "J01.B01",
                "J02.B01",
                "J03.B01",
            ),
            sharedPrefix.map { edge -> edge.extensionRef },
        )
        assertEquals(
            "J01.B01/J02.B01/J03.B01/J04.B01",
            routes.single { it.caseKey == "download-and-remove" }.routeRef,
        )
        assertEquals(
            "J01.B01/J02.B01/J03.B01/J04.B02/J05.B02/J06.B01/J07.B02/J08.B01/J09.B01",
            routes.single { it.caseKey == "producer-offline-restart" }.routeRef,
        )
    }

    @Test
    fun restoredSharedCheckpointProducesTheSameSuccessfulSuffixResultAsFullReplay() {
        val route =
            graph.routes()
                .single { candidate ->
                    candidate.caseKey == "producer-offline-restart"
                }
        val prefix = sharedPrefix(graph.routes())
        val snapshot = executePrefix(prefix)

        val full = executeRoute(route)
        val restored = executeFromSnapshot(snapshot, route, prefix.size)

        assertEquals(
            full.finalState,
            restored.finalState,
        )
        assertEquals(
            full.completedExtensions.drop(prefix.size),
            restored.completedExtensions,
        )
        assertNull(full.failure)
        assertNull(restored.failure)
    }

    @Test
    fun oneFailedChildPreservesPassingSiblingAndNamesExactDivergence() {
        val routes = graph.routes()
        val prefix = sharedPrefix(routes)
        val snapshot = executePrefix(prefix)
        val removeRoute =
            routes.single { route ->
                route.caseKey == "download-and-remove"
            }
        val restartRoute =
            routes.single { route ->
                route.caseKey == "producer-offline-restart"
            }

        val results =
            listOf(
                executeFromSnapshot(
                    snapshot = snapshot,
                    route = removeRoute,
                    prefixLength = prefix.size,
                ),
                executeFromSnapshot(
                    snapshot = snapshot,
                    route = restartRoute,
                    prefixLength = prefix.size,
                    failAtExtensionRef = "J08.B01",
                ),
            )

        val passing =
            results.single { result ->
                result.caseKey == "download-and-remove"
            }
        val failing =
            results.single { result ->
                result.caseKey == "producer-offline-restart"
            }

        assertTrue(passing.passed)
        assertNull(passing.failure)

        assertFalse(failing.passed)
        assertEquals(snapshot.snapshotRef, failing.snapshotRef)
        assertEquals(restartRoute.routeRef, failing.routeRef)
        assertEquals("J08.B01", failing.failure?.extensionRef)
        assertEquals("restore-offline-index", failing.failure?.stepRef)
        assertEquals(4, failing.failure?.suffixStepIndex)
        assertEquals(
            listOf(
                "J04.B02",
                "J05.B02",
                "J06.B01",
                "J07.B02",
            ),
            failing.completedExtensions,
        )
        assertNotNull(failing.failure?.message)
    }

    @Test
    fun snapshotIdentityComesFromTheExactReachedCheckpointState() {
        val prefix = sharedPrefix(graph.routes())
        val first = executePrefix(prefix)
        val second = executePrefix(prefix)

        assertEquals(first.snapshotRef, second.snapshotRef)
        assertEquals(
            "snapshot:downloaded:paired|catalog-visible|download-verified",
            first.snapshotRef,
        )
    }

    private fun executePrefix(
        prefix: List<RouteEdge>,
    ): RouteSnapshot {
        val execution = RouteExecution()
        prefix.forEach(execution::apply)

        return RouteSnapshot(
            nodeId = prefix.last().toNode,
            checkpoints = execution.checkpoints.toSet(),
        )
    }

    private fun executeRoute(
        route: DerivedRoute,
    ): ChildRouteResult {
        val execution = RouteExecution()
        return executeEdges(
            snapshot = null,
            route = route,
            edges = route.edges,
            execution = execution,
            failAtExtensionRef = null,
        )
    }

    private fun executeFromSnapshot(
        snapshot: RouteSnapshot,
        route: DerivedRoute,
        prefixLength: Int,
        failAtExtensionRef: String? = null,
    ): ChildRouteResult {
        val execution =
            RouteExecution(
                checkpoints = snapshot.checkpoints.toMutableList(),
            )
        val suffix = route.edges.drop(prefixLength)

        return executeEdges(
            snapshot = snapshot,
            route = route,
            edges = suffix,
            execution = execution,
            failAtExtensionRef = failAtExtensionRef,
        )
    }

    private fun executeEdges(
        snapshot: RouteSnapshot?,
        route: DerivedRoute,
        edges: List<RouteEdge>,
        execution: RouteExecution,
        failAtExtensionRef: String?,
    ): ChildRouteResult {
        val completed = mutableListOf<String>()

        edges.forEachIndexed { index, edge ->
            if (edge.extensionRef == failAtExtensionRef) {
                return ChildRouteResult(
                    caseKey = route.caseKey,
                    routeRef = route.routeRef,
                    snapshotRef = snapshot?.snapshotRef,
                    completedExtensions = completed,
                    finalState = execution.checkpoints.toSet(),
                    failure =
                        ChildRouteFailure(
                            extensionRef = edge.extensionRef,
                            stepRef = edge.stepRef,
                            suffixStepIndex = index,
                            message = "synthetic branch failure at ${edge.extensionRef}",
                        ),
                )
            }

            execution.apply(edge)
            completed += edge.extensionRef
        }

        return ChildRouteResult(
            caseKey = route.caseKey,
            routeRef = route.routeRef,
            snapshotRef = snapshot?.snapshotRef,
            completedExtensions = completed,
            finalState = execution.checkpoints.toSet(),
            failure = null,
        )
    }

    private fun sharedPrefix(
        routes: List<DerivedRoute>,
    ): List<RouteEdge> {
        require(routes.isNotEmpty())
        val shortest = routes.minOf { route -> route.edges.size }

        val count =
            (0 until shortest)
                .takeWhile { index ->
                    routes.map { route -> route.edges[index].extensionRef }
                        .distinct()
                        .size == 1
                }
                .count()

        return routes.first().edges.take(count)
    }

    private fun edge(
        from: String,
        to: String,
        extensionRef: String,
        stepRef: String,
        checkpoint: String,
    ): RouteEdge =
        RouteEdge(
            fromNode = from,
            toNode = to,
            extensionRef = extensionRef,
            stepRef = stepRef,
            checkpoint = checkpoint,
        )

    private data class RouteEdge(
        val fromNode: String,
        val toNode: String,
        val extensionRef: String,
        val stepRef: String,
        val checkpoint: String,
    )

    private data class DerivedRoute(
        val caseKey: String,
        val edges: List<RouteEdge>,
    ) {
        val routeRef: String
            get() = edges.joinToString("/") { edge -> edge.extensionRef }
    }

    private data class RouteSnapshot(
        val nodeId: String,
        val checkpoints: Set<String>,
    ) {
        val snapshotRef: String
            get() =
                "snapshot:$nodeId:" +
                    checkpoints.sorted().joinToString("|")
    }

    private data class ChildRouteFailure(
        val extensionRef: String,
        val stepRef: String,
        val suffixStepIndex: Int,
        val message: String,
    )

    private data class ChildRouteResult(
        val caseKey: String,
        val routeRef: String,
        val snapshotRef: String?,
        val completedExtensions: List<String>,
        val finalState: Set<String>,
        val failure: ChildRouteFailure?,
    ) {
        val passed: Boolean
            get() = failure == null
    }

    private class RouteExecution(
        val checkpoints: MutableList<String> = mutableListOf(),
    ) {
        fun apply(edge: RouteEdge) {
            checkpoints += edge.checkpoint
        }
    }

    private data class RouteGraph(
        val startNode: String,
        val edges: List<RouteEdge>,
        val terminals: Map<String, String>,
    ) {
        fun routes(): List<DerivedRoute> =
            walk(
                nodeId = startNode,
                path = emptyList(),
            )

        private fun walk(
            nodeId: String,
            path: List<RouteEdge>,
        ): List<DerivedRoute> {
            terminals[nodeId]?.let { caseKey ->
                return listOf(
                    DerivedRoute(
                        caseKey = caseKey,
                        edges = path,
                    ),
                )
            }

            val next = edges.filter { edge -> edge.fromNode == nodeId }
            require(next.isNotEmpty()) {
                "route graph reached non-terminal node with no outgoing branch: $nodeId"
            }

            return next.flatMap { edge ->
                walk(
                    nodeId = edge.toNode,
                    path = path + edge,
                )
            }
        }
    }
}
