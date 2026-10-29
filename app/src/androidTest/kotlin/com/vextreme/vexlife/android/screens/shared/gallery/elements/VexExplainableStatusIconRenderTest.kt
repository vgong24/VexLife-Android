package com.vextreme.vexlife.android.screens.shared.gallery.elements

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexExplainableStatusIcon
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.ui.VexLifeTheme
import com.vextreme.vexlife.android.ui.VexThemePresentation
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import org.junit.Rule
import org.junit.Test

/**
 * Android-lived proof for the reusable explainable-status interaction.
 *
 * Pure Elements tests own placement/caret math. This test owns the native claim that the exact status
 * icon is inspectable by both tap and long-press, the explanation is rendered as a real Popup, and
 * edge pressure still produces inspectable pixels for lived review.
 */
class VexExplainableStatusIconRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun statusIconExplainsItselfByTapAndLongPressNearWindowEdge() {
        compose.setContent {
            VexLifeTheme(
                presentation =
                    VexThemePresentation.Dark,
            ) {
                Surface {
                    Box(
                        modifier =
                            VexModifier
                                .fillMaxSize(),
                    ) {
                        VexComposeScope(
                            parentRef =
                                TEST_ROOT_ID,
                        ) {
                            VexExplainableStatusIcon(
                                id = "home-status",
                                icon = ElementIcon.Home,
                                statusLabel =
                                    STATUS_LABEL,
                                explanationTitle =
                                    EXPLANATION_TITLE,
                                explanationBody =
                                    EXPLANATION_BODY,
                                modifier =
                                    VexModifier
                                        .align(
                                            Alignment.TopStart,
                                        )
                                        .padding(
                                            start = 2.dp,
                                            top = 72.dp,
                                        ),
                            )
                        }
                    }
                }
            }
        }

        val status =
            compose.onNodeWithTag(
                STATUS_ID,
                useUnmergedTree = true,
            )

        status
            .assertContentDescriptionEquals(
                STATUS_LABEL,
            )
            .assertIsDisplayed()
            .performClick()

        compose
            .onNodeWithTag(
                EXPLANATION_ID,
            )
            .assertIsDisplayed()
        compose
            .onNodeWithText(
                EXPLANATION_TITLE,
            )
            .assertIsDisplayed()
        compose
            .onNodeWithText(
                EXPLANATION_BODY,
            )
            .assertIsDisplayed()

        captureEvidence(
            checkpoint =
                "00-tap-explanation-near-left-edge",
        )

        Espresso.pressBack()
        compose.waitForIdle()

        status.performTouchInput {
            longClick()
        }

        compose
            .onNodeWithTag(
                EXPLANATION_ID,
            )
            .assertIsDisplayed()

        captureEvidence(
            checkpoint =
                "01-long-press-explanation-near-left-edge",
        )
    }

    /**
     * Focused local evidence capture only.
     *
     * ScreenCrawler already owns its own scenario-shaped screenshot pipeline. Conversation also
     * exposed a local physical-proof capture. Repetition across those proof owners is useful pressure
     * for the active Test Foundation lane; this Elements PR does not create a competing global
     * evidence API merely to remove a few lines locally.
     */
    private fun captureEvidence(
        checkpoint: String,
    ) {
        val instrumentation =
            InstrumentationRegistry
                .getInstrumentation()

        compose.waitForIdle()
        instrumentation.waitForIdleSync()

        val bitmap =
            requireNotNull(
                instrumentation
                    .uiAutomation
                    .takeScreenshot(),
            ) {
                "explainable-status screenshot returned null at $checkpoint"
            }

        val bytes =
            try {
                ByteArrayOutputStream()
                    .use { output ->
                        check(
                            bitmap.compress(
                                Bitmap.CompressFormat.PNG,
                                100,
                                output,
                            ),
                        ) {
                            "failed to encode explainable-status screenshot at $checkpoint"
                        }
                        output.toByteArray()
                    }
            } finally {
                bitmap.recycle()
            }

        check(bytes.isNotEmpty()) {
            "explainable-status screenshot encoded to zero bytes at $checkpoint"
        }

        val storage =
            PlatformTestStorageRegistry
                .getInstance()
        val path =
            "vex-explainable-status/screenshots/" +
                "$checkpoint.png"

        storage
            .openOutputFile(path)
            .buffered()
            .use { output ->
                output.write(bytes)
            }

        val sha256 =
            MessageDigest
                .getInstance(
                    "SHA-256",
                )
                .digest(bytes)
                .joinToString(
                    separator = "",
                ) { byte ->
                    "%02x".format(
                        byte,
                    )
                }

        Log.i(
            EVIDENCE_LOG_TAG,
            "CAPTURE checkpoint=$checkpoint " +
                "output=$path " +
                "bytes=${bytes.size} " +
                "sha256=$sha256 " +
                "uri=${storage.getOutputFileUri(path)}",
        )
    }

    private companion object {
        const val TEST_ROOT_ID =
            "test.elements.explainable-status"
        const val STATUS_ID =
            "$TEST_ROOT_ID.home-status"
        const val EXPLANATION_ID =
            "$STATUS_ID.explanation"

        const val STATUS_LABEL =
            "Home connection unavailable"
        const val EXPLANATION_TITLE =
            "Can’t send right now"
        const val EXPLANATION_BODY =
            "VexLife can’t currently establish the Home connection required for this example."

        const val EVIDENCE_LOG_TAG =
            "VexExplainableStatus"
    }
}

// [VXG RealForever]
