package com.vextreme.vexlife.android.logging

import com.vextreme.vexlife.android.elements.ActionKey
import com.vextreme.vexlife.android.elements.AppBlueprint
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.elements.ElementPlacement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.ScreenRegistration
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.elements.UIElement
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexLoggerTopologyTest {
    private val screen =
        ScreenElement(
            screenId = "screen.settings",
            titleRes = 1,
        )
    private val body =
        UIElement(
            elementId = "element.settings.body",
            kind = "Column",
        )
    private val action =
        ButtonElement(
            elementId = "element.settings.body.theme.dark",
            labelRes = 2,
            intent =
                ElementIntent.Invoke(
                    ActionKey(
                        "action.theme.dark",
                    ),
                ),
        )
    private val registry =
        UIBlueprintRegistry(
            AppBlueprint(
                shell =
                    ElementPlacement(
                        UIElement(
                            elementId = "element.shell",
                            kind = "Shell",
                        ),
                    ),
                screens =
                    listOf(
                        ScreenRegistration(
                            screen = screen,
                            root =
                                ElementPlacement(
                                    element = body,
                                    children =
                                        listOf(
                                            ElementPlacement(
                                                action,
                                            ),
                                        ),
                                ),
                        ),
                    ),
            ),
        )

    @Test
    fun stableElementIdResolvesThroughBlueprintIntoExplicitFields() {
        val fields =
            VexLogger.subjectFields(
                subjectRef =
                    action.elementId,
                registry =
                    registry,
            )

        assertEquals(
            VexLogSubjectFields.Resolution.RESOLVED,
            fields.resolution,
        )
        assertEquals(
            screen.screenId,
            fields.ownerScreenId,
        )
        assertEquals(
            "Button",
            fields.elementKind,
        )
        assertEquals(
            listOf(
                body.elementId,
                action.elementId,
            ),
            fields.placementPath,
        )
        assertEquals(
            "Invoke(action.theme.dark)",
            fields.declaredIntent,
        )
    }

    @Test
    fun formatterKeepsStoredCoordinatePrimaryAndShowsResolvedStructure() {
        val record =
            VexLogRecord(
                sequence = 1,
                timestamp =
                    Instant.parse(
                        "2026-10-31T23:13:00Z",
                    ),
                durationSincePreviousSeconds = null,
                kind = VexLogKind.Interaction,
                subjectRef = action.elementId,
                action = "Clicked",
                screenBeforeId = screen.screenId,
                screenAfterId = null,
                section = 0,
            )

        val rendered =
            VexLogger.formatStructuredHistory(
                records =
                    listOf(
                        record,
                    ),
                zoneId =
                    ZoneOffset.UTC,
                registry =
                    registry,
            )

        assertTrue(
            rendered.contains(
                "source.ref = ${action.elementId}",
            ),
        )
        assertTrue(
            rendered.contains(
                "source.screen = ${screen.screenId}",
            ),
        )
        assertTrue(
            rendered.contains(
                "source.path = ${body.elementId} -> ${action.elementId}",
            ),
        )
        assertTrue(
            rendered.contains(
                "source.intent = Invoke(action.theme.dark)",
            ),
        )
    }

    @Test
    fun intentionalRuntimeElementIsNotMisclassifiedAsMissingBlueprintArchitecture() {
        val fields =
            VexLogger.subjectFields(
                subjectRef =
                    "element.settings.dynamic.resource-123",
                registry =
                    registry,
                runtimeCoordinate =
                    true,
            )

        assertEquals(
            VexLogSubjectFields.Resolution.RUNTIME_ELEMENT,
            fields.resolution,
        )
    }

    @Test
    fun unresolvedStaticElementProducesAnExplicitDiagnosticFact() {
        val logger =
            VexLogger(
                blueprintRegistry =
                    registry,
            )

        logger.recordInteractionRef(
            subjectRef =
                "element.settings.body.unknown",
            screen =
                screen,
        )

        assertEquals(
            2,
            logger.history.value.size,
        )
        assertEquals(
            VexLogKind.Interaction,
            logger.history.value[0].kind,
        )
        assertEquals(
            VexLogKind.Diagnostic,
            logger.history.value[1].kind,
        )
        assertEquals(
            "BlueprintMappingMissing",
            logger.history.value[1].action,
        )
    }

    @Test
    fun missingBlueprintMappingIsVisibleInsteadOfInventedFromIdText() {
        val fields =
            VexLogger.subjectFields(
                subjectRef =
                    "element.settings.body.unknown",
                registry =
                    registry,
            )

        assertEquals(
            VexLogSubjectFields.Resolution.BLUEPRINT_MAPPING_MISSING,
            fields.resolution,
        )
        assertTrue(
            fields.placementPath
                .isEmpty(),
        )
    }
}

// [VXG RealForever]
