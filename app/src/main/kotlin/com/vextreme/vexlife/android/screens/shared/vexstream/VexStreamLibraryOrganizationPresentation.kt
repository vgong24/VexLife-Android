package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexModalBottomSheet
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.VexTextField
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.runtime.VexRuntime

/**
 * One compact organization surface for the current VexStream library.
 *
 * Sort and Filter are deliberately separate human questions:
 *
 * - Sort changes ordering of the current set.
 * - Filter changes membership in the current set.
 *
 * The sheet mutates only Screen-local [VexStreamLibraryOrganization] state. Repository/catalog,
 * playlist, Queue and Playback truth remain with their existing owners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VexStreamLibraryOrganizationSheet(
    runtime: VexRuntime,
    organization: VexStreamLibraryOrganization,
    options: VexStreamLibraryOrganizationOptions,
    resultCount: Int,
    onOrganizationChange: (VexStreamLibraryOrganization) -> Unit,
) {
    var thresholdText by remember(
        organization.durationThresholdMinutes,
    ) {
        mutableStateOf(
            organization.durationThresholdMinutes.toString(),
        )
    }

    val actionHandler =
        ElementActionHandler { element ->
            runtime.activate(
                element,
            )
            val next =
                when (element) {
                    VexStreamMusicScreen.SortLibraryByTitle ->
                        organization.copy(
                            sort = VexStreamLibrarySort.TITLE,
                        )

                    VexStreamMusicScreen.SortLibraryByArtist ->
                        organization.copy(
                            sort = VexStreamLibrarySort.ARTIST,
                        )

                    VexStreamMusicScreen.SortLibraryByAlbum ->
                        organization.copy(
                            sort = VexStreamLibrarySort.ALBUM,
                        )

                    VexStreamMusicScreen.SortLibraryByYear ->
                        organization.copy(
                            sort = VexStreamLibrarySort.YEAR,
                        )

                    VexStreamMusicScreen.SortLibraryByDuration ->
                        organization.copy(
                            sort = VexStreamLibrarySort.DURATION,
                        )

                    VexStreamMusicScreen.DurationFilterAny ->
                        organization.copy(
                            durationComparison =
                                VexStreamDurationComparison.ANY,
                        )

                    VexStreamMusicScreen.DurationFilterLessThan ->
                        organization.copy(
                            durationComparison =
                                VexStreamDurationComparison.LESS_THAN,
                        )

                    VexStreamMusicScreen.DurationFilterGreaterThan ->
                        organization.copy(
                            durationComparison =
                                VexStreamDurationComparison.GREATER_THAN,
                        )

                    VexStreamMusicScreen.ClearLibraryFilters ->
                        organization.clearFilters()

                    else ->
                        organization
                }

            if (next != organization) {
                onOrganizationChange(
                    next,
                )
            }
        }

    VexModalBottomSheet(
        element = VexStreamMusicScreen.OrganizationSheet,
        onDismissRequest = {
            runtime.dismissPresentation(
                VexStreamMusicScreen.OrganizationSheet,
            )
        },
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = false,
            ),
    ) {
        VexColumn(
            id = "content",
            modifier =
                VexModifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp,
                ),
        ) {
            VexText(
                id = "title",
                text =
                    stringResource(
                        R.string.vexstream_music_organize_title,
                    ),
                style = MaterialTheme.typography.headlineSmall,
            )
            VexText(
                id = "result-count",
                text =
                    stringResource(
                        R.string.vexstream_music_organize_result_count,
                        resultCount,
                    ),
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OrganizationSectionLabel(
                text =
                    stringResource(
                        R.string.vexstream_music_sort_by,
                    ),
            )
            Row(
                modifier =
                    VexModifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState(),
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp,
                    ),
            ) {
                SortButton(
                    runtime = runtime,
                    handler = actionHandler,
                    element =
                        VexStreamMusicScreen.SortLibraryByTitle,
                    selected =
                        organization.sort ==
                            VexStreamLibrarySort.TITLE,
                )
                SortButton(
                    runtime = runtime,
                    handler = actionHandler,
                    element =
                        VexStreamMusicScreen.SortLibraryByArtist,
                    selected =
                        organization.sort ==
                            VexStreamLibrarySort.ARTIST,
                )
                SortButton(
                    runtime = runtime,
                    handler = actionHandler,
                    element =
                        VexStreamMusicScreen.SortLibraryByAlbum,
                    selected =
                        organization.sort ==
                            VexStreamLibrarySort.ALBUM,
                )
                SortButton(
                    runtime = runtime,
                    handler = actionHandler,
                    element =
                        VexStreamMusicScreen.SortLibraryByYear,
                    selected =
                        organization.sort ==
                            VexStreamLibrarySort.YEAR,
                )
                SortButton(
                    runtime = runtime,
                    handler = actionHandler,
                    element =
                        VexStreamMusicScreen
                            .SortLibraryByDuration,
                    selected =
                        organization.sort ==
                            VexStreamLibrarySort.DURATION,
                )
            }

            OrganizationSectionLabel(
                text =
                    stringResource(
                        R.string.vexstream_music_filter_duration,
                    ),
            )
            Row(
                modifier =
                    VexModifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState(),
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp,
                    ),
            ) {
                FilterModeButton(
                    handler = actionHandler,
                    element =
                        VexStreamMusicScreen.DurationFilterAny,
                    selected =
                        organization.durationComparison ==
                            VexStreamDurationComparison.ANY,
                )
                FilterModeButton(
                    handler = actionHandler,
                    element =
                        VexStreamMusicScreen
                            .DurationFilterLessThan,
                    selected =
                        organization.durationComparison ==
                            VexStreamDurationComparison.LESS_THAN,
                )
                FilterModeButton(
                    handler = actionHandler,
                    element =
                        VexStreamMusicScreen
                            .DurationFilterGreaterThan,
                    selected =
                        organization.durationComparison ==
                            VexStreamDurationComparison.GREATER_THAN,
                )
            }
            VexTextField(
                element =
                    VexStreamMusicScreen.DurationThresholdMinutes,
                value = thresholdText,
                onValueChange = { raw ->
                    val normalized =
                        raw.filter(
                            Char::isDigit,
                        ).take(
                            5,
                        )
                    thresholdText = normalized

                    normalized
                        .toIntOrNull()
                        ?.takeIf {
                            it > 0
                        }
                        ?.let { minutes ->
                            onOrganizationChange(
                                organization.copy(
                                    durationThresholdMinutes =
                                        minutes,
                                ),
                            )
                        }
                },
            )

            if (options.artists.isNotEmpty()) {
                OrganizationSectionLabel(
                    text =
                        stringResource(
                            R.string.vexstream_music_filter_artist,
                        ),
                )
                StringFilterRow(
                    values = options.artists,
                    selected = organization.artists,
                    refFor =
                        VexStreamPresentationRefs::organizationArtistOption,
                    onToggle = { artist ->
                        runtime.activateDynamic(
                            element =
                                VexStreamMusicScreen
                                    .ToggleLibraryFilter,
                            subjectRef =
                                VexStreamPresentationRefs
                                    .organizationArtistOption(
                                        artist,
                                    ),
                        )
                        onOrganizationChange(
                            organization.toggleArtist(
                                artist,
                            ),
                        )
                    },
                )
            }

            if (options.genres.isNotEmpty()) {
                OrganizationSectionLabel(
                    text =
                        stringResource(
                            R.string.vexstream_music_filter_genre,
                        ),
                )
                StringFilterRow(
                    values = options.genres,
                    selected = organization.genres,
                    refFor =
                        VexStreamPresentationRefs::organizationGenreOption,
                    onToggle = { genre ->
                        runtime.activateDynamic(
                            element =
                                VexStreamMusicScreen
                                    .ToggleLibraryFilter,
                            subjectRef =
                                VexStreamPresentationRefs
                                    .organizationGenreOption(
                                        genre,
                                    ),
                        )
                        onOrganizationChange(
                            organization.toggleGenre(
                                genre,
                            ),
                        )
                    },
                )
            }

            if (options.availability.isNotEmpty()) {
                OrganizationSectionLabel(
                    text =
                        stringResource(
                            R.string.vexstream_music_filter_availability,
                        ),
                )
                LazyRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp,
                        ),
                ) {
                    items(
                        items = options.availability,
                        key = VexStreamTrackAvailability::name,
                    ) { availability ->
                        val ref =
                            VexStreamPresentationRefs
                                .organizationAvailabilityOption(
                                    availability,
                                )
                        FilterChip(
                            selected =
                                availability in
                                    organization.availability,
                            onClick = {
                                runtime.activateDynamic(
                                    element =
                                        VexStreamMusicScreen
                                            .ToggleLibraryFilter,
                                    subjectRef = ref,
                                )
                                onOrganizationChange(
                                    organization.toggleAvailability(
                                        availability,
                                    ),
                                )
                            },
                            label = {
                                Text(
                                    text =
                                        availabilityLabel(
                                            availability,
                                        ),
                                )
                            },
                            modifier =
                                VexModifier.vex(
                                    ref,
                                ),
                        )
                    }
                }
            }

            VexButton(
                element =
                    VexStreamMusicScreen.ClearLibraryFilters,
                handler = actionHandler,
                modifier =
                    VexModifier.fillMaxWidth(),
                enabled = organization.hasActiveFilters,
            )
            VexButton(
                element =
                    VexStreamMusicScreen.CloseOrganization,
                handler = runtime,
                modifier =
                    VexModifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OrganizationSectionLabel(
    text: String,
) {
    VexText(
        id =
            "section-" +
                text
                    .lowercase()
                    .replace(
                        Regex("[^a-z0-9]+"),
                        "-",
                    )
                    .trim('-'),
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun SortButton(
    runtime: VexRuntime,
    handler: ElementActionHandler,
    element: com.vextreme.vexlife.android.elements.ButtonElement,
    selected: Boolean,
) {
    VexButton(
        element = element,
        handler = handler,
        style =
            if (selected) {
                VexElementStyles.PrimaryAction
            } else {
                VexElementStyles.Navigation
            },
    )
}

@Composable
private fun FilterModeButton(
    handler: ElementActionHandler,
    element: com.vextreme.vexlife.android.elements.ButtonElement,
    selected: Boolean,
) {
    VexButton(
        element = element,
        handler = handler,
        style =
            if (selected) {
                VexElementStyles.PrimaryAction
            } else {
                VexElementStyles.Navigation
            },
    )
}

@Composable
private fun StringFilterRow(
    values: List<String>,
    selected: Set<String>,
    refFor: (String) -> String,
    onToggle: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp,
            ),
    ) {
        items(
            items = values,
            key = {
                it.lowercase()
            },
        ) { value ->
            FilterChip(
                selected =
                    selected.any {
                        it.equals(
                            value,
                            ignoreCase = true,
                        )
                    },
                onClick = {
                    onToggle(
                        value,
                    )
                },
                label = {
                    Text(
                        text = value,
                    )
                },
                modifier =
                    VexModifier.vex(
                        refFor(
                            value,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun availabilityLabel(
    availability: VexStreamTrackAvailability,
): String =
    when (availability) {
        VexStreamTrackAvailability.HUB_ONLY ->
            stringResource(
                R.string.vexstream_music_filter_hub_only,
            )

        VexStreamTrackAvailability.HUB_AND_LOCAL ->
            stringResource(
                R.string.vexstream_music_filter_hub_and_device,
            )

        VexStreamTrackAvailability.LOCAL_ONLY ->
            stringResource(
                R.string.vexstream_music_filter_device_only,
            )

        VexStreamTrackAvailability.UNAVAILABLE ->
            stringResource(
                R.string.vexstream_music_filter_unavailable,
            )
    }

// [VXG RealForever]
