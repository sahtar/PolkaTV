package ru.polkatv.launcher.ui.apps

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.ui.common.PolkaDialog

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppPickerDialog(apps: List<TvApp>, onPick: (TvApp) -> Unit, onDismiss: () -> Unit) {
    PolkaDialog(onDismiss = onDismiss, title = stringResource(R.string.picker_title)) {
        LazyColumn(Modifier.height(340.dp)) {
            items(apps, key = { it.packageName }) { app ->
                ListItem(selected = false, onClick = { onPick(app) }, headlineContent = { Text(app.label) })
            }
        }
    }
}
