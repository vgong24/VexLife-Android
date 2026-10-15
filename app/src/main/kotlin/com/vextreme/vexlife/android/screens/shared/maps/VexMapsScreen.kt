package com.vextreme.vexlife.android.screens.shared.maps

import androidx.compose.runtime.Composable
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.close
import com.vextreme.vexlife.android.elements.localEffect
import com.vextreme.vexlife.android.elements.open
import com.vextreme.vexlife.android.elements.then
import com.vextreme.vexlife.android.elements.toggle
import com.vextreme.vexlife.android.preview.VexRenderedScreenPreview
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint

/**
 * Home-level VexMaps destination.
 *
 * Map rendering and runtime navigation remain feature-local, but stable user actions are registered
 * here so logging, testing, and repository inspection can address the same semantic identities that
 * the custom Compose surface renders. Runtime map points/search rows remain dynamic data and are not
 * promoted into the static registry.
 *
 * The [content] tree below is therefore the static semantic registration surface, not a claim that
 * the generic element renderer is VexMaps' visual body. [renderContent] owns the native/custom Maps
 * composition and the Preview at the bottom of this file intentionally exercises that real renderer.
 */
object VexMapsScreen : AppScreenBlueprint(
    screenKey = "vex-maps",
    titleRes = R.string.vexmaps_title,
) {
    val UsePreciseLocation = buttonRef(
        id = "body.location.use-precise",
        labelRes = R.string.vexmaps_use_precise_location,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Location,
        intent = localEffect(),
    )

    val MapMenu = columnRef(
        "body.map-menu",
    )

    val OfflinePanel = modalBottomSheetRef(
        "body.offline.panel",
    )

    val OpenMapMenu = buttonRef(
        id = "body.map-menu.open",
        labelRes = R.string.vexmaps_map_options,
        style = VexElementStyles.ToolbarIcon,
        icon = ElementIcon.Menu,
        intent = toggle(MapMenu),
    )

    val OpenOfflineMaps = buttonRef(
        id = "body.map-menu.offline",
        labelRes = R.string.vexmaps_offline_maps,
        style = VexElementStyles.SecondaryAction,
        intent =
            close(MapMenu)
                .then(open(OfflinePanel)),
    )

    val ToggleOfflinePreparation = buttonRef(
        id = "body.offline.prepare.toggle",
        labelRes = R.string.vexmaps_offline_maps,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val PrepareOfflineAroundMe = buttonRef(
        id = "body.offline.prepare.around-me",
        labelRes = R.string.vexmaps_offline_prepare_around,
        intent = localEffect(),
    )

    val PrepareOfflineTrip = buttonRef(
        id = "body.offline.prepare.trip",
        labelRes = R.string.vexmaps_offline_prepare_trip,
        intent = localEffect(),
    )

    val BrowseOfflineRegions = buttonRef(
        id = "body.offline.prepare.regions",
        labelRes = R.string.vexmaps_offline_prepare_regions,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val ToggleAdvancedOfflineSetup = buttonRef(
        id = "body.offline.prepare.advanced",
        labelRes = R.string.vexmaps_offline_prepare_advanced,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val ImportPlaceSearch = buttonRef(
        id = "body.planner.place-search.import",
        labelRes = R.string.vexmaps_place_search_import,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val PlaceSearchQuery = textFieldRef(
        id = "body.planner.place-search.query",
        labelRes = R.string.vexmaps_place_search_query,
    )

    val SearchPlaces = buttonRef(
        id = "body.planner.place-search.search",
        labelRes = R.string.vexmaps_place_search_action,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val ImportOfflineMap = buttonRef(
        id = "body.planner.offline-map.import",
        labelRes = R.string.vexmaps_offline_map_import,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val ImportRoutingRegion = buttonRef(
        id = "body.planner.routing-region.import",
        labelRes = R.string.vexmaps_region_pack_import,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val DriveMode = buttonRef(
        id = "body.planner.travel-mode.drive",
        labelRes = R.string.vexmaps_route_mode_drive,
        style = VexElementStyles.Navigation,
        intent = localEffect(),
    )

    val WalkMode = buttonRef(
        id = "body.planner.travel-mode.walk",
        labelRes = R.string.vexmaps_route_mode_walk,
        style = VexElementStyles.Navigation,
        intent = localEffect(),
    )

    val BicycleMode = buttonRef(
        id = "body.planner.travel-mode.bicycle",
        labelRes = R.string.vexmaps_route_mode_bicycle,
        style = VexElementStyles.Navigation,
        intent = localEffect(),
    )

    val ChooseOriginOnMap = buttonRef(
        id = "body.planner.origin.choose-on-map",
        labelRes = R.string.vexmaps_route_choose_start,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val UseDeviceOrigin = buttonRef(
        id = "body.planner.origin.use-device",
        labelRes = R.string.vexmaps_route_use_current_start,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val PlanRoute = buttonRef(
        id = "body.planner.route.calculate",
        labelRes = R.string.vexmaps_route_here,
        intent = localEffect(),
    )

    val ToggleDistributionMode = buttonRef(
        id = "body.distribution.mode.toggle",
        labelRes = R.string.vexmaps_distribution_title,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val AddDistributionStop = buttonRef(
        id = "body.distribution.stop.add",
        labelRes = R.string.vexmaps_distribution_add_stop,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val ToggleDistributionReturnToStart = buttonRef(
        id = "body.distribution.return.toggle",
        labelRes = R.string.vexmaps_distribution_return_on,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val PrepareDistributionRun = buttonRef(
        id = "body.distribution.run.prepare",
        labelRes = R.string.vexmaps_distribution_prepare,
        intent = localEffect(),
    )

    val StartDistributionRun = buttonRef(
        id = "body.distribution.run.start",
        labelRes = R.string.vexmaps_distribution_start,
        intent = localEffect(),
    )

    val CompleteDistributionStop = buttonRef(
        id = "body.distribution.run.complete-stop",
        labelRes = R.string.vexmaps_distribution_delivered,
        intent = localEffect(),
    )

    val ResetDistributionRun = buttonRef(
        id = "body.distribution.run.reset",
        labelRes = R.string.vexmaps_distribution_clear,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val EditRoute = buttonRef(
        id = "body.navigation.edit-route",
        labelRes = R.string.vexmaps_navigation_edit_route,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val StartNavigation = buttonRef(
        id = "body.navigation.start",
        labelRes = R.string.vexmaps_navigation_start,
        intent = localEffect(),
    )

    val StopNavigation = buttonRef(
        id = "body.navigation.stop",
        labelRes = R.string.vexmaps_navigation_stop,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val DoneNavigation = buttonRef(
        id = "body.navigation.done",
        labelRes = R.string.vexmaps_navigation_done,
        intent = localEffect(),
    )

    val RecenterNavigation = buttonRef(
        id = "body.navigation.recenter",
        labelRes = R.string.vexmaps_navigation_recenter,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    override val content = layout {
        scrollableColumn("body") {
            card("foundation") {
                text(
                    id = "title",
                    textRes = R.string.vexmaps_foundation_title,
                    role = TextRole.Title,
                )
                text(
                    id = "message",
                    textRes = R.string.vexmaps_foundation_body,
                    role = TextRole.Supporting,
                )
            }

            column("location") {
                button(UsePreciseLocation)
            }

            column(
                element = MapMenu,
            ) {
                button(OpenOfflineMaps)
            }

            modalBottomSheet(
                OfflinePanel,
            ) {}

            column("planner") {
                button(ToggleOfflinePreparation)
                button(PrepareOfflineAroundMe)
                button(PrepareOfflineTrip)
                button(BrowseOfflineRegions)
                button(ToggleAdvancedOfflineSetup)
                button(ImportPlaceSearch)
                textField(PlaceSearchQuery)
                button(SearchPlaces)
                button(ImportOfflineMap)
                button(ImportRoutingRegion)

                row("travel-mode") {
                    button(DriveMode)
                    button(WalkMode)
                    button(BicycleMode)
                }

                button(ChooseOriginOnMap)
                button(UseDeviceOrigin)
                button(PlanRoute)
            }

            column("distribution") {
                button(ToggleDistributionMode)
                button(AddDistributionStop)
                button(ToggleDistributionReturnToStart)
                button(PrepareDistributionRun)
                button(StartDistributionRun)
                button(CompleteDistributionStop)
                button(ResetDistributionRun)
            }

            column("navigation") {
                button(EditRoute)
                button(StartNavigation)
                button(StopNavigation)
                button(DoneNavigation)
                button(RecenterNavigation)
            }
        }
    }

    internal override val toolbarActions
        get() =
            listOf(
                OpenMapMenu,
            )

    internal fun travelModeElement(
        mode: VexMapsTravelMode,
    ): ButtonElement =
        when (mode) {
            VexMapsTravelMode.DRIVE ->
                DriveMode

            VexMapsTravelMode.WALK ->
                WalkMode

            VexMapsTravelMode.BICYCLE ->
                BicycleMode
        }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        darkTheme: Boolean,
    ) {
        VexMapsMapSurface(
            runtime = runtime,
            repository = repositories.vexMaps,
            distributionRepository =
                repositories.vexMapsDistribution,
            darkTheme = darkTheme,
        )
    }
}

@VexScreenPreviews
@Composable
private fun VexMapsScreenPreview() {
    VexRenderedScreenPreview(VexMapsScreen)
}

// [VXG RealForever]
