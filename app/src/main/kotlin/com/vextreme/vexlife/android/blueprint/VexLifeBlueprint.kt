package com.vextreme.vexlife.android.blueprint

import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.ActionKey
import com.vextreme.vexlife.android.elements.AppBlueprint
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ColumnElement
import com.vextreme.vexlife.android.elements.DialogElement
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.FabElement
import com.vextreme.vexlife.android.elements.ImageElement
import com.vextreme.vexlife.android.elements.RowElement
import com.vextreme.vexlife.android.elements.ScreenBlueprint
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.ScrollableColumnElement
import com.vextreme.vexlife.android.elements.StackElement
import com.vextreme.vexlife.android.elements.SwappableContainerElement
import com.vextreme.vexlife.android.elements.TextElement
import com.vextreme.vexlife.android.elements.ToggleElement
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.close
import com.vextreme.vexlife.android.elements.column
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.navigateBack
import com.vextreme.vexlife.android.elements.navigateTo
import com.vextreme.vexlife.android.elements.open
import com.vextreme.vexlife.android.elements.scrollableColumn
import com.vextreme.vexlife.android.elements.swapTo
import com.vextreme.vexlife.android.elements.then
import com.vextreme.vexlife.android.elements.toggle

object Screens {
    object Home : ScreenElement("screen.home", R.string.home_title)
    object Events : ScreenElement("screen.events", R.string.events_title)
    object Settings : ScreenElement("screen.settings", R.string.settings_title)
    object DebugMenu : ScreenElement("screen.debug-menu", R.string.debug_menu_title)
    object Gallery : ScreenElement("screen.gallery", R.string.gallery_title)

    val Primary = listOf(Home, Events, Settings)
}

object AppActions {
    val ToggleDarkMode = ActionKey("action.theme.dark-mode.toggle")
    val ResetLogSection = ActionKey("action.log-history.reset-section")
    val GallerySample = ActionKey("action.gallery.sample")
}

object ShellElements {
    object Root : ColumnElement("element.shell.root")
    object Toolbar : RowElement("element.shell.toolbar")
    object BrandMark : ImageElement("element.shell.brand-mark", R.string.brand_content_description)
    object BrandName : TextElement("element.shell.brand-name", R.string.brand_name)
    object BrandTagline : TextElement("element.shell.brand-tagline", R.string.brand_tagline)
    object Back : ButtonElement(
        elementId = "element.shell.back",
        labelRes = R.string.back,
        style = VexElementStyles.ToolbarIcon,
        icon = ElementIcon.Back,
        intent = navigateBack(),
    )

    object PrimaryNavigation : RowElement("element.shell.primary-navigation")
    object Home : ButtonElement(
        "element.navigation.home",
        R.string.nav_home,
        VexElementStyles.Navigation,
        ElementIcon.Home,
        navigateTo(Screens.Home),
    )
    object Events : ButtonElement(
        "element.navigation.events",
        R.string.nav_events,
        VexElementStyles.Navigation,
        ElementIcon.Events,
        navigateTo(Screens.Events),
    )
    object Settings : ButtonElement(
        "element.navigation.settings",
        R.string.nav_settings,
        VexElementStyles.Navigation,
        ElementIcon.Settings,
        navigateTo(Screens.Settings),
    )

    object FabCluster : StackElement("element.shell.fab-cluster")
    object FabOptions : ColumnElement("element.shell.fab-options")
    object Add : FabElement(
        "element.shell.fab.add",
        R.string.fab_more,
        ElementIcon.Add,
        VexElementStyles.FabPrimary,
        toggle(FabOptions),
    )
    object DebugMenu : FabElement(
        "element.debug-menu",
        R.string.fab_debug,
        ElementIcon.Debug,
        VexElementStyles.FabOption,
        close(FabOptions) then navigateTo(Screens.DebugMenu),
    )
    object Gallery : FabElement(
        "element.gallery",
        R.string.fab_gallery,
        ElementIcon.Gallery,
        VexElementStyles.FabOption,
        close(FabOptions) then navigateTo(Screens.Gallery),
    )
}

