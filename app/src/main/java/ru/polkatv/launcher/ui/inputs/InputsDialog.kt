package ru.polkatv.launcher.ui.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.system.InputState
import ru.polkatv.launcher.system.TvInput
import ru.polkatv.launcher.ui.common.PolkaDialog

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun InputsDialog(inputs: List<TvInput>, onPick: (TvInput) -> Unit, onDismiss: () -> Unit) {
    PolkaDialog(onDismiss = onDismiss, title = stringResource(R.string.inputs_title)) {
        if (inputs.isEmpty()) Text(stringResource(R.string.inputs_none))
        inputs.forEach { input ->
            val stateRes = when (input.state) {
                InputState.CONNECTED -> R.string.input_state_connected
                InputState.NO_SIGNAL -> R.string.input_state_no_signal
                InputState.DISCONNECTED -> R.string.input_state_disconnected
                InputState.UNKNOWN -> R.string.input_state_unknown
            }
            ListItem(selected = false, onClick = { onPick(input) }, headlineContent = { Text(input.label) }, supportingContent = { Text(stringResource(stateRes)) })
        }
        ListItem(selected = false, onClick = onDismiss, headlineContent = { Text(stringResource(R.string.action_cancel)) })
    }
}
