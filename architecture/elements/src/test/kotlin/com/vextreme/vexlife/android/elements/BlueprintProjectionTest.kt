package com.vextreme.vexlife.android.elements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlueprintProjectionTest {
    private object Home : ScreenElement("screen.home", 1)
    private object Help : ScreenElement("screen.help", 2)

    private object ShellRoot : ColumnElement("element.shell")
    private object ShellHelp : ButtonElement(
        elementId = "element.shell.help",
        labelRes = 3,
        icon = ElementIcon.Help,
        intent = navigateTo(Help),
    )

    private object HomeRoot : ColumnElement("element.home.body")
    private object HomeTitle : TextElement(
        elementId = "element.home.body.title",
        textRes = 4,
        role = TextRole.Title,
    )
    private object HomeStatus : DynamicTextElement(
        elementId = "element.home.body.status",
        role = TextRole.Supporting,
    )
    private object PanelA : ColumnElement("element.home.body.panel-a")
    private object PanelB : ColumnElement("element.home.body.panel-b")
    private object Content : SwappableContainerElement(
        elementId = "element.home.body.content",
        defaultChild = PanelA,
    )
    private object ShowPanelB : ButtonElement(
        elementId = "element.home.body.show-panel-b",
        labelRes = 5,
        icon = ElementIcon.Next,
        intent =
            swapTo(
                container = Content,
                child = PanelB,
            ),
    )

    private object HelpRoot : ColumnElement("element.help.body")
    private object HelpBack : ButtonElement(
        elementId = "element.help.body.back",
        labelRes = 6,
        icon = ElementIcon.Back,
        intent = navigateBack(),
    )

    private fun registry(): UIBlueprintRegistry =
        UIBlueprintRegistry(
            AppBlueprint(
                shell =
                    column(ShellRoot) {
                        button(
                            ShellHelp,
                            style = VexElementStyles.Navigation,
                        )
                    },
                screens =
                    listOf(
                        ScreenRegistration(
                            Home,
                            column(HomeRoot) {
                                text(HomeTitle)
                                dynamicText(HomeStatus)
                                swappable(Content) {
                                    column(PanelA) {}
                                    column(PanelB) {}
                                }
                                button(ShowPanelB)
                            },
                        ),
                        ScreenRegistration(
                            Help,
                            column(HelpRoot) {
                                button(HelpBack)
                            },
                        ),
                    ),
            ),
        )

    @Test
    fun projectionIsDeterministicAndKeepsStructuralOrder() {
        val first = registry().semanticProjection()
        val second = registry().semanticProjection()

        assertEquals(first, second)
        assertEquals(
            listOf(
                "screen.help",
                "screen.home",
            ),
            first.screens.map { it.screenRef },
        )
        assertEquals(
            listOf(
                "element.shell",
                "element.shell.help",
            ),
            first.shellElementRefs,
        )

        val home =
            first.screens.single {
                it.screenRef == Home.screenId
            }

        assertEquals(
            HomeRoot.elementId,
            home.rootElementRef,
        )
        assertEquals(
            home.elementRefs.sorted(),
            home.elementRefs,
        )

        val root =
            first.elements.single {
                it.semanticRef == HomeRoot.elementId
            }

        assertEquals(
            listOf(
                HomeTitle.elementId,
                HomeStatus.elementId,
                Content.elementId,
                ShowPanelB.elementId,
            ),
            root.childSemanticRefs,
        )

        assertEquals(
            first.elements.map { it.semanticRef }.sorted(),
            first.elements.map { it.semanticRef },
        )
    }

    @Test
    fun projectionKeepsOwnerParentStyleAndDynamicTextMeaning() {
        val projection = registry().semanticProjection()

        val dynamicText =
            projection.elements.single {
                it.semanticRef == HomeStatus.elementId
            }

        assertEquals("DynamicText", dynamicText.kind)
        assertEquals(VexElementStyles.Default.value, dynamicText.styleRef)
        assertEquals(Home.screenId, dynamicText.ownerScreenRefOrNull)
        assertEquals(HomeRoot.elementId, dynamicText.parentSemanticRefOrNull)
        assertEquals(TextRole.Supporting.name, dynamicText.textRoleOrNull)
        assertNull(dynamicText.intentOrNull)

        val shellHelp =
            projection.elements.single {
                it.semanticRef == ShellHelp.elementId
            }

        assertNull(shellHelp.ownerScreenRefOrNull)
        assertEquals(ShellRoot.elementId, shellHelp.parentSemanticRefOrNull)
        assertEquals(VexElementStyles.Navigation.value, shellHelp.styleRef)
        assertEquals("icon.help", shellHelp.iconRefOrNull)
    }

    @Test
    fun projectionKeepsTypedIntentRelationshipsWithoutRuntimeState() {
        val projection = registry().semanticProjection()

        val shellHelp =
            projection.elements.single {
                it.semanticRef == ShellHelp.elementId
            }
        assertEquals(
            UIBlueprintIntentProjection(
                kind = "NAVIGATE_TO",
                screenRefs = listOf(Help.screenId),
            ),
            shellHelp.intentOrNull,
        )

        val swap =
            projection.elements.single {
                it.semanticRef == ShowPanelB.elementId
            }
        assertEquals(
            UIBlueprintIntentProjection(
                kind = "SWAP_TO",
                elementRefs =
                    listOf(
                        Content.elementId,
                        PanelB.elementId,
                    ),
            ),
            swap.intentOrNull,
        )

        val content =
            projection.elements.single {
                it.semanticRef == Content.elementId
            }
        assertEquals(
            PanelA.elementId,
            content.defaultChildSemanticRefOrNull,
        )
        assertEquals(
            listOf(
                PanelA.elementId,
                PanelB.elementId,
            ),
            content.childSemanticRefs,
        )

        val back =
            projection.elements.single {
                it.semanticRef == HelpBack.elementId
            }
        assertEquals(
            "NAVIGATE_BACK",
            back.intentOrNull?.kind,
        )
        assertEquals(
            "icon.back",
            back.iconRefOrNull,
        )
    }
}

// [VXG RealForever][UI-BLUEPRINT][SEMANTIC-PROJECTION]
