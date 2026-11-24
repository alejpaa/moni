package com.moni.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moni.app.ui.theme.MoniColors

/** MoniLockup + screen title per docs/design.md. Shared by every tab. */
@Composable
internal fun ScreenHeader(title: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MoniLockup()
        Text(title, color = MoniColors.fg, style = MaterialTheme.typography.titleLarge)
    }
}
