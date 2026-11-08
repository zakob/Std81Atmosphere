package org.std81atmosphere.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import org.std81atmosphere.model.Atmosphere
import org.std81atmosphere.model.AtmosphereState
import org.std81atmosphere.model.Format
import org.std81atmosphere.model.HeightError
import org.std81atmosphere.model.I18n

/** Calculation screen: enter a geometric height, get all parameters. */
@Composable
fun CalcScreen(modifier: Modifier = Modifier, onMessage: (String) -> Unit = {}) {
    var text by remember { mutableStateOf("10000") }
    val clipboard = LocalClipboardManager.current

    var state: AtmosphereState? by remember { mutableStateOf(null) }
    var error: String? by remember { mutableStateOf(null) }

    fun recalc() {
        val value = text.trim().replace(',', '.')
        if (value.isEmpty()) {
            state = null
            error = I18n.tr("calc_empty")
            return
        }
        val height = value.toDoubleOrNull()
        if (height == null) {
            state = null
            error = I18n.tr("invalid_number")
            return
        }
        try {
            state = Atmosphere.compute(height)
            error = null
        } catch (cause: HeightError) {
            state = null
            error = I18n.tr(cause.code)
        }
    }

    LaunchedEffect(Unit) { recalc() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(I18n.tr("calc_hint")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val current = text.trim()
                text = if (current.startsWith("-")) current.removePrefix("-") else "-$current"
            }) {
                Text("\u00B1")
            }
        }
        Button(onClick = { recalc() }, modifier = Modifier.fillMaxWidth()) {
            Text(I18n.tr("nav_calc"))
        }

        val currentState = state
        when {
            error != null -> Text(
                error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
            currentState != null -> ParameterList(
                state = currentState,
                onCopy = { value ->
                    clipboard.setText(AnnotatedString(value))
                    onMessage(I18n.tr("copied_value", "value" to value))
                },
            )
        }
    }
}

private data class ParameterRow(val symbol: String, val value: String, val unit: String)

private fun rowsFor(state: AtmosphereState): List<ParameterRow> = listOf(
    ParameterRow("P", Format.number(state.pressure, 6), I18n.tr("unit_pa")),
    ParameterRow("T", Format.number(state.temperature, 5), I18n.tr("unit_k")),
    ParameterRow("\u03C1", Format.number(state.density, 5), I18n.tr("unit_kg_m3")),
    ParameterRow("M", Format.number(state.molarMass, 5), I18n.tr("unit_kg_mol")),
    ParameterRow("n", Format.number(state.concentration, 5), I18n.tr("unit_m3")),
    ParameterRow("a", Format.number(state.soundSpeed, 5), I18n.tr("unit_m_s")),
    ParameterRow("l", Format.number(state.meanFreePath, 5), I18n.tr("unit_m")),
    ParameterRow("\u03BC", Format.number(state.dynamicViscosity, 5), I18n.tr("unit_pa_s")),
    ParameterRow("\u03BD", Format.number(state.kinematicViscosity, 5), I18n.tr("unit_m2_s")),
    ParameterRow("W", Format.number(state.thermalConductivity, 5), I18n.tr("unit_w_mk")),
    ParameterRow("H", Format.number(state.geopotentialHeight, 5), I18n.tr("unit_m")),
)

@Composable
private fun ParameterList(state: AtmosphereState, onCopy: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (row in rowsFor(state)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                    .clickable { onCopy(row.value) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    row.symbol,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 17.sp),
                    modifier = Modifier.width(48.dp),
                )
                Text(
                    row.value,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 17.sp),
                    modifier = Modifier.weight(1f),
                )
                Text(row.unit, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}