package ru.polkatv.launcher.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import ru.polkatv.launcher.R
import ru.polkatv.launcher.settings.City
import ru.polkatv.launcher.ui.common.PolkaDialog
import ru.polkatv.launcher.ui.theme.TextSecondary
import ru.polkatv.launcher.ui.theme.TextTertiary

private sealed interface SearchState { object Idle : SearchState; object Loading : SearchState; object Error : SearchState; data class Results(val cities: List<City>) : SearchState }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AddCityDialog(search: suspend (String) -> List<City>, onPick: (City) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val firstResult = remember { FocusRequester() }
    // Hide the on-screen keyboard (it covers the list) and step into the results; a no-op while there are none.
    val goToResults = { keyboard?.hide(); runCatching { firstResult.requestFocus() }; Unit }

    LaunchedEffect(query) {
        if (query.trim().length < 2) { state = SearchState.Idle; return@LaunchedEffect }
        delay(400)
        state = SearchState.Loading
        state = runCatching { SearchState.Results(search(query.trim())) }.getOrElse { SearchState.Error }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    PolkaDialog(onDismiss = onDismiss, title = stringResource(R.string.add_city_title), width = 640) {
        Box(Modifier.fillMaxWidth().background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (query.isEmpty()) Text(stringResource(R.string.add_city_hint), fontSize = 22.sp, color = TextTertiary)
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 22.sp), cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { goToResults() }),
                // A text field swallows D-pad Down; hand it on so the result list is reachable.
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown) { goToResults(); true } else false
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        when (val s = state) {
            SearchState.Idle -> Unit
            SearchState.Loading -> Text("…", fontSize = 20.sp, color = TextSecondary)
            SearchState.Error -> Text(stringResource(R.string.add_city_no_network), fontSize = 20.sp, color = TextSecondary)
            is SearchState.Results -> if (s.cities.isEmpty()) Text(stringResource(R.string.add_city_nothing), fontSize = 20.sp, color = TextSecondary)
            else LazyColumn(Modifier.height(320.dp)) {
                itemsIndexed(s.cities, key = { _, c -> c.key }) { i, c ->
                    ListItem(selected = false, onClick = { onPick(c) }, modifier = if (i == 0) Modifier.focusRequester(firstResult) else Modifier,
                        headlineContent = { Text(c.name) },
                        supportingContent = { Text(listOfNotNull(c.region, c.country).joinToString(", ")) })
                }
            }
        }
    }
}
