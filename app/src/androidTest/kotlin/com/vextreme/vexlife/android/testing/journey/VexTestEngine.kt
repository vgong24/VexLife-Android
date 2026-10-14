package com.vextreme.vexlife.android.testing.journey

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.text.intl.LocaleList
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.RelayEventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.loghistory.InMemoryLogHistoryRepository
import com.vextreme.vexlife.android.data.maps.InMemoryVexMapsRepository
import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamPlaylist
import com.vextreme.vexlife.android.data.vexstream.VexStreamPlaylistRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.TextFieldElement
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.ui.VexLifeApp
import androidx.test.espresso.Espresso
import org.junit.Assert.fail

/**
 * Android realization of the shared journey grammar.
 *
 * Every operation first advances the independent expected-walk model, then drives the real Compose
 * UI through stable production element IDs. Final verification reads the actual repository sink fed
 * by VexLogger -> LogHistoryPersistenceQueue.
 */
class VexTestEngine(
    private val compose: ComposeContentTestRule,
    private val observer: VexJourneyExecutionObserver =
        VexJourneyExecutionObserver.None,
) : VexJourneyScope {
    private val logHistory = InMemoryLogHistoryRepository()
    private val repositories = AppRepositories(
        logHistory = logHistory,
        userProfile = InMemoryUserProfileRepository(),
        vexMaps = InMemoryVexMapsRepository(),
        vexStream = vexStreamJourneyRepository(),
        events = RelayEventsRepository(),
        eventsAttention = RelayEventsAttentionPreferenceRepository(),
        eventsAttentionLab = RelayEventsAttentionLabRepository(),
    )
    private val expected = VexExpectedWalk()
    private var stepIndex = 0

    fun run(case: VexJourneyCase) {
        run(
            journey = case.journey,
            environment = case.environment,
            caseLabel = case.toString(),
        )
    }

    fun run(
        journey: VexJourney,
        environment: VexJourneyEnvironment,
        caseLabel: String,
    ) {
        compose.setContent {
            environment.Host {
                VexLifeApp(repositories = repositories)
            }
        }

        journey.walk(this)
        verifyWalkHistory(caseLabel)
        observer.afterJourney(
            caseLabel = caseLabel,
            finalScreenId = expected.currentScreen.screenId,
        )
    }

    override fun start(screen: ScreenElement) {
        expected.start(screen)

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(screen.screenId)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.waitForIdle()
        observe(
            stepKind = "start",
            semanticRef = screen.screenId,
        )
    }

    override fun signal(signal: VexJourneySignal) {
        signal.applyTo(repositories)
        compose.waitForIdle()
        observe(
            stepKind = "signal",
            semanticRef = signal.signalRef,
        )
    }

    override fun tapDynamic(element: VexDynamicElementRef) {
        expected.tapDynamic(element)

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(
                testTag = element.elementId,
                useUnmergedTree = true,
            )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag(
            testTag = element.elementId,
            useUnmergedTree = true,
        )
            .performClick()
        compose.waitForIdle()
        observe(
            stepKind = "tap-dynamic",
            semanticRef = element.elementId,
        )
    }

    override fun tap(element: ActionElement) {
        expected.tap(element)
        val uiElement = element as? UIElement
            ?: error("ActionElement must also be a UIElement")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(
                testTag = uiElement.elementId,
                useUnmergedTree = true,
            )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        node(element)
            .scrollIntoViewIfPossible()
            .performClick()
        compose.waitForIdle()
        observe(
            stepKind = "tap",
            semanticRef = (element as UIElement).elementId,
        )
    }

    override fun type(
        field: TextFieldElement,
        value: String,
    ) {
        expected.type(field, value)

        val node = compose.onNodeWithTag(
            testTag = field.elementId,
            useUnmergedTree = true,
        )
            .scrollIntoViewIfPossible()
        node.performTextClearance()
        node.performTextInput(value)
        compose.waitForIdle()
        observe(
            stepKind = "type",
            semanticRef = field.elementId,
        )
    }

    override fun submit(element: ActionElement) {
        expected.submit(element)
        node(element)
            .scrollIntoViewIfPossible()
            .performClick()
        compose.waitForIdle()
        observe(
            stepKind = "submit",
            semanticRef = (element as UIElement).elementId,
        )
    }

    override fun dismiss(element: UIElement) {
        expected.dismiss(element)

        compose.onNodeWithTag(
            testTag = element.elementId,
            useUnmergedTree = true,
        )
            .performTouchInput {
                swipeDown()
            }

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(
                testTag = element.elementId,
                useUnmergedTree = true,
            )
                .fetchSemanticsNodes()
                .isEmpty()
        }
        observe(
            stepKind = "dismiss",
            semanticRef = element.elementId,
        )
    }

    override fun back() {
        expected.back()
        Espresso.pressBack()
        compose.waitForIdle()
        observe(
            stepKind = "back",
            semanticRef = "system.back",
        )
    }

    private fun observe(
        stepKind: String,
        semanticRef: String?,
    ) {
        observer.afterStep(
            VexJourneyStepObservation(
                stepIndex = stepIndex++,
                stepKind = stepKind,
                screenId = expected.currentScreen.screenId,
                semanticRef = semanticRef,
            ),
        )
    }

    private fun verifyWalkHistory(title: String) {
        val wanted = expected.records

        /*
         * Persistence is asynchronous. Wait for the exact semantic sequence when it arrives, but do
         * not let the synchronization timeout hide the actual mismatch. The pure verifier below
         * becomes the authoritative failure report.
         */
        val waitFailure = runCatching {
            compose.waitUntil(timeoutMillis = 5_000) {
                actualHistory() == wanted
            }
        }.exceptionOrNull()

        val comparison = VexJourneyHistoryVerifier.compare(
            expected = wanted,
            actual = actualHistory(),
        )

        if (!comparison.matches) {
            fail(
                buildString {
                    append(comparison.render(title))
                    waitFailure?.let { failure ->
                        appendLine()
                        appendLine()
                        appendLine(
                            "wait/synchronization failure: " +
                                "${failure::class.simpleName}: ${failure.message}",
                        )
                    }
                },
            )
        }

        // If histories match but the wait primitive failed for another reason, preserve that failure.
        if (waitFailure != null) {
            throw waitFailure
        }
    }

    private fun actualHistory(): List<VexExpectedRecord> =
        logHistory.records.value.map { record ->
            record.toJourneyRecord()
        }

    private fun node(element: ActionElement): SemanticsNodeInteraction {
        val uiElement = element as? UIElement
            ?: error("ActionElement must also be a UIElement")
        return compose.onNodeWithTag(
            testTag = uiElement.elementId,
            useUnmergedTree = true,
        )
    }
}

