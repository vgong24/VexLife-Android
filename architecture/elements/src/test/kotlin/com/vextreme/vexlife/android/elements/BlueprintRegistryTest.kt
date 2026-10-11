package com.vextreme.vexlife.android.elements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlueprintRegistryTest {
    private object Home : ScreenElement("screen.home", 1)
    private object Help : ScreenElement("screen.help", 2)

    private object ShellRoot : ColumnElement("element.shell")
    private object ShellButton : ButtonElement(
        elementId = "element.help",
        labelRes = 3,
        intent = navigateTo(Help),
    )

    private object HomeRoot : ColumnElement("element.home.root")
    private object HomeBody : ScrollableColumnElement("element.home.body")
    private object HomeText : TextElement("element.home.preview", 4)

    private object HomePanelA : ColumnElement("element.home.panel-a")
    private object HomePanelB : ColumnElement("element.home.panel-b")
    private object HomeSwap : SwappableContainerElement(
        elementId = "element.home.swap",
        defaultChild = HomePanelA,
    )

    private object HelpCrossScreenSwap : ButtonElement(
        elementId = "element.help.cross-screen-swap",
        labelRes = 5,
        intent = swapTo(
            container = HomeSwap,
            child = HomePanelB,
        ),
    )

    private object HelpRoot : ColumnElement("element.help.root")

    @Test
    fun registryKeepsStaticPlacementSeparateFromRuntimeScreenContext() {
        val registry = UIBlueprintRegistry(
            AppBlueprint(
                shell = column(ShellRoot) {
                    button(ShellButton)
                },
                screens = listOf(
                    ScreenRegistration(
                        Home,
                        column(HomeRoot) {
                            scrollableColumn(HomeBody) {
                                text(HomeText)
                            }
                        },
                    ),
                    ScreenRegistration(Help, column(HelpRoot) {}),
                ),
            ),
        )

        assertNull(registry.ownerScreenOf(ShellButton))
        assertEquals(Home, registry.ownerScreenOf(HomeText))
        assertEquals(HomeBody, registry.parentOf(HomeText))
        assertEquals(listOf(Help), registry.navigationTargetsFrom(listOf(ShellButton)))
    }

    @Test
    fun placementStyleOverrideResolvesWithoutMutatingTheElementDefault() {
        val registry = UIBlueprintRegistry(
            AppBlueprint(
                shell = column(ShellRoot) {
                    button(ShellButton, style = VexElementStyles.Navigation)
                },
                screens = listOf(
                    ScreenRegistration(Home, column(HomeRoot) {}),
                    ScreenRegistration(Help, column(HelpRoot) {}),
                ),
            ),
        )

        assertEquals(VexElementStyles.PrimaryAction, ShellButton.style)
        assertEquals(VexElementStyles.Navigation, registry.styleOf(ShellButton))
    }

    @Test(expected = IllegalStateException::class)
    fun localPresentationIntentCannotMutateAnotherScreensContainer() {
        UIBlueprintRegistry(
            AppBlueprint(
                shell = column(ShellRoot) {},
                screens = listOf(
                    ScreenRegistration(
                        Home,
                        column(HomeRoot) {
                            swappable(HomeSwap) {
                                column(HomePanelA) {}
                                column(HomePanelB) {}
                            }
                        },
                    ),
                    ScreenRegistration(
                        Help,
                        column(HelpRoot) {
                            button(HelpCrossScreenSwap)
                        },
                    ),
                ),
            ),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun duplicateElementIdentityFailsClosed() {
        UIBlueprintRegistry(
            AppBlueprint(
                shell = column(ShellRoot) {
                    button(ShellButton)
                },
                screens = listOf(
                    ScreenRegistration(
                        Home,
                        column(HomeRoot) {
                            element(ShellButton)
                        },
                    ),
                    ScreenRegistration(Help, column(HelpRoot) {}),
                ),
            ),
        )
    }
}
