package com.moni.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moni.app.ui.theme.MoniColors

/** Brand lockup (isotipo + "moni") + screen title. Shared by every tab. */
@Composable
internal fun ScreenHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 8.dp)) {
        MoniLockup()
        Spacer(Modifier.height(4.dp))
        Text(title, color = MoniColors.fg, style = MaterialTheme.typography.titleLarge)
    }
}
