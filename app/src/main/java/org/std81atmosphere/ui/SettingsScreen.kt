package org.std81atmosphere.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.std81atmosphere.model.I18n

/** Settings screen: language choice and the About dialog. */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    currentLanguage: String,
    onLanguage: (String) -> Unit,
    onMessage: (String) -> Unit = {},
) {
    var showAbout by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(I18n.tr("settings_language"), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LanguageButton(
                label = I18n.Language.RU.label,
                selected = currentLanguage != "en",
                onClick = { onLanguage("ru") },
                modifier = Modifier.weight(1f),
            )
            LanguageButton(
                label = I18n.Language.EN.label,
                selected = currentLanguage == "en",
                onClick = { onLanguage("en") },
                modifier = Modifier.weight(1f),
            )
        }
        TextButton(onClick = { showAbout = true }) {
            Text(I18n.tr("settings_about"))
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(I18n.tr("about_title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(I18n.tr("about_text"))
                    Text(I18n.tr("about_note"))
                    Text(I18n.tr("about_contact"))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text(I18n.tr("close"))
                }
            },
        )
    }
}

@Composable
private fun LanguageButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}