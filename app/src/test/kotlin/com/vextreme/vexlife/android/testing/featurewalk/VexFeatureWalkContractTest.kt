package com.vextreme.vexlife.android.testing.featurewalk

import com.vextreme.vexlife.android.conversation.vexConversationFeatureWalkProofBacklinks
import com.vextreme.vexlife.android.screens.shared.maps.vexMapsFeatureWalkProofBacklinks
import com.vextreme.vexlife.android.screens.shared.vexstream.vexStreamFeatureWalkProofBacklinks
import com.vextreme.vexlife.android.testing.journey.VexJourney
import com.vextreme.vexlife.android.testing.journey.VexJourneyCatalog
import com.vextreme.vexlife.android.testing.journey.VexJourneyShard
import com.vextreme.vexlife.android.testing.journey.featureWalkCatalogEntries
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexFeatureWalkContractTest {
    private val proofBacklinks =
        vexConversationFeatureWalkProofBacklinks +
            vexMapsFeatureWalkProofBacklinks +
            vexStreamFeatureWalkProofBacklinks

    @Test
    fun stableHierarchyIsUniqueAndDirectlyAddressable() {
        assertUnique(
            FeatureWalks.features.map { feature ->
                feature.featureRef
            },
            "feature refs",
        )
        assertUnique(
            FeatureWalks.walks.map { walk ->
                walk.ref
            },
            "Feature-Walk refs",
        )
        assertUnique(
            FeatureWalks.cases.map { case ->
                case.ref
            },
            "Feature-Walk Case refs",
        )
        assertUnique(
            FeatureWalks.checkpoints.map { checkpoint ->
                checkpoint.ref
            },
            "Feature-Walk Checkpoint refs",
        )

        FeatureWalks.cases.forEach { case ->
            assertSame(
                case,
                FeatureWalks.requireCase(
                    case.ref,
                ),
            )
        }
        FeatureWalks.checkpoints.forEach { checkpoint ->
            assertSame(
                checkpoint,
                FeatureWalks.requireCheckpoint(
                    checkpoint.ref,
                ),
            )
        }
    }

    @Test
    fun appendOnlyStableAddressesCanGrowPastTwoDigitsWithoutRenumbering() {
        val checkpoint =
            VexFeatureWalkCheckpoint(
                ref =
                    "feature.fixture/W100/C100/P100",
                key =
                    "checkpoint-100",
                title =
                    "A later checkpoint keeps its append-only address",
            )
        val case =
            VexFeatureWalkCase(
                ref =
                    "feature.fixture/W100/C100",
                key =
                    "case-100",
                title =
                    "A later case keeps its append-only address",
                checkpoints =
                    listOf(
                        checkpoint,
                    ),
            )
        val walk =
            VexFeatureWalk(
                ref =
                    "feature.fixture/W100",
                key =
                    "walk-100",
                title =
                    "A later walk keeps its append-only address",
                cases =
                    listOf(
                        case,
                    ),
            )

        val feature =
            VexFeatureWalkFeature(
                featureRef =
                    "feature.fixture",
                currentName =
                    "Fixture",
                walks =
                    listOf(
                        walk,
                    ),
            )

        assertEquals(
            "feature.fixture/W100/C100/P100",
            feature.walks
                .single()
                .cases
                .single()
                .checkpoints
                .single()
                .ref,
        )
    }

    @Test
    fun futureCaseOwnsFutureStateAndCannotCarryCurrentHolds() {
        val futureCheckpoint =
            VexFeatureWalkCheckpoint(
                ref =
                    "feature.fixture/W97/C97/P97",
                key =
                    "later-boundary",
                title =
                    "A future boundary inherits the Case frontier",
            )
        val futureCase =
            VexFeatureWalkCase(
                ref =
                    "feature.fixture/W97/C97",
                key =
                    "later-path",
                title =
                    "A future path",
                commitment =
                    VexFeatureWalkCommitment.FUTURE,
                checkpoints =
                    listOf(
                        futureCheckpoint,
                    ),
            )

        assertEquals(
            VexFeatureWalkCommitment.FUTURE,
            futureCase.commitment,
        )
        assertTrue(
            !futureCase.journeyExpectedNow,
        )

        val heldFutureFailure =
            runCatching {
                VexFeatureWalkCase(
                    ref =
                        "feature.fixture/W96/C96",
                    key =
                        "held-future-path",
                    title =
                        "Invalid held future path",
                    commitment =
                        VexFeatureWalkCommitment.FUTURE,
                    checkpoints =
                        listOf(
                            VexFeatureWalkCheckpoint(
                                ref =
                                    "feature.fixture/W96/C96/P96",
                                key =
                                    "future-hold",
                                title =
                                    "Future hold must not become current state",
                                hold =
                                    VexFeatureWalkHold(
                                        reason =
                                            "Synthetic invalid future hold.",
                                        releasePredicate =
                                            "Synthetic invalid release.",
                                    ),
                            ),
                        ),
                )
            }.exceptionOrNull()

        assertTrue(
            heldFutureFailure is IllegalArgumentException,
        )
    }

    @Test
    fun everyRequiredUnheldCaseHasExactlyOneTypedJourneyBinding() {
        val expectedCaseRefs =
            FeatureWalks.cases
                .filter { case ->
                    case.journeyExpectedNow
                }
                .map { case ->
                    case.ref
                }
                .toSet()
        val bindings =
            VexJourneyCatalog.featureWalkBindings
        val actualCaseRefs =
            bindings
                .map { entry ->
                    entry.binding.featureWalkCase.ref
                }
                .toSet()

        assertEquals(
            expectedCaseRefs,
            actualCaseRefs,
        )
        assertEquals(
            bindings.size,
            actualCaseRefs.size,
        )

        bindings.forEach { entry ->
            val featureWalkCase =
                entry.binding.featureWalkCase
            val journey =
                entry.binding.journey

            assertSame(
                FeatureWalks.requireCase(
                    featureWalkCase.ref,
                ),
                featureWalkCase,
            )
            assertEquals(
                featureWalkCase.key,
                journey.id,
            )
            assertEquals(
                featureWalkCase.title,
                journey.title,
            )
            assertTrue(
                journey.environments.any { environment ->
                    environment.id == "baseline"
                },
            )
        }
    }

    @Test
    fun featureWalkJourneyBindingCannotCreateAShadowJourneyOutsideItsShard() {
        val featureWalkCase =
            VexFeatureWalkCase(
                ref =
                    "feature.fixture/W98/C98",
                key =
                    "shadow-journey",
                title =
                    "Shadow Journey negative control",
                checkpoints =
                    listOf(
                        VexFeatureWalkCheckpoint(
                            ref =
                                "feature.fixture/W98/C98/P98",
                            key =
                                "reachable",
                            title =
                                "The Journey belongs to its shard",
                        ),
                    ),
            )
        val binding =
            vexFeatureWalkJourney(
                featureWalkCase =
                    featureWalkCase,
            ) {
            }
        val shard =
            object : VexJourneyShard {
                override val shardId: String =
                    "fixture.shadow"
                override val journeys: List<VexJourney> =
                    emptyList()
                override val featureWalkJourneys:
                    List<VexFeatureWalkJourneyBinding> =
                    listOf(
                        binding,
                    )
            }

        val failure =
            runCatching {
                shard.featureWalkCatalogEntries()
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException,
        )
        assertTrue(
            failure
                ?.message
                ?.contains(
                    "Feature-Walk Journey is not registered in shard",
                ) == true,
        )
    }

    @Test
    fun proofBacklinksResolveToRealFocusedTestsWithoutPromotingWholeCases() {
        assertUnique(
            proofBacklinks.map { backlink ->
                "${backlink.checkpointRef}|${backlink.testLocator}"
            },
            "Feature-Walk proof backlinks",
        )

        val proofCheckpointRefs =
            proofBacklinks
                .map { backlink ->
                    val checkpoint =
                        FeatureWalks.requireCheckpoint(
                            backlink.checkpointRef,
                        )
                    val method =
                        backlink.resolveMethod()

                    assertNotNull(
                        method.getAnnotation(
                            Test::class.java,
                        ),
                    )
                    val featureWalkCase =
                        FeatureWalks.requireCase(
                            backlink.checkpointRef
                                .substringBeforeLast('/'),
                        )
                    assertEquals(
                        VexFeatureWalkCommitment.REQUIRED,
                        featureWalkCase.commitment,
                    )

                    backlink.checkpointRef
                }
                .toSet()

        val journeyCaseRefs =
            VexJourneyCatalog.featureWalkBindings
                .map { entry ->
                    entry.binding.featureWalkCase.ref
                }
                .toSet()

        FeatureWalks.cases
            .filter { case ->
                case.commitment ==
                    VexFeatureWalkCommitment.REQUIRED
            }
            .forEach { case ->
                case.checkpoints
                    .filter { checkpoint ->
                        checkpoint.hold == null
                    }
                    .forEach { checkpoint ->
                    assertTrue(
                        "active checkpoint has neither Journey nor focused proof backlink: " +
                            checkpoint.ref,
                        case.ref in journeyCaseRefs ||
                            checkpoint.ref in proofCheckpointRefs,
                    )
                }
        }
    }

    @Test
    fun traceabilityReportKeepsConversationDoorwayAndVexMapsCompletionHeld() {
        val report =
            renderFeatureWalkReport(
                features =
                    FeatureWalks.features,
                journeyBindings =
                    VexJourneyCatalog.featureWalkBindings,
                proofBacklinks =
                    proofBacklinks,
            )

        assertTrue(
            report.contains(
                "${ConversationFeatureWalkRefs.ROUND_TRIP} status=HELD journey=HELD",
            ),
        )
        assertTrue(
            report.contains(
                "${ConversationFeatureWalkRefs.INSTALLED_DOORWAY} state=HELD",
            ),
        )
        assertTrue(
            report.contains(
                ConversationFeatureWalkRefs.VICTOR_SEND_ADMITTED,
            ),
        )
        assertTrue(
            report.contains(
                "${ThemeModeFeatureWalkRefs.TOGGLE_THEME_MODE} status=HELD journey=HELD",
            ),
        )
        assertTrue(
            report.contains(
                "${ThemeModeFeatureWalkRefs.INSTALLED_TOGGLE_REALIZATION} state=HELD",
            ),
        )

        assertTrue(
            report.contains(
                "${VexMapsFeatureWalkRefs.DESTINATION_SHELL} status=ACTIVE " +
                    "journey=shared.vex-maps/destination-shell",
            ),
        )
        assertTrue(
            report.contains(
                "${VexMapsFeatureWalkRefs.FIND_ROUTE_NAVIGATE} status=HELD journey=HELD",
            ),
        )
        assertTrue(
            report.contains(
                "${VexStreamFeatureWalkRefs.DOWNLOAD_AND_REMOVE} status=ACTIVE " +
                    "journey=shared.vexstream/download-and-remove",
            ),
        )
        assertTrue(
            report.contains(
                "${VexStreamFeatureWalkRefs.SORT_FILTER_PLAYBACK} status=ACTIVE " +
                    "journey=shared.vexstream/sort-filter-playback",
            ),
        )

        val reportPath =
            Path.of(
                "build",
                "reports",
                "vex",
                "feature-walks.txt",
            )
        Files.createDirectories(
            reportPath.parent,
        )
        Files.writeString(
            reportPath,
            report,
        )
    }

    private fun assertUnique(
        values: List<String>,
        label: String,
    ) {
        assertEquals(
            "$label must be unique",
            values.size,
            values.distinct().size,
        )
    }
}

