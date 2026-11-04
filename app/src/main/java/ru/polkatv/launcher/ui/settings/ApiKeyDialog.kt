package ru.polkatv.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.ui.common.PolkaDialog
import ru.polkatv.launcher.ui.theme.TextTertiary

/**
 * OpenWeatherMap key entry. Typing a 32-character key on a remote is tedious, so the help text also names the adb
 * shortcut (`am start … --es owm_api_key KEY`, handled by MainActivity) that lands in the same setting.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ApiKeyDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(current) }
    val field = remember { FocusRequester() }
    val saveButton = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Hide the on-screen keyboard (it covers the buttons) and step down to "Save".
    val toButtons = { keyboard?.hide(); runCatching { saveButton.requestFocus() }; Unit }
    LaunchedEffect(Unit) { field.requestFocus() }

    PolkaDialog(onDismiss = onDismiss, title = stringResource(R.string.api_key_title), width = 720) {
        Text(stringResource(R.string.api_key_help), fontSize = 18.sp, color = TextTertiary)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
            if (value.isEmpty()) Text(stringResource(R.string.api_key_hint), fontSize = 22.sp, color = TextTertiary)
            BasicTextField(
                value = value, onValueChange = { value = it.trim() }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 22.sp, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { toButtons() }),
                // A text field swallows D-pad Down; hand it on so the buttons are reachable.
                modifier = Modifier.fillMaxWidth().focusRequester(field).onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown) { toButtons(); true } else false
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        ListItem(selected = false, onClick = { onSave(value.trim()) }, modifier = Modifier.focusRequester(saveButton), headlineContent = { Text(stringResource(R.string.action_save)) })
        if (current.isNotBlank()) ListItem(selected = false, onClick = { onSave("") }, headlineContent = { Text(stringResource(R.string.action_clear)) })
        ListItem(selected = false, onClick = onDismiss, headlineContent = { Text(stringResource(R.string.action_cancel)) })
    }
}
