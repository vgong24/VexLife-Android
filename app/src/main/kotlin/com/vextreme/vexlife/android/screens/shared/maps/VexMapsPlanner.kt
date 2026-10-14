package com.vextreme.vexlife.android.screens.shared.maps

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.vexElement

/**
 * Human-facing trip/offline preparation surface.
 *
 * This renders planning meaning supplied by the Maps composition owner. It does not own MapLibre,
 * provider routing, repositories, or navigation-session lifetime.
 */
@Composable
internal fun VexMapsRoutePlannerCard(
    regionPack: VexMapsRegionPack?,
    offlineMapState:
        VexMapsOfflineMapState,
    routeOrigin: VexMapCoordinate?,
    usingPinnedOrigin: Boolean,
    selectingOrigin: Boolean,
    canUseDeviceOrigin: Boolean,
    destination: VexMapCoordinate?,
    placeSearchPack: VexMapsPlaceSearchPack?,
    placeSearchQuery: String,
    placeSearchState: VexMapsPlaceSearchState,
    travelMode: VexMapsTravelMode,
    planningState: VexMapsPlanningState,
    onToggleOfflinePreparation: () -> Unit,
    onPrepareOfflineAroundMe: () -> Unit,
    onPrepareOfflineTrip: () -> Unit,
    onBrowseOfflineRegions: () -> Unit,
    onToggleAdvancedOfflineSetup: () -> Unit,
    onTravelModeSelected: (VexMapsTravelMode) -> Unit,
    onPlaceSearchQueryChange: (String) -> Unit,
    onImportPlaceSearch: () -> Unit,
    onSearchPlaces: () -> Unit,
    onPlaceSelected: (VexMapsPlaceSearchResult) -> Unit,
    onChooseOriginOnMap: () -> Unit,
    onUseDeviceOrigin: () -> Unit,
    onImportOfflineMap: () -> Unit,
    onImportRegion: () -> Unit,
    onRouteHere: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offlinePreparationVisible by
        rememberSaveable {
            mutableStateOf(true)
        }
    var preparationModeName by
        rememberSaveable {
            mutableStateOf<String?>(null)
        }
    var tripDestinationDraft by
        rememberSaveable {
            mutableStateOf("")
        }
    var regionDraft by
        rememberSaveable {
            mutableStateOf("")
        }
    var advancedOfflineVisible by
        rememberSaveable {
            mutableStateOf(false)
        }

    Card(
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(
                        rememberScrollState(),
                    )
                    .padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp),
        ) {
            if (!offlinePreparationVisible) {
                Text(
                    text =
                        stringResource(
                            R.string.vexmaps_online_title,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                )
                Text(
                    text =
                        stringResource(
                            R.string.vexmaps_online_body,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                )
                Button(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.ToggleOfflinePreparation,
                        ),
                    onClick = {
                        onToggleOfflinePreparation()
                        offlinePreparationVisible = true
                    },
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_offline_maps,
                        ),
                    )
                }
            } else {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.vexmaps_offline_prepare_title,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        modifier =
                            Modifier.weight(1f),
                    )
                    OutlinedButton(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.ToggleOfflinePreparation,
                            ),
                        onClick = {
                            onToggleOfflinePreparation()
                            offlinePreparationVisible = false
                            preparationModeName = null
                            advancedOfflineVisible = false
                        },
                    ) {
                        Text(
                            stringResource(
                                R.string.vexmaps_offline_maps_close,
                            ),
                        )
                    }
                }

                Text(
                    text =
                        stringResource(
                            R.string.vexmaps_offline_prepare_body,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                )

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.PrepareOfflineAroundMe,
                        ),
                    onClick = {
                        onPrepareOfflineAroundMe()
                        preparationModeName = "AROUND_ME"
                    },
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_offline_prepare_around,
                        ),
                    )
                }

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.PrepareOfflineTrip,
                        ),
                    onClick = {
                        onPrepareOfflineTrip()
                        preparationModeName = "TRIP"
                    },
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_offline_prepare_trip,
                        ),
                    )
                }

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.BrowseOfflineRegions,
                        ),
                    onClick = {
                        onBrowseOfflineRegions()
                        preparationModeName = "REGION"
                    },
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_offline_prepare_regions,
                        ),
                    )
                }

                when (preparationModeName) {
                    "AROUND_ME" -> {
                        Text(
                            text =
                                stringResource(
                                    if (routeOrigin != null) {
                                        R.string.vexmaps_offline_prepare_around_ready
                                    } else {
                                        R.string.vexmaps_offline_prepare_around_location_needed
                                    },
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,
                        )
                    }

                    "TRIP" -> {
                        Text(
                            text =
                                stringResource(
                                    if (routeOrigin != null) {
                                        R.string.vexmaps_offline_prepare_trip_start_current
                                    } else {
                                        R.string.vexmaps_offline_prepare_trip_start_needed
                                    },
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                        OutlinedTextField(
                            value = tripDestinationDraft,
                            onValueChange = {
                                tripDestinationDraft = it
                            },
                            singleLine = true,
                            label = {
                                Text(
                                    stringResource(
                                        R.string.vexmaps_offline_prepare_trip_destination,
                                    ),
                                )
                            },
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.vexmaps_offline_prepare_trip_body,
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                    }

                    "REGION" -> {
                        OutlinedTextField(
                            value = regionDraft,
                            onValueChange = {
                                regionDraft = it
                            },
                            singleLine = true,
                            label = {
                                Text(
                                    stringResource(
                                        R.string.vexmaps_offline_prepare_region_name,
                                    ),
                                )
                            },
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.vexmaps_offline_prepare_region_body,
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.vexmaps_offline_prepare_region_catalog_missing,
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                    }
                }

                if (preparationModeName != null) {
                    Text(
                        text =
                            stringResource(
                                R.string.vexmaps_offline_prepare_review_title,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleSmall,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.vexmaps_offline_prepare_review_components,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.vexmaps_offline_prepare_review_size,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.vexmaps_offline_prepare_source_missing,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )
                }

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.ToggleAdvancedOfflineSetup,
                        ),
                    onClick = {
                        onToggleAdvancedOfflineSetup()
                        advancedOfflineVisible =
                            !advancedOfflineVisible
                    },
                ) {
                    Text(
                        stringResource(
                            if (advancedOfflineVisible) {
                                R.string.vexmaps_offline_prepare_hide_advanced
                            } else {
                                R.string.vexmaps_offline_prepare_advanced
                            },
                        ),
                    )
                }

                if (advancedOfflineVisible) {
                    Text(
                        text =
                            stringResource(
                                if (placeSearchPack == null) {
                                    R.string.vexmaps_place_search_none
                                } else {
                                    R.string.vexmaps_place_search_ready
                                },
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                    )
                    OutlinedButton(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.ImportPlaceSearch,
                            ),
                        onClick = onImportPlaceSearch,
                        enabled =
                            placeSearchState !is VexMapsPlaceSearchState.Importing &&
                                placeSearchState !is VexMapsPlaceSearchState.Searching,
                    ) {
                        Text(
                            stringResource(
                                if (placeSearchPack == null) {
                                    R.string.vexmaps_place_search_import
                                } else {
                                    R.string.vexmaps_place_search_replace
                                },
                            ),
                        )
                    }

                    Text(
                        text =
                            when (offlineMapState) {
                                is VexMapsOfflineMapState.Loading ->
                                    stringResource(
                                        R.string.vexmaps_offline_map_loading,
                                    )

                                VexMapsOfflineMapState.None ->
                                    stringResource(
                                        R.string.vexmaps_offline_map_none,
                                    )

                                is VexMapsOfflineMapState.Ready ->
                                    stringResource(
                                        R.string.vexmaps_offline_map_ready,
                                        offlineMapState.completeRegionCount,
                                    )

                                is VexMapsOfflineMapState.Error ->
                                    stringResource(
                                        R.string.vexmaps_offline_map_error,
                                    )
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )
                    OutlinedButton(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.ImportOfflineMap,
                            ),
                        onClick = onImportOfflineMap,
                        enabled =
                            offlineMapState !is VexMapsOfflineMapState.Loading,
                    ) {
                        Text(
                            stringResource(
                                R.string.vexmaps_offline_map_import,
                            ),
                        )
                    }

                    Text(
                        text =
                            stringResource(
                                if (regionPack == null) {
                                    R.string.vexmaps_region_pack_needed
                                } else {
                                    R.string.vexmaps_region_pack_ready
                                },
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                    )
                    OutlinedButton(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.ImportRoutingRegion,
                            ),
                        onClick = onImportRegion,
                        enabled =
                            planningState !is VexMapsPlanningState.Importing &&
                                planningState !is VexMapsPlanningState.Routing,
                    ) {
                        Text(
                            stringResource(
                                if (regionPack == null) {
                                    R.string.vexmaps_region_pack_import
                                } else {
                                    R.string.vexmaps_region_pack_replace
                                },
                            ),
                        )
                    }
                }
            }

            if (placeSearchPack != null) {
                OutlinedTextField(
                    value = placeSearchQuery,
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.PlaceSearchQuery,
                        ),
                    onValueChange =
                        onPlaceSearchQueryChange,
                    singleLine = true,
                    label = {
                        Text(
                            stringResource(
                                R.string.vexmaps_place_search_query,
                            ),
                        )
                    },
                )

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.SearchPlaces,
                        ),
                    onClick = onSearchPlaces,
                    enabled =
                        placeSearchQuery.isNotBlank() &&
                            placeSearchState !is VexMapsPlaceSearchState.Importing &&
                            placeSearchState !is VexMapsPlaceSearchState.Searching,
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_place_search_action,
                        ),
                    )
                }

                when (placeSearchState) {
                    VexMapsPlaceSearchState.Searching ->
                        Text(
                            stringResource(
                                R.string.vexmaps_place_searching,
                            ),
                        )

                    is VexMapsPlaceSearchState.Results ->
                        placeSearchState.places.forEach { place ->
                            OutlinedButton(
                                onClick = {
                                    onPlaceSelected(
                                        place,
                                    )
                                },
                            ) {
                                Column {
                                    Text(
                                        place.name,
                                    )
                                    place.subtitle
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                        ?.let { subtitle ->
                                            Text(
                                                text = subtitle,
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodySmall,
                                            )
                                        }
                                }
                            }
                        }

                    is VexMapsPlaceSearchState.Error ->
                        Text(
                            stringResource(
                                R.string.vexmaps_place_search_error,
                            ),
                        )

                    VexMapsPlaceSearchState.Importing,
                    VexMapsPlaceSearchState.Idle ->
                        Unit
                }
            }

            if (regionPack != null) {
                Text(
                    text =
                        stringResource(
                            R.string.vexmaps_route_mode,
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp),
                ) {
                    VexMapsTravelMode.entries.forEach { mode ->
                        if (mode == travelMode) {
                            Button(
                                modifier =
                                    Modifier.vexElement(
                                        VexMapsScreen.travelModeElement(
                                            mode,
                                        ),
                                    ),
                                onClick = {
                                    onTravelModeSelected(
                                        mode,
                                    )
                                },
                            ) {
                                Text(
                                    stringResource(
                                        mode.labelRes,
                                    ),
                                )
                            }
                        } else {
                            OutlinedButton(
                                modifier =
                                    Modifier.vexElement(
                                        VexMapsScreen.travelModeElement(
                                            mode,
                                        ),
                                    ),
                                onClick = {
                                    onTravelModeSelected(
                                        mode,
                                    )
                                },
                            ) {
                                Text(
                                    stringResource(
                                        mode.labelRes,
                                    ),
                                )
                            }
                        }
                    }
                }

                Text(
                    text =
                        stringResource(
                            when {
                                selectingOrigin ->
                                    R.string.vexmaps_route_start_selecting

                                usingPinnedOrigin ->
                                    R.string.vexmaps_route_start_pinned

                                routeOrigin != null ->
                                    R.string.vexmaps_route_start_current

                                else ->
                                    R.string.vexmaps_route_start_needed
                            },
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                )

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.ChooseOriginOnMap,
                        ),
                    onClick = onChooseOriginOnMap,
                    enabled =
                        planningState !is VexMapsPlanningState.Routing &&
                            planningState !is VexMapsPlanningState.Importing,
                ) {
                    Text(
                        stringResource(
                            if (usingPinnedOrigin) {
                                R.string.vexmaps_route_change_start
                            } else {
                                R.string.vexmaps_route_choose_start
                            },
                        ),
                    )
                }

                if (
                    usingPinnedOrigin &&
                    canUseDeviceOrigin
                ) {
                    OutlinedButton(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.UseDeviceOrigin,
                            ),
                        onClick = onUseDeviceOrigin,
                    ) {
                        Text(
                            stringResource(
                                R.string.vexmaps_route_use_current_start,
                            ),
                        )
                    }
                }
            }

            if (
                regionPack != null &&
                routeOrigin != null &&
                destination != null &&
                !selectingOrigin
            ) {
                Button(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.PlanRoute,
                        ),
                    onClick = onRouteHere,
                    enabled =
                        planningState !is VexMapsPlanningState.Routing &&
                            planningState !is VexMapsPlanningState.Importing,
                ) {
                    Text(
                        stringResource(
                            R.string.vexmaps_route_here,
                        ),
                    )
                }
            }
        }
    }
}

// [VXG RealForever]
