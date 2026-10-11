package com.vextreme.vexlife.android.testing.journey

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.loghistory.InMemoryLogHistoryRepository
import com.vextreme.vexlife.android.data.userprofile.InMemoryUserProfileRepository
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.TextFieldElement
import com.vextreme.vexlife.android.elements.UIElement
import com.vextreme.vexlife.android.logging.VexLogRecord
import com.vextreme.vexlife.android.ui.VexLifeApp
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals

/**
 * Android realization of the shared journey grammar.
 *
 * Every operation first advances the independent expected-walk model, then drives the real Compose
 * UI through stable production element IDs. Final verification reads the actual repository sink fed
 * by VexLogger -> LogHistoryPersistenceQueue.
 */
class VexTestEngine(
    private val compose: ComposeTestRule,
) : VexJourneyScope {
    private val logHistory = InMemoryLogHistoryRepository()
    private val repositories = AppRepositories(
        logHistory = logHistory,
        userProfile = InMemoryUserProfileRepository(),
    )
    private val expected = VexExpectedWalk()

    fun run(journey: VexJourney) {
        compose.setContent {
            VexLifeApp(repositories = repositories)
        }

        journey.walk(this)
        verifyWalkHistory(journey.title)
    }

    override fun start(screen: ScreenElement) {
        expected.start(screen)

        val root = com.vextreme.vexlife.android.screens.AppScreens.registry
            .screenRoot(screen)
            .element
        compose.onNodeWithTag(root.elementId)
            .assertExists()
    }

    override fun tap(element: ActionElement) {
        expected.tap(element)
        node(element)
            .scrollIntoViewIfPossible()
            .performClick()
        compose.waitForIdle()
    }

    override fun type(
        field: TextFieldElement,
        value: String,
    ) {
        expected.type(field, value)

        val node = compose.onNodeWithTag(field.elementId)
            .scrollIntoViewIfPossible()
        node.performTextClearance()
        node.performTextInput(value)
        compose.waitForIdle()
    }

    override fun submit(element: ActionElement) {
        expected.submit(element)
        node(element)
            .scrollIntoViewIfPossible()
            .performClick()
        compose.waitForIdle()
    }

    override fun dismiss(element: UIElement) {
        expected.dismiss(element)

        compose.onNodeWithTag(element.elementId)
            .performTouchInput {
                swipeDown()
            }

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(element.elementId)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    override fun back() {
        expected.back()
        Espresso.pressBack()
        compose.waitForIdle()
    }

    private fun verifyWalkHistory(title: String) {
        val wanted = expected.records

        compose.waitUntil(timeoutMillis = 5_000) {
            actualHistory() == wanted
        }

        assertEquals(
            "semantic walk history mismatch for journey: $title",
            wanted,
            actualHistory(),
        )
    }

    private fun actualHistory(): List<VexExpectedRecord> =
        logHistory.records.value.map(VexLogRecord::semanticProjection)

    private fun node(element: ActionElement): SemanticsNodeInteraction {
        val uiElement = element as? UIElement
            ?: error("ActionElement must also be a UIElement")
        return compose.onNodeWithTag(uiElement.elementId)
    }
}

private fun SemanticsNodeInteraction.scrollIntoViewIfPossible(): SemanticsNodeInteraction {
    runCatching {
        performScrollTo()
    }
    return this
}

private fun VexLogRecord.semanticProjection(): VexExpectedRecord =
    VexExpectedRecord(
        kind = kind,
        subjectRef = subjectRef,
        action = action,
        screenBeforeId = screenBeforeId,
        screenAfterId = screenAfterId,
        detailsJson = detailsJson,
    )
