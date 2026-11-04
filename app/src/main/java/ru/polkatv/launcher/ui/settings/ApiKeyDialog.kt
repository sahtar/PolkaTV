package ru.polkatv.launcher.ui.settings

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
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
import ru.polkatv.launcher.settings.KeyEntryServer
import ru.polkatv.launcher.system.LocalIp
import ru.polkatv.launcher.ui.common.PolkaDialog
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextSecondary
import ru.polkatv.launcher.ui.theme.TextTertiary

/**
 * OpenWeatherMap key entry. The primary path is a phone: while the dialog is open the TV serves a one-field page on the
 * LAN behind a one-time token, shown as a QR code (and as a URL for manual typing). Typing on the remote stays available
 * behind "enter manually", so the on-screen keyboard does not cover the QR code until it is asked for.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ApiKeyDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(current) }
    var manual by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val saveButton = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val toButtons = { keyboard?.hide(); runCatching { saveButton.requestFocus() }; Unit }
    val latestSave by rememberUpdatedState(onSave)

    val token = remember { KeyEntryServer.newToken() }
    val ip = remember { LocalIp.find() }
    var url by remember { mutableStateOf<String?>(null) }
    DisposableEffect(token) {
        val main = Handler(Looper.getMainLooper())
        val server = KeyEntryServer(token) { key -> main.post { latestSave(key) } }   // server thread → main
        val port = runCatching { server.start() }.getOrNull()
        url = if (ip != null && port != null) "http://$ip:$port/$token" else null
        onDispose { server.stop() }
    }
    val qr = remember(url) { url?.let { runCatching { QrCode.render(it, 512) }.getOrNull() } }
    LaunchedEffect(manual) { if (manual) field.requestFocus() }

    PolkaDialog(onDismiss = onDismiss, title = stringResource(R.string.api_key_title), width = 800) {
        Row {
            if (qr != null) {
                Image(qr.asImageBitmap(), contentDescription = url, modifier = Modifier.size(220.dp).background(Color.White, RoundedCornerShape(10.dp)).padding(8.dp))
                Spacer(Modifier.width(20.dp))
            }
            Column(Modifier.weight(1f)) {
                if (url != null) {
                    Text(stringResource(R.string.api_key_qr_hint), fontSize = 19.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.api_key_qr_url, url!!), fontSize = 16.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                } else {
                    Text(stringResource(R.string.api_key_no_network), fontSize = 19.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.api_key_help), fontSize = 15.sp, color = TextTertiary)
            }
        }
        Spacer(Modifier.height(14.dp))
        if (manual) {
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
            ListItem(selected = false, onClick = { onSave(value.trim()) }, modifier = Modifier.focusRequester(saveButton), headlineContent = { Text(stringResource(R.string.action_save)) })
        } else {
            ListItem(selected = false, onClick = { manual = true }, headlineContent = { Text(stringResource(R.string.api_key_manual)) })
        }
        if (current.isNotBlank()) ListItem(selected = false, onClick = { onSave("") }, headlineContent = { Text(stringResource(R.string.action_clear)) })
        ListItem(selected = false, onClick = onDismiss, headlineContent = { Text(stringResource(R.string.action_cancel)) })
    }
}
