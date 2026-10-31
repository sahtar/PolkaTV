package ru.polkatv.launcher.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import ru.polkatv.launcher.R
import ru.polkatv.launcher.settings.ClockFormat
import ru.polkatv.launcher.system.NetState
import ru.polkatv.launcher.ui.common.Formatters
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextSecondary
import java.time.LocalDateTime

data class HeaderActions(
    val onInputs: () -> Unit,
    val onNotifications: () -> Unit,
    val onNetwork: () -> Unit,
    val onTvSettings: () -> Unit,
    val onPolkaSettings: () -> Unit,
)

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Header(clockFormat: ClockFormat, showDate: Boolean, net: NetState, actions: HeaderActions, modifier: Modifier = Modifier, iconsFocus: FocusRequester = remember { FocusRequester() }, showInputs: Boolean = true) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000L - (System.currentTimeMillis() % 1000L))
        }
    }
    val use24h = when (clockFormat) {
        ClockFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        ClockFormat.H24 -> true
        ClockFormat.H12 -> false
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column {
            Text(Formatters.clock(now.toLocalTime(), use24h), fontSize = Dimens.clockSize, color = TextPrimary, lineHeight = Dimens.clockSize)
            if (showDate) Text(Formatters.date(now.toLocalDate(), locale), fontSize = Dimens.dateSize, color = TextSecondary)
        }
        Row(Modifier.focusRequester(iconsFocus).focusRestorer().focusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Set-top boxes have no HDMI inputs: the icon is hidden rather than opening an empty dialog.
            if (showInputs) HeaderIcon(Icons.AutoMirrored.Filled.Input, stringResource(R.string.header_inputs), actions.onInputs)
            HeaderIcon(Icons.Default.Notifications, stringResource(R.string.header_notifications), actions.onNotifications)
            val netIcon: ImageVector = when (net) {
                NetState.NONE -> Icons.Default.WifiOff
                NetState.WIFI -> Icons.Default.Wifi
                NetState.ETHERNET -> Icons.Default.SettingsEthernet
            }
            HeaderIcon(netIcon, stringResource(R.string.header_network), actions.onNetwork)
            HeaderIcon(Icons.Default.Settings, stringResource(R.string.header_tv_settings), actions.onTvSettings)
            HeaderIcon(Icons.Default.Tune, stringResource(R.string.header_polka_settings), actions.onPolkaSettings)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeaderIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(Dimens.iconButton)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
    }
}
