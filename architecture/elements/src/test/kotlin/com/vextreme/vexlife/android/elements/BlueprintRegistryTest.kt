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

    private object HelpRoot : ColumnElement("element.help.root")

    @Test
    fun registryKeepsStaticPlacementSeparateFromRuntimeScreenContext() {
        val registry = UIBlueprintRegistry(
            AppBlueprint(
                shell = column(ShellRoot) {
                    button(ShellButton)
                },
                screens = listOf(
                    ScreenBlueprint(
                        Home,
                        column(HomeRoot) {
                            scrollableColumn(HomeBody) {
                                text(HomeText)
                            }
                        },
                    ),
                    ScreenBlueprint(Help, column(HelpRoot) {}),
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
                    ScreenBlueprint(Home, column(HomeRoot) {}),
                    ScreenBlueprint(Help, column(HelpRoot) {}),
                ),
            ),
        )

        assertEquals(VexElementStyles.PrimaryAction, ShellButton.style)
        assertEquals(VexElementStyles.Navigation, registry.styleOf(ShellButton))
    }

    @Test(expected = IllegalStateException::class)
    fun duplicateElementIdentityFailsClosed() {
        UIBlueprintRegistry(
            AppBlueprint(
                shell = column(ShellRoot) {
                    button(ShellButton)
                },
                screens = listOf(
                    ScreenBlueprint(
                        Home,
                        column(HomeRoot) {
                            element(ShellButton)
                        },
                    ),
                    ScreenBlueprint(Help, column(HelpRoot) {}),
                ),
            ),
        )
    }
}