object HomeElements {
    object Root : ScrollableColumnElement("element.home.root")
    object PreviewCard : ColumnElement("element.home.preview-card", VexElementStyles.RaisedSurface)
    object PreviewTitle : TextElement("element.home.preview-title", R.string.home_preview_title)
    object PreviewBody : TextElement("element.home.preview-body", R.string.home_preview_body)
}

object EventsElements {
    object Root : ScrollableColumnElement("element.events.root")
    object PreviewCard : ColumnElement("element.events.preview-card", VexElementStyles.RaisedSurface)
    object PreviewTitle : TextElement("element.events.preview-title", R.string.events_preview_title)
    object PreviewBody : TextElement("element.events.preview-body", R.string.events_preview_body)
}

object SettingsElements {
    object Root : ScrollableColumnElement("element.settings.root")
    object PreviewCard : ColumnElement("element.settings.preview-card", VexElementStyles.RaisedSurface)
    object PreviewTitle : TextElement("element.settings.preview-title", R.string.settings_preview_title)
    object PreviewBody : TextElement("element.settings.preview-body", R.string.settings_preview_body)
}

object DebugElements {
    object Root : ScrollableColumnElement("element.debug-menu.root")
    object DarkMode : ToggleElement(
        "element.debug-menu.dark-mode",
        R.string.dark_mode,
        intent = invoke(AppActions.ToggleDarkMode),
    )
    object LogHistory : ColumnElement("element.debug-menu.log-history", VexElementStyles.RaisedSurface)
    object LogHistoryTitle : TextElement("element.debug-menu.log-history.title", R.string.log_history)
    object LogHistoryActions : RowElement("element.debug-menu.log-history.actions")
    object Show : ButtonElement(
        "element.debug-menu.log-history.show",
        R.string.log_history_show,
        VexElementStyles.SecondaryAction,
        ElementIcon.History,
        open(LogDialog),
    )
    object Reset : ButtonElement(
        "element.debug-menu.log-history.reset",
        R.string.log_history_reset,
        VexElementStyles.SecondaryAction,
        ElementIcon.Reset,
        invoke(AppActions.ResetLogSection),
    )
    object Helper : TextElement("element.debug-menu.log-history.helper", R.string.log_history_helper)
    object LogDialog : DialogElement("element.debug-menu.log-history.dialog")
    object LogDialogBody : ColumnElement("element.debug-menu.log-history.dialog.body")
    object CloseLogDialog : ButtonElement(
        "element.debug-menu.log-history.dialog.close",
        R.string.close,
        VexElementStyles.SecondaryAction,
        ElementIcon.Close,
        close(LogDialog),
    )
}

object GalleryElements {
    object Root : ColumnElement("element.gallery.root")
    object Tabs : RowElement("element.gallery.tabs")
    object ElementsPanel : ScrollableColumnElement("element.gallery.elements")
    object AppGraphPanel : ColumnElement("element.gallery.app-graph")
    object Content : SwappableContainerElement(
        "element.gallery.content",
        defaultChild = ElementsPanel,
    )
    object ElementsTab : ButtonElement(
        "element.gallery.tabs.elements",
        R.string.gallery_elements,
        VexElementStyles.Navigation,
        intent = swapTo(ElementsPanel),
    )
    object AppGraphTab : ButtonElement(
        "element.gallery.tabs.app-graph",
        R.string.gallery_app_graph,
        VexElementStyles.Navigation,
        intent = swapTo(AppGraphPanel),
    )

