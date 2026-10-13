package com.vextreme.vexlife.android.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ColumnElement
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.FabElement
import com.vextreme.vexlife.android.elements.ImageElement
import com.vextreme.vexlife.android.elements.RowElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.StackElement
import com.vextreme.vexlife.android.elements.TextElement
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexFab
import com.vextreme.vexlife.android.elements.VexIcon
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.close
import com.vextreme.vexlife.android.elements.column
import com.vextreme.vexlife.android.elements.navigateBack
import com.vextreme.vexlife.android.elements.navigateTo
import com.vextreme.vexlife.android.elements.then
import com.vextreme.vexlife.android.elements.toggle
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.primary.settings.SettingsScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppToolbarLeadingMode
import com.vextreme.vexlife.android.ui.VexLayoutTokens

/**
 * App-wide chrome and its stable element identities.
 *
 * This owns toolbar, primary navigation, and FAB destinations that exist outside any one screen.
 * Feature screens should be registered in AppScreens and referenced here rather than rebuilding shell
 * controls inside the screen.
 */
object AppShell {
    object Elements {
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
            elementId = "element.navigation.home",
            labelRes = R.string.nav_home,
            style = VexElementStyles.Navigation,
            icon = ElementIcon.Home,
            intent = navigateTo(HomeScreen),
        )

        object Events : ButtonElement(
            elementId = "element.navigation.events",
            labelRes = R.string.nav_events,
            style = VexElementStyles.Navigation,
            icon = ElementIcon.Events,
            intent = navigateTo(EventsScreen),
        )

        object Settings : ButtonElement(
            elementId = "element.navigation.settings",
            labelRes = R.string.nav_settings,
            style = VexElementStyles.Navigation,
            icon = ElementIcon.Settings,
            intent = navigateTo(SettingsScreen),
        )

        object FabCluster : StackElement("element.shell.fab-cluster")
        object FabOptions : ColumnElement("element.shell.fab-options")

        object Add : FabElement(
            elementId = "element.shell.fab.add",
            labelRes = R.string.fab_more,
            icon = ElementIcon.Add,
            intent = toggle(FabOptions),
        )

        object DebugMenu : FabElement(
            elementId = "element.debug-menu",
            labelRes = R.string.fab_debug,
            icon = ElementIcon.Debug,
            style = VexElementStyles.FabOption,
            intent = close(FabOptions) then navigateTo(DebugMenuScreen),
        )

        object Gallery : FabElement(
            elementId = "element.gallery",
            labelRes = R.string.fab_gallery,
            icon = ElementIcon.Gallery,
            style = VexElementStyles.FabOption,
            intent = close(FabOptions) then navigateTo(GalleryScreen),
        )

        object UserProfile : FabElement(
            elementId = "element.user-profile",
            labelRes = R.string.fab_profile,
            icon = ElementIcon.Person,
            style = VexElementStyles.FabOption,
            intent = close(FabOptions) then navigateTo(UserProfileScreen),
        )
    }

    val blueprint = column(Elements.Root) {
        row(Elements.Toolbar) {
            image(Elements.BrandMark)
            button(Elements.Back)
        }
        row(Elements.PrimaryNavigation) {
            button(Elements.Home)
            button(Elements.Events)
            button(Elements.Settings)
        }
        stack(Elements.FabCluster) {
            column(Elements.FabOptions) {
                fab(Elements.UserProfile)
                fab(Elements.DebugMenu)
                fab(Elements.Gallery)
            }
            fab(Elements.Add)
        }
    }
}

@Composable
fun AppToolbar(
    currentScreen: ScreenElement,
    runtime: VexRuntime,
    showBack: Boolean,
    leadingMode: AppToolbarLeadingMode =
        AppToolbarLeadingMode.Standard,
    actions: List<ButtonElement> = emptyList(),
) {
    Surface(
        modifier = Modifier.height(VexLayoutTokens.ToolbarHeight),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val brandOwnsBack =
                showBack &&
                    leadingMode ==
                    AppToolbarLeadingMode.BrandBack

            if (
                showBack &&
                !brandOwnsBack
            ) {
                VexIconButton(
                    element = AppShell.Elements.Back,
                    handler = runtime,
                )
            }

            Surface(
                modifier =
                    Modifier
                        .size(42.dp)
                        .vexElement(
                            AppShell.Elements.BrandMark,
                        ),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (brandOwnsBack) {
                                    Modifier
                                        .vexElement(
                                            AppShell.Elements.Back,
                                        )
                                        .clickable {
                                            runtime.activate(
                                                AppShell.Elements.Back,
                                            )
                                        }
                                } else {
                                    Modifier
                                },
                            ),
                    contentAlignment =
                        Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_vex_mark),
                        contentDescription =
                            stringResource(
                                if (brandOwnsBack) {
                                    R.string.back
                                } else {
                                    R.string.brand_content_description
                                },
                            ),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .size(26.dp)
                                .then(
                                    if (brandOwnsBack) {
                                        Modifier.rotate(90f)
                                    } else {
                                        Modifier
                                    },
                                ),
                    )
                }
            }

            Text(
                text = stringResource(currentScreen.titleRes),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )

            actions.forEach { action ->
                VexIconButton(
                    element = action,
                    handler = runtime,
                )
            }
        }
    }
}

@Composable
fun PrimaryBottomNavigation(
    currentScreen: ScreenElement,
    runtime: VexRuntime,
) {
    NavigationBar(
        modifier = Modifier.height(VexLayoutTokens.BottomBarHeight),
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        PrimaryNavigationItem(AppShell.Elements.Home, HomeScreen, currentScreen, runtime)
        PrimaryNavigationItem(AppShell.Elements.Events, EventsScreen, currentScreen, runtime)
        PrimaryNavigationItem(AppShell.Elements.Settings, SettingsScreen, currentScreen, runtime)
    }
}

@Composable
private fun RowScope.PrimaryNavigationItem(
    element: ButtonElement,
    target: ScreenElement,
    currentScreen: ScreenElement,
    runtime: VexRuntime,
) {
    NavigationBarItem(
        selected = currentScreen.screenId == target.screenId,
        onClick = { runtime.activate(element) },
        modifier = Modifier.vexElement(element),
        icon = {
            VexIcon(
                icon = requireNotNull(element.icon),
                contentDescription = null,
            )
        },
        label = {
            Text(stringResource(requireNotNull(element.labelRes)))
        },
    )
}

@Composable
fun AppFabMenu(
    runtime: VexRuntime,
) {
    val expanded = runtime.presentation.isOpen(AppShell.Elements.FabOptions)

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut(),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VexFab(AppShell.Elements.UserProfile, runtime)
                VexFab(AppShell.Elements.DebugMenu, runtime)
                VexFab(AppShell.Elements.Gallery, runtime)
            }
        }
        VexFab(AppShell.Elements.Add, runtime)
    }
}
