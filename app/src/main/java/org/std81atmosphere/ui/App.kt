package org.std81atmosphere.ui

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.std81atmosphere.model.I18n

/** Which bottom-navigation screen is shown. */
private enum class Tab(val key: String) {
    CALC("calc"),
    TABLE("table"),
    PLOT("plot"),
    SETTINGS("settings"),
}

/** Root composable: bottom navigation plus the current screen. */
@Composable
fun App(initialLanguage: String) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    var languageCode by rememberSaveable { mutableStateOf(initialLanguage) }
    var tab by rememberSaveable { mutableStateOf(Tab.CALC.name) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Apply the persisted language on first composition / on change.
    I18n.current = if (languageCode == "en") I18n.Language.EN else I18n.Language.RU

    fun chooseLanguage(code: String) {
        languageCode = code
        preferences.edit().putString("language", code).apply()
    }

    fun showMessage(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    val currentTab = remember(tab) { Tab.valueOf(tab) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                items(currentTab) { selected ->
                    NavigationBarItem(
                        selected = selected,
                        onClick = { tab = selected.name },
                        icon = {
                            Icon(
                                imageVector = when (selected) {
                                    Tab.CALC -> Icons.Filled.Home
                                    Tab.TABLE -> Icons.Filled.List
                                    Tab.PLOT -> Icons.Filled.Star
                                    Tab.SETTINGS -> Icons.Filled.Info
                                },
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(
                                when (selected) {
                                    Tab.CALC -> I18n.tr("nav_calc")
                                    Tab.TABLE -> I18n.tr("nav_table")
                                    Tab.PLOT -> I18n.tr("nav_plot")
                                    Tab.SETTINGS -> I18n.tr("nav_settings")
                                }
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        // Recreate the screen subtree when the language changes so that every
        // I18n.tr() call is re-evaluated.
        key(languageCode) {
            when (currentTab) {
                Tab.CALC -> CalcScreen(
                    modifier = Modifier.padding(padding),
                    onMessage = ::showMessage,
                )
                Tab.TABLE -> TableScreen(
                    modifier = Modifier.padding(padding),
                    onMessage = ::showMessage,
                )
                Tab.PLOT -> PlotScreen(modifier = Modifier.padding(padding))
                Tab.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    currentLanguage = languageCode,
                    onLanguage = ::chooseLanguage,
                    onMessage = ::showMessage,
                )
            }
        }
    }
}