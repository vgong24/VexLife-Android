package com.vextreme.vexlife.android.screens.shared.maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.runtime.VexRuntime

/**
 * Primary ordinary Maps discovery surface.
 *
 * Query text remains presentation input. [VexMapsDestinationSearchSession] owns submitted request /
 * result currentness. Selecting one result is routed back to the Maps composition owner so this
 * surface never becomes the selected-destination or route owner.
 */
@Composable
internal fun VexMapsDestinationSearchOverlay(
    runtime: VexRuntime,
    query: String,
    state: VexMapsDestinationSearchState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPlaceSelected:
        (VexMapsPlaceSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard =
        LocalSoftwareKeyboardController.current

    fun submit() {
        if (
            query.trim().isNotEmpty() &&
            state !is
                VexMapsDestinationSearchState
                    .Searching
        ) {
            keyboard?.hide()
            runtime.activate(
                VexMapsScreen.SearchDestination,
            )
            onSearch()
        }
    }

    Card(
        modifier =
            modifier
                .widthIn(
                    max = 560.dp,
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .vexElement(
                            VexMapsScreen
                                .DestinationSearchQuery,
                        ),
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = {
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_destination_search_hint,
                        ),
                    )
                },
                trailingIcon = {
                    if (
                        state is
                            VexMapsDestinationSearchState
                                .Searching
                    ) {
                        CircularProgressIndicator()
                    } else {
                        IconButton(
                            modifier =
                                Modifier.vexElement(
                                    VexMapsScreen
                                        .SearchDestination,
                                ),
                            onClick = ::submit,
                            enabled =
                                query
                                    .trim()
                                    .isNotEmpty(),
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Filled.Search,
                                contentDescription =
                                    stringResource(
                                        R.string
                                            .vexmaps_destination_search_action,
                                    ),
                            )
                        }
                    }
                },
                keyboardOptions =
                    KeyboardOptions(
                        imeAction =
                            ImeAction.Search,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onSearch = {
                            submit()
                        },
                    ),
            )

            when (state) {
                VexMapsDestinationSearchState
                    .Unavailable ->
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_destination_search_unavailable,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )

                VexMapsDestinationSearchState
                    .NoResults ->
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_destination_search_no_results,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )

                VexMapsDestinationSearchState
                    .Error ->
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_destination_search_error,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )

                is VexMapsDestinationSearchState
                    .Results ->
                    state.places
                        .forEach { place ->
                            TextButton(
                                onClick = {
                                    runtime
                                        .recordDynamicInteraction(
                                            subjectRef =
                                                "presentation.vexmaps.destination-search.result." +
                                                    place.placeRef,
                                            action =
                                                "SelectedAsDestination",
                                        )
                                    onPlaceSelected(
                                        place,
                                    )
                                },
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
                                    verticalAlignment =
                                        Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier =
                                            Modifier
                                                .weight(1f),
                                    ) {
                                        Text(
                                            text =
                                                place.name,
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodyLarge,
                                        )
                                        place.subtitle
                                            ?.let {
                                                subtitle ->
                                                Text(
                                                    text =
                                                        subtitle,
                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .bodySmall,
                                                )
                                            }
                                    }
                                }
                            }
                        }

                VexMapsDestinationSearchState
                    .Idle,
                VexMapsDestinationSearchState
                    .Searching,
                ->
                    Unit
            }
        }
    }
}

// [VXG RealForever]
