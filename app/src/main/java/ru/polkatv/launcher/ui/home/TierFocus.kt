package ru.polkatv.launcher.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties

/**
 * Focus requesters for the three vertical tiers of the home screen (spec 4.2):
 * [header] (service icons, top), [shelf] (weather shelf, middle) and [row] (app tiles, bottom).
 *
 * The shelf is not focusable yet, so Up/Down currently skip it. Task 8 makes the shelf focusable
 * (attach [shelf] to its focus group) and switches the header's `down` and the row's `up`
 * targets in HomeScreen to [shelf], and gives the shelf its own [tierExits].
 */
class TierFocus {
    val header = FocusRequester()
    val shelf = FocusRequester()
    val row = FocusRequester()
}

/**
 * Makes this node a focus group that redirects focus leaving it Up/Down to [up]/[down]
 * (null keeps the default search). A parent group is used (not the tier's own group) so the
 * tier's `focusRestorer` keeps saving and restoring its last focused child.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.tierExits(up: FocusRequester?, down: FocusRequester?): Modifier =
    this.focusProperties {
        exit = { dir ->
            when (dir) {
                FocusDirection.Up -> up ?: FocusRequester.Default
                FocusDirection.Down -> down ?: FocusRequester.Default
                else -> FocusRequester.Default
            }
        }
    }.focusGroup()
