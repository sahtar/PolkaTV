package ru.polkatv.launcher.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import ru.polkatv.launcher.ui.theme.GlassBorder
import ru.polkatv.launcher.ui.theme.TextPrimary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PolkaDialog(onDismiss: () -> Unit, title: String, width: Int = 560, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = androidx.compose.ui.graphics.Color(0xF0141828)),
            modifier = Modifier.width(width.dp).border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
        ) {
            Column(Modifier.padding(28.dp)) {
                Text(title, fontSize = 26.sp, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
    }
}