    object Intro : TextElement("element.gallery.elements.intro", R.string.gallery_intro)
    object SampleContainer : ColumnElement("element.gallery.elements.sample-container", VexElementStyles.RaisedSurface)
    object SampleText : TextElement("element.gallery.elements.sample-text", R.string.gallery_sample_text)
    object SampleRow : RowElement("element.gallery.elements.sample-row")
    object SampleButton : ButtonElement(
        "element.gallery.elements.sample-button",
        R.string.gallery_sample_button,
        VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.GallerySample),
    )
    object SampleToggle : ToggleElement(
        "element.gallery.elements.sample-toggle",
        R.string.gallery_sample_toggle,
        intent = invoke(AppActions.GallerySample),
    )
    object SampleFab : FabElement(
        "element.gallery.elements.sample-fab",
        R.string.gallery_sample_fab,
        ElementIcon.Add,
        VexElementStyles.FabOption,
        invoke(AppActions.GallerySample),
    )

    object GraphIntro : TextElement("element.gallery.app-graph.intro", R.string.gallery_graph_intro)
    object GraphViewport : ColumnElement("element.gallery.app-graph.viewport", VexElementStyles.RaisedSurface)
}

object VexLifeBlueprint {
    val app = AppBlueprint(
        shell = column(ShellElements.Root) {
            row(ShellElements.Toolbar) {
                image(ShellElements.BrandMark)
                text(ShellElements.BrandName)
                text(ShellElements.BrandTagline)
                button(ShellElements.Back)
            }
            row(ShellElements.PrimaryNavigation) {
                button(ShellElements.Home)
                button(ShellElements.Events)
                button(ShellElements.Settings)
            }
            stack(ShellElements.FabCluster) {
                column(ShellElements.FabOptions) {
                    fab(ShellElements.DebugMenu)
                    fab(ShellElements.Gallery)
                }
                fab(ShellElements.Add)
            }
        },
        screens = listOf(
            ScreenBlueprint(
                Screens.Home,
                scrollableColumn(HomeElements.Root) {
                    column(HomeElements.PreviewCard) {
                        text(HomeElements.PreviewTitle)
                        text(HomeElements.PreviewBody)
                    }
                },
            ),
            ScreenBlueprint(
                Screens.Events,
                scrollableColumn(EventsElements.Root) {
                    column(EventsElements.PreviewCard) {
                        text(EventsElements.PreviewTitle)
                        text(EventsElements.PreviewBody)
                    }
                },
            ),
            ScreenBlueprint(
                Screens.Settings,
                scrollableColumn(SettingsElements.Root) {
                    column(SettingsElements.PreviewCard) {
                        text(SettingsElements.PreviewTitle)
                        text(SettingsElements.PreviewBody)
                    }
                },
            ),
            ScreenBlueprint(
                Screens.DebugMenu,
                scrollableColumn(DebugElements.Root) {
                    toggle(DebugElements.DarkMode)
                    column(DebugElements.LogHistory) {
                        text(DebugElements.LogHistoryTitle)
                        row(DebugElements.LogHistoryActions) {
                            button(DebugElements.Show)
                            button(DebugElements.Reset)
                        }
                        text(DebugElements.Helper)
                    }
                    dialog(DebugElements.LogDialog) {
                        column(DebugElements.LogDialogBody) {}
                        button(DebugElements.CloseLogDialog)
                    }
                },
            ),
            ScreenBlueprint(
                Screens.Gallery,
                column(GalleryElements.Root) {
                    row(GalleryElements.Tabs) {
                        button(GalleryElements.ElementsTab)
                        button(GalleryElements.AppGraphTab)
                    }
                    swappable(GalleryElements.Content) {
                        scrollableColumn(GalleryElements.ElementsPanel) {
                            text(GalleryElements.Intro)
                            column(GalleryElements.SampleContainer) {
                                text(GalleryElements.SampleText)
                                row(GalleryElements.SampleRow) {
                                    button(GalleryElements.SampleButton)
                                }
                                toggle(GalleryElements.SampleToggle)
                                fab(GalleryElements.SampleFab)
                            }
                        }
                        column(GalleryElements.AppGraphPanel) {
                            text(GalleryElements.GraphIntro)
                            column(GalleryElements.GraphViewport) {}
                        }
                    }
                },
            ),
        ),
    )

    val registry = UIBlueprintRegistry(app)
}