private fun vexStreamJourneyRepository(): InMemoryVexStreamMusicRepository {
    val track =
        VexStreamTrack(
            trackRef =
                VexStreamTrackRef(
                    "journey.track.one",
                ),
            mediaSha256 =
                VexStreamMediaSha256(
                    "1111111111111111111111111111111111111111111111111111111111111111",
                ),
            mediaByteLength = 4_096L,
            title = "Journey Song",
            artist = "Journey Artist",
            album = "Journey Album",
            genre = "Journey",
            durationSeconds = 180.0,
        )

    return InMemoryVexStreamMusicRepository(
        initialState =
            VexStreamMusicState(
                tracks =
                    listOf(
                        VexStreamTrackState(
                            track = track,
                            hubAvailable = true,
                        ),
                    ),
                playlists =
                    listOf(
                        VexStreamPlaylist(
                            playlistRef =
                                VexStreamPlaylistRef(
                                    "journey.playlist.one",
                                ),
                            name = "Journey Playlist",
                            trackRefs =
                                listOf(
                                    track.trackRef,
                                ),
                        ),
                    ),
            ),
    )
}

private fun SemanticsNodeInteraction.scrollIntoViewIfPossible(): SemanticsNodeInteraction {
    runCatching {
        performScrollTo()
    }
    return this
}



@Composable
private fun VexJourneyEnvironment.Host(
    content: @Composable () -> Unit,
) {
    if (fontScale != null) {
        DeviceConfigurationOverride(
            DeviceConfigurationOverride.FontScale(fontScale),
        ) {
            LocaleHost(content)
        }
    } else {
        LocaleHost(content)
    }
}

@Composable
private fun VexJourneyEnvironment.LocaleHost(
    content: @Composable () -> Unit,
) {
    if (localeTag != null) {
        DeviceConfigurationOverride(
            DeviceConfigurationOverride.Locales(LocaleList(localeTag)),
        ) {
            content()
        }
    } else {
        content()
    }
}
