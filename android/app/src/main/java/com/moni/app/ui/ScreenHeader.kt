package com.moni.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moni.app.ui.theme.MoniColors

/** Compact monkey face + optional screen title. Shared by every tab. */
@Composable
internal fun ScreenHeader(title: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MoniFace(36.dp)
        if (title != null) {
            Text(title, color = MoniColors.fg, style = MaterialTheme.typography.titleLarge)
        }
    }
}