private fun renderFeatureWalkReport(
    features: List<VexFeatureWalkFeature>,
    journeyBindings: List<VexFeatureWalkJourneyCatalogEntry>,
    proofBacklinks: List<VexFeatureWalkProofBacklink>,
): String {
    val journeyByCaseRef =
        journeyBindings.associateBy { entry ->
            entry.binding.featureWalkCase.ref
        }
    val proofsByCheckpointRef =
        proofBacklinks.groupBy { backlink ->
            backlink.checkpointRef
        }

    return buildString {
        appendLine("# Vex Feature Walk Traceability")
        appendLine()
        appendLine("projection=STRUCTURAL_LINKS_ONLY")
        appendLine("proofExecution=NOT_ASSERTED")
        appendLine("journeyExecution=NOT_ASSERTED")

        features.forEach { feature ->
            appendLine()
            appendLine("feature ${feature.featureRef} :: ${feature.currentName}")

            feature.walks.forEach { walk ->
                appendLine("  walk ${walk.ref} :: ${walk.title}")

                walk.cases.forEach { case ->
                    val journey =
                        journeyByCaseRef[case.ref]
                    val status =
                        when {
                            case.commitment ==
                                VexFeatureWalkCommitment.FUTURE ->
                                "FUTURE"

                            case.requiredHolds.isNotEmpty() ->
                                "HELD"

                            else ->
                                "ACTIVE"
                        }
                    val journeyProjection =
                        when {
                            journey != null ->
                                journey.journeyRef

                            status == "HELD" ->
                                "HELD"

                            status == "FUTURE" ->
                                "NOT_REQUIRED"

                            else ->
                                "MISSING"
                        }

                    appendLine(
                        "    case ${case.ref} status=$status journey=$journeyProjection :: ${case.title}",
                    )

                    case.checkpoints.forEach { checkpoint ->
                        val checkpointState =
                            when {
                                case.commitment ==
                                    VexFeatureWalkCommitment.FUTURE ->
                                    "FUTURE"

                                checkpoint.hold != null ->
                                    "HELD"

                                else ->
                                    "ACTIVE"
                            }
                        val proofs =
                            proofsByCheckpointRef[checkpoint.ref]
                                .orEmpty()
                                .joinToString(
                                    separator = ",",
                                ) { backlink ->
                                    backlink.testLocator
                                }
                                .ifBlank {
                                    "NONE"
                                }

                        appendLine(
                            "      checkpoint ${checkpoint.ref} state=$checkpointState " +
                                "proofBacklinks=$proofs :: ${checkpoint.title}",
                        )
                        checkpoint.hold?.let { hold ->
                            appendLine(
                                "        holdReason=${hold.reason}",
                            )
                            appendLine(
                                "        releasePredicate=${hold.releasePredicate}",
                            )
                        }
                    }
                }
            }
        }
    }
}

// [VXG RealForever]
